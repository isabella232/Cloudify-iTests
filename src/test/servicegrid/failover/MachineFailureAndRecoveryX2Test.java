package test.servicegrid.failover;

import static test.utils.AdminUtils.loadGSCs;
import static test.utils.AdminUtils.loadGSM;
import static test.utils.DistributionUtils.assertBackups;
import static test.utils.DistributionUtils.assertEvenlyDistributed;
import static test.utils.DistributionUtils.assertEvenlyDistributedPrimaries;
import static test.utils.DistributionUtils.assertPrimaries;
import static test.utils.LogUtils.log;
import static test.utils.ProcessingUnitUtils.waitForDeploymentStatus;
import static test.utils.ProcessingUnitUtils.waitForManaged;
import static test.utils.SLAUtils.assertMaxPerMachine;
import static test.utils.SLAUtils.assertMaxPerVM;

import java.util.concurrent.TimeUnit;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.DeploymentStatus;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.openspaces.admin.space.SpaceDeployment;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.AbstractTest;
import test.utils.ProcessingUnitUtils;

import com.gigaspaces.cluster.activeelection.SpaceMode;

/**
 * Topology:
 * - 1 GSM and 2 GSC at 2 machines
 * - Cluster A - partitioned-sync2backup 1,1 with max-per-machine=1 
 * - Cluster B - partitioned-sync2backup 1,1 with max-per-machine=1
 * 
 * TEST (5) Machine (GSM, GSC) Failure and recovery, failure and recovery
 * 
 * Start GSM A, GSC A1, GSM B, GSC B1, GSC A2, GSC B2
 * Deploy Cluster A to GSM A
 * Deploy Cluster B to GSM A
 * 
 * a. Spaces are evenly distributed across all 4 GSCs
 * b. 2 clusters deployed, 4 spaces total
 * c. Kill -9 GSM A, GSC A1, GSC A2
 *    Space instances from A1, A2 are not re-deployed because of max-per-machine
 * d. Only 2 primaries are available.
 * e. Start GSM A, GSC A1, GSC A2
 *    2 backups are started on A1 and/or A2
 * f. 2 clusters are available, 4 spaces total
 * g. Kill -9 GSM B, GSC B1, GSC B2
 *    Space instances from B1, B2 are not re-deployed because of max-per-machine
 * h. Only 2 primaries are available.
 * i. Start GSM B, GSC B1, GSC B2
 *    Both backups are started on B1 and/or B2
 * j. 2 clusters are available, 4 spaces total
 * 
 * @author Moran Avigdor
 */
public class MachineFailureAndRecoveryX2Test extends AbstractTest {
	private Machine machineA, machineB;

	@BeforeMethod
	public void setup() {
		//1 GSM and 2 GSC at 2 machines
		log("waiting for 2 machines");
		admin.getMachines().waitFor(2);

		log("waiting for 2 GSAs");
		admin.getGridServiceAgents().waitFor(2);
		GridServiceAgent[] agents = admin.getGridServiceAgents().getAgents();
		GridServiceAgent gsaA = agents[0];
		GridServiceAgent gsaB = agents[1];
		
		machineA = gsaA.getMachine();
		machineB = gsaB.getMachine();
		
		//Start GSM A, GSC A1, GSM B, GSC B1, GSC A2, GSC B2
		log("starting: 1 GSM and 2 GSC at 2 machines");
		GridServiceManager gsmA = loadGSM(machineA); //GSM A
		loadGSCs(machineA, 2); //GSC A1, GSC A2
		GridServiceManager gsmB = loadGSM(machineB); //GSM B
		loadGSCs(machineB, 2); //GSC B1, GSC B2
		
		doDeploy(gsmA, gsmB);
	}
	
	//sub-classed 
	protected void doDeploy(GridServiceManager gsmA, GridServiceManager gsmB) {
		//Deploy Cluster A to GSM A
		log("deploying: 2 clusters A and B, partitioned 1,1");
		ProcessingUnit puA = gsmA.deploy(new SpaceDeployment("A").partitioned(1, 1).maxInstancesPerMachine(1).maxInstancesPerVM(0));
		ProcessingUnit puB = gsmA.deploy(new SpaceDeployment("B").partitioned(1, 1).maxInstancesPerMachine(1).maxInstancesPerVM(0));
		assertTrue(puA.waitFor(puA.getTotalNumberOfInstances()));
		assertTrue(puB.waitFor(puB.getTotalNumberOfInstances()));
	}
	
	@Test(timeOut=DEFAULT_TEST_TIMEOUT, groups = "2")
	public void test() throws Exception {
		testDeploymentStatus();
		testDistribution();
		killAllOnMachineA();
		startAllOnMachineA();
		testDeploymentStatus();
		killAllOnMachineB();
		startAllOnMachineB();
		testDeploymentStatus();
	}

	//2 processing units are deployed
	private void testDeploymentStatus() {
		assertEquals("Expected 2 processing units", 2, admin.getProcessingUnits().getSize());
		for (ProcessingUnit processingUnit : admin.getProcessingUnits()) {
			assertEquals("Expected 2 processing units", 2, processingUnit.getInstances().length);
			assertEquals("Expected partition 1,1", 1, processingUnit.getPartitions().length);
			assertEquals("Unexpected deployment status", DeploymentStatus.INTACT, waitForDeploymentStatus(processingUnit, DeploymentStatus.INTACT));
		}
	}
	
	//Spaces are evenly distributed across all 4 GSCs 
	private void testDistribution() {
		assertEvenlyDistributed(admin);
		assertEvenlyDistributedPrimaries(admin);
		assertMaxPerMachine(admin);
		assertMaxPerVM(admin);
	}
	
	//Kill -9 GSM A, GSC A1, GSC A2
	//Space instances from A1, A2 are not re-deployed because of max-per-machine
	private void killAllOnMachineA() {
		
		GridServiceManager gsmA = machineA.getGridServiceManagers().getManagers()[0];
		GridServiceManager gsmB = machineB.getGridServiceManagers().getManagers()[0];
		
		GridServiceContainer[] containers = machineA.getGridServiceContainers().getContainers();
		GridServiceContainer gscA1 = containers[0];
		GridServiceContainer gscA2 = containers[1];
		
		//Kill -9 GSM A, GSC A1
		log("killing GSM A, GSC A1, GSC A2, containing: " + ProcessingUnitUtils.getProcessingUnitInstanceName(gscA1.getProcessingUnitInstances())
				+", " + ProcessingUnitUtils.getProcessingUnitInstanceName(gscA2.getProcessingUnitInstances()));
		gsmA.kill();
		gscA1.kill();
		gscA2.kill();
		
		log("wait for GSM B to manage processing units");
		for (ProcessingUnit pu : admin.getProcessingUnits()) {
            assertEquals("GSM B should manage all Processing Units", gsmB, waitForManaged(pu, gsmB));
        }
		
		//Space instances from A1, A2 are not re-deployed because of max-per-machine
		assertEquals("Expected 2 processing units", 2, admin.getProcessingUnits().getSize());
		for (ProcessingUnit processingUnit : admin.getProcessingUnits()) {
			assertEquals("Expected 1 processing units", 1, processingUnit.getInstances().length);
			assertEquals("Unexpected deployment status", DeploymentStatus.COMPROMISED, waitForDeploymentStatus(processingUnit, DeploymentStatus.COMPROMISED));
		}
		
		//2 Spaces are available both primary
		log("wait for active election of primaries ...");
		for (ProcessingUnit processingUnit : admin.getProcessingUnits()) {
			ProcessingUnitInstance[] instances = processingUnit.getInstances();
			for (ProcessingUnitInstance processingUnitInstance : instances) {
				assertTrue("wait for PRIMARY mode change", processingUnitInstance.getSpaceInstance().waitForMode(SpaceMode.PRIMARY, 60000, TimeUnit.MILLISECONDS));
			}
		}

		assertPrimaries(admin, 2);
		assertBackups(admin, 0);
	}
	
	//Start GSM A, GSC A1, GSC A2
	//2 backups are started on A1 and/or A2
	private void startAllOnMachineA() {
		log("start GSM A, GSC A1, GSC A2");
		loadGSM(machineA);
		GridServiceContainer[] gscs = loadGSCs(machineA, 2);
		
		//2 backups are started on A1 and/or A2
		log("wait for processing units to instantiate ...");
		for (ProcessingUnit processingUnit : admin.getProcessingUnits()) {
			assertEquals("Unexpected deployment status", DeploymentStatus.INTACT, waitForDeploymentStatus(processingUnit, DeploymentStatus.INTACT));
		}
		
		//2 Spaces are available both backups
		log("wait for 2 Spaces are available both backups ...");
		for (GridServiceContainer gsc : gscs) {
			for (ProcessingUnitInstance processingUnitInstance : gsc.getProcessingUnitInstances()) {
				assertTrue("wait for BACKUP mode change", processingUnitInstance.getSpaceInstance().waitForMode(SpaceMode.BACKUP, 60000, TimeUnit.MILLISECONDS));
			}
		}
		assertPrimaries(admin, 2);
		assertBackups(admin, 2);
	}
	
	//Kill -9 GSM B, GSC B1, GSC B2
	//Space instances from B1, B2 are not re-deployed because of max-per-machine
	private void killAllOnMachineB() {
		
		GridServiceManager gsmA = machineA.getGridServiceManagers().getManagers()[0];
		GridServiceManager gsmB = machineB.getGridServiceManagers().getManagers()[0];
		
		GridServiceContainer[] containers = machineB.getGridServiceContainers().getContainers();
		GridServiceContainer gscB1 = containers[0];
		GridServiceContainer gscB2 = containers[1];
		
		//Kill -9 GSM B, GSC B1, GSC B2
		log("killing GSM B, GSC B1, GSC B2, containing: " + ProcessingUnitUtils.getProcessingUnitInstanceName(gscB1.getProcessingUnitInstances())
				+", " + ProcessingUnitUtils.getProcessingUnitInstanceName(gscB2.getProcessingUnitInstances()));
		gsmB.kill();
		gscB1.kill();
		gscB2.kill();
		
		log("wait for GSM A to manage processing units");
		for (ProcessingUnit pu : admin.getProcessingUnits()) {
            assertEquals("GSM A should manage all Processing Units", gsmA, waitForManaged(pu, gsmA));
        }
		
		//Space instances from B1, B2 are not re-deployed because of max-per-machine
		assertEquals("Expected 2 processing units", 2, admin.getProcessingUnits().getSize());
		for (ProcessingUnit processingUnit : admin.getProcessingUnits()) {
			assertEquals("Expected 1 processing units", 1, processingUnit.getInstances().length);
			assertEquals("Unexpected deployment status", DeploymentStatus.COMPROMISED, waitForDeploymentStatus(processingUnit, DeploymentStatus.COMPROMISED));
		}
		
		//2 Spaces are available both primary
		log("wait for active election of primaries ...");
		for (ProcessingUnit processingUnit : admin.getProcessingUnits()) {
			ProcessingUnitInstance[] instances = processingUnit.getInstances();
			for (ProcessingUnitInstance processingUnitInstance : instances) {
				assertTrue("wait for PRIMARY mode change", processingUnitInstance.getSpaceInstance().waitForMode(SpaceMode.PRIMARY, 60000, TimeUnit.MILLISECONDS));
			}
		}

		assertPrimaries(admin, 2);
		assertBackups(admin, 0);
	}
	
	//Start GSM B, GSC B1, GSC B2
	//2 backups are started on B1 and/or B2
	private void startAllOnMachineB() {
		
		log("start GSM B, GSC B1, GSC B2");
		loadGSM(machineB);
		GridServiceContainer[] gscs = loadGSCs(machineB, 2);
		
		//2 backups are started on B1 and/or B2
		log("wait for processing units to instantiate ...");
		for (ProcessingUnit processingUnit : admin.getProcessingUnits()) {
			assertEquals("Unexpected deployment status", DeploymentStatus.INTACT, waitForDeploymentStatus(processingUnit, DeploymentStatus.INTACT));
		}
		
		//2 Spaces are available both backups
		log("wait for 2 Spaces are available both backups ...");
		for (GridServiceContainer gsc : gscs) {
			for (ProcessingUnitInstance processingUnitInstance : gsc.getProcessingUnitInstances()) {
				assertTrue("wait for BACKUP mode change", processingUnitInstance.getSpaceInstance().waitForMode(SpaceMode.BACKUP, 60000, TimeUnit.MILLISECONDS));
			}
		}
		assertPrimaries(admin, 2);
		assertBackups(admin, 2);
	}
}
