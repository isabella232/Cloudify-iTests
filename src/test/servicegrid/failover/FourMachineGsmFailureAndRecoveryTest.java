package test.servicegrid.failover;

import static org.testng.AssertJUnit.fail;
import static test.utils.AdminUtils.loadGSCs;
import static test.utils.AdminUtils.loadGSM;
import static test.utils.DistributionUtils.assertBackups;
import static test.utils.DistributionUtils.assertEvenlyDistributed;
import static test.utils.DistributionUtils.assertEvenlyDistributedPrimaries;
import static test.utils.DistributionUtils.assertPrimaries;
import static test.utils.LogUtils.log;
import static test.utils.ProcessingUnitUtils.waitForDeploymentStatus;
import static test.utils.SLAUtils.assertMaxPerMachine;
import static test.utils.SLAUtils.assertMaxPerVM;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.gsc.GridServiceContainers;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.DeploymentStatus;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.openspaces.admin.pu.ProcessingUnits;
import org.openspaces.admin.pu.events.ProcessingUnitInstanceRemovedEventListener;
import org.openspaces.admin.space.SpaceDeployment;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.AbstractTest;
import test.utils.AdminUtils;
import test.utils.ProcessingUnitUtils;

import com.gigaspaces.cluster.activeelection.SpaceMode;

/**
 * Topology: (4 machines)
 * - 1 GSM and 4 GSC at 2 machines
 * - 4 GSCS at 2 machines
 * - Cluster A - partitioned-sync2backup 4,1 with max-per-machine=1 
 * - Cluster B - partitioned-sync2backup 4,1 with max-per-machine=1
 * 
 * TEST (8) Machine GSM Failure and recovery
 * 
 * Start
 * GSM A, GSC A1, GSC A2, GSC A3, GSC A4
 * GSM B, GSC B1, GSC B2, GSC B3, GSC B4
 * GSC C1, GSC C2, GSC C3, GSC C4
 * GSC D1, GSC D2, GSC D3, GSC D4
 * 
 * Deploy
 * Cluster A - partitioned-sync2backup 4,1 with max-per-machine=1 via GSM A
 * Cluster B - partitioned-sync2backup 4,1 with max-per-machine=1 via GSM A
 * 
 * a. spaces are evenly distributed across all 16 GSCs
 * b. 2 clusters are available, 16 spaces total
 * c. Kill -9 GSC D1, GSC D2, GSC D3, and GSC D4
 * 	  Spaces are evenly distributed across all 12 GSCs
 * d. 2 clusters are available, 16 spaces total
 * e. Kill -9 GSC C1, GSC C2, GSC C3, GSC C4
 *    Spaces are evenly distributed across all 8 GSCs
 * f. 2 clusters are available, 16 spaces total
 * g. Kill -9 GSM B, GSC B1, GSC B2, GSC B3, and GSC B4
 *    Space instances from B1, B2, B3, and B4 are not re-deployed because of max-per-machine.
 * h. 2 clusters are available, only 8 primaries available.
 * i. Start GSM B, GSC B1, GSC B2, GSC B3, GSC B4
 *    Space instances are started on B1, B2, B3 and/or B4
 * j. 2 clusters are available, 16 spaces total
 * i. Kill -9 GSM A, GSC A1, GSC A2, GSC A3, GSC A4
 *    Space instances from A1, A2, A3, and A4 are not re-deployed because of max-per-machine
 * j. 2 clusters are available, 8 primaries available only.
 * k. Start GSM A, GSC A1, GSC A2, GSC A3, GSC A4
 *    space instances are started on A1, A2, A3 and/or A4
 * h. 2 clusters are available, 16 spaces total
 */
public class FourMachineGsmFailureAndRecoveryTest extends AbstractTest {
	protected Machine machineA, machineB, machineC, machineD;
	
	@BeforeMethod
	public void setup() {
		log("waiting for 4 machines");
		admin.getMachines().waitFor(4);

		log("waiting for 4 GSAs");
		admin.getGridServiceAgents().waitFor(4);
		GridServiceAgent[] agents = admin.getGridServiceAgents().getAgents();
		
		//Start GSM A, GSC A1, GSC A2, GSC A3, GSC A4
		log("starting: GSM A, GSC A1, GSC A2, GSC A3, GSC A4");
		GridServiceAgent gsaA = agents[0];
		GridServiceManager gsmA = loadGSM(gsaA);
		loadGSCs(gsaA, 4);
		machineA = gsaA.getMachine();
		
		//Start GSM B, GSC B1, GSC B2, GSC B3, GSC B4
		log("starting: GSM B, GSC B1, GSC B2, GSC B3, GSC B4");
		GridServiceAgent gsaB = agents[1];
		loadGSM(gsaB);
		loadGSCs(gsaB, 4);
		machineB = gsaB.getMachine();
		
		//Start GSC C1, GSC C2, GSC C3, GSC C4
		log("starting: GSC C1, GSC C2, GSC C3, GSC C4");
		GridServiceAgent gsaC = agents[2];
		loadGSCs(gsaC, 4);
		machineC = gsaC.getMachine();
		
		//Start GSC D1, GSC D2, GSC D3, GSC D4
		log("starting: GSC D1, GSC D2, GSC D3, GSC D4");
		GridServiceAgent gsaD = agents[3];
		loadGSCs(gsaD, 4);
		machineD = gsaD.getMachine();
		
		log("Deploy - Cluster A - partitioned-sync2backup 4,1 with max-per-machine=1 via GSM A");
		ProcessingUnit puA = gsmA.deploy(new SpaceDeployment("A").partitioned(4, 1).maxInstancesPerMachine(1).maxInstancesPerVM(0));
		
		log("Deploy - Cluster B - partitioned-sync2backup 4,1 with max-per-machine=1 via GSM A");
		ProcessingUnit puB = gsmA.deploy(new SpaceDeployment("B").partitioned(4, 1).maxInstancesPerMachine(1).maxInstancesPerVM(0));
		
		assertTrue(puA.waitFor(puA.getTotalNumberOfInstances()));
		assertTrue(puB.waitFor(puB.getTotalNumberOfInstances()));
	}
	
	@Test(timeOut=DEFAULT_TEST_TIMEOUT, groups = "4")
	public void test() throws Exception {
		testDeploymentStatus();
		testDistribution();
		
		killGSCsOnMachineD();
		testDistributionAfterFailover();
		testDeploymentStatus();
		
		killGSCsOnMachineC();
		testDistributionAfterFailover();
		testDeploymentStatus();
		
		killGSMAndGSCsOnMachineB();
		startGSMAndGSCsOnMachineB();
		testDeploymentStatus();
		
		killGSMAndGSCsOnMachineA();
		startGSMAndGSCsOnMachineA();
		testDeploymentStatus();
	}

	//2 processing units are deployed
	private void testDeploymentStatus() {
		assertEquals("Expected 2 processing units", 2, admin.getProcessingUnits().getSize());
		for (ProcessingUnit processingUnit : admin.getProcessingUnits()) {
			assertEquals("Expected 8 processing units", 8, processingUnit.getInstances().length);
			assertEquals("Expected partition 4,1", 4, processingUnit.getPartitions().length);
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
	
	//SLA is obeyed after fail-over
	private void testDistributionAfterFailover() {
		assertMaxPerMachine(admin);
		assertMaxPerVM(admin);
	}
	
	//Kill -9 GSMs on this machine
	private void killGSMsOnMachine(Machine machine) {
		for (GridServiceManager gsm : machine.getGridServiceManagers()) {
			gsm.kill();
		}
	}
	
	//Kill -9 GSCs on this machine
	protected void killGSCsOnMachine(Machine machine, boolean intact) {
		ProcessingUnits processingUnits = admin.getProcessingUnits();
		ProcessingUnitInstance[] puInstancesOnD = machine.getProcessingUnitInstances();
		final CountDownLatch removedLatch = new CountDownLatch(puInstancesOnD.length);
		processingUnits.getProcessingUnitInstanceRemoved().add(new ProcessingUnitInstanceRemovedEventListener() {
			public void processingUnitInstanceRemoved(
					ProcessingUnitInstance processingUnitInstance) {
				removedLatch.countDown();				
			}
		});
		
		GridServiceContainers gridServiceContainers = machine.getGridServiceContainers();
		for (GridServiceContainer gsc : gridServiceContainers) {
			log("killing GSC, containing: " + ProcessingUnitUtils.getProcessingUnitInstanceName(gsc.getProcessingUnitInstances()));
			gsc.kill();
		}
		
		try {
			assertTrue("expired removal of processing units", removedLatch.await(15*60, TimeUnit.SECONDS));
		} catch (InterruptedException e) {
			fail(e.toString());
		}
		
		if (intact) {
			log("waiting for instantiation of PUs");
			for (ProcessingUnit pu : processingUnits) {
				assertTrue(pu.waitFor(pu.getTotalNumberOfInstances()));
			}
		}
	}
	
	//Kill -9 GSC D1, GSC D2, GSC D3, and GSC D4
	//Spaces are evenly distributed across all 12 GSCs
	private void killGSCsOnMachineD() {
		log("Kill -9 GSC D1, GSC D2, GSC D3, and GSC D4");
		killGSCsOnMachine(machineD, true /*intact*/);
		
		assertPrimaries(admin, 8);
		assertBackups(admin, 8);
	}
	
	//Kill -9 GSC C1, GSC C2, GSC C3, GSC C4
	//Spaces are evenly distributed across all 8 GSCs
	private void killGSCsOnMachineC() {
		log("Kill -9 GSC C1, GSC C2, GSC C3, GSC C4");
		killGSCsOnMachine(machineC, true /*intact*/);
		
		assertPrimaries(admin, 8);
		assertBackups(admin, 8);
	}
	
	//Kill -9 GSM B, GSC B1, GSC B2, GSC B3, and GSC B4
	//Space instances from B1, B2, B3, and B4 are not re-deployed because of max-per-machine.
	private void killGSMAndGSCsOnMachineB() {
		log("Kill -9 GSM B, GSC B1, GSC B2, GSC B3, and GSC B4");
		ProcessingUnitInstance[] puInstancesOnMachineB = machineB.getProcessingUnitInstances();
		killGSMsOnMachine(machineB);
		killGSCsOnMachine(machineB, false /*intact*/);
		
		//Space instances from B1, B2, B3, and B4 are not re-deployed because of max-per-machine.
		assertEquals("Expected 2 processing units", 2, admin.getProcessingUnits().getSize());
		for (ProcessingUnitInstance puInstance : puInstancesOnMachineB) {
			ProcessingUnit processingUnit = admin.getProcessingUnits().getProcessingUnit(puInstance.getName());
			assertEquals("Expected 4 processing unit instances", 4, processingUnit.getInstances().length);
			assertEquals("Unexpected deployment status", DeploymentStatus.COMPROMISED, waitForDeploymentStatus(processingUnit, DeploymentStatus.COMPROMISED));
		}
		
		//8 Spaces are available both primary
		log("wait for active election of primaries ...");
		for (ProcessingUnit processingUnit : admin.getProcessingUnits()) {
			ProcessingUnitInstance[] instances = processingUnit.getInstances();
			for (ProcessingUnitInstance processingUnitInstance : instances) {
				assertTrue(
						"wait for PRIMARY mode change for "
								+ ProcessingUnitUtils
										.getProcessingUnitInstanceName(processingUnitInstance),
						processingUnitInstance.getSpaceInstance()
								.waitForMode(SpaceMode.PRIMARY, 60000,
										TimeUnit.MILLISECONDS));
			}
		}
		
		assertPrimaries(admin, 8);
		assertBackups(admin, 0);
	}
	
	//Start GSM B, GSC B1, GSC B2, GSC B3, GSC B4
	//Space instances are started on B1, B2, B3 and/or B4
	private void startGSMAndGSCsOnMachineB() throws Exception {
		
		log("Start GSM B, GSC B1, GSC B2, GSC B3, GSC B4");
		AdminUtils.loadGSM(machineB);
		GridServiceContainer[] gscs = AdminUtils.loadGSCs(machineB, 4);
		
		//8 backups are started on B1, B2, B3 and/or B4
		log("wait for processing units to instantiate ...");
		for (ProcessingUnit processingUnit : admin.getProcessingUnits()) {
			processingUnit.waitFor(processingUnit.getTotalNumberOfInstances());
		}
		
		log("wait for INTACT deployment status");
		for (ProcessingUnit processingUnit : admin.getProcessingUnits()) {
			assertEquals("Unexpected deployment status", DeploymentStatus.INTACT, waitForDeploymentStatus(processingUnit, DeploymentStatus.INTACT));
		}
		
		log("wait until 8 backup Spaces are available");
		for (GridServiceContainer gsc : gscs) {
			for (ProcessingUnitInstance processingUnitInstance : gsc.getProcessingUnitInstances()) {
				assertTrue("wait for BACKUP mode change", processingUnitInstance.getSpaceInstance().waitForMode(SpaceMode.BACKUP, 60000, TimeUnit.MILLISECONDS));
			}
		}
		
		assertPrimaries(admin, 8);
		assertBackups(admin, 8);
	}
	
	//Kill -9 GSM A, GSC A1, GSC A2, GSC A3, and GSC A4
	//Space instances from A1, A2, A3, and A4 are not re-deployed because of max-per-machine.
	private void killGSMAndGSCsOnMachineA() {
		log("Kill -9 GSM A, GSC A1, GSC A2, GSC A3, and GSC A4");
		ProcessingUnitInstance[] puInstancesOnMachineB = machineA.getProcessingUnitInstances();
		killGSMsOnMachine(machineA);
		killGSCsOnMachine(machineA, false /*intact*/);
		
		//wait for GSM B to manage all processing units managed by GSM A
		GridServiceManager gsmB = machineB.getGridServiceManagers().getManagers()[0];
		for (ProcessingUnitInstance puInstance : puInstancesOnMachineB) {
			ProcessingUnitUtils.waitForManaged(puInstance.getProcessingUnit(), gsmB);
		}
		
		//Space instances from A1, A2, A3, and A4 are not re-deployed because of max-per-machine.
		assertEquals("Expected 2 processing units", 2, admin.getProcessingUnits().getSize());
		for (ProcessingUnitInstance puInstance : puInstancesOnMachineB) {
			ProcessingUnit processingUnit = admin.getProcessingUnits().getProcessingUnit(puInstance.getName());
			assertEquals("Expected 4 processing unit instances", 4, processingUnit.getInstances().length);
			assertEquals("Unexpected deployment status", DeploymentStatus.COMPROMISED, waitForDeploymentStatus(processingUnit, DeploymentStatus.COMPROMISED));
		}
		
		//8 Spaces are available both primary
		log("wait for active election of primaries ...");
		for (ProcessingUnit processingUnit : admin.getProcessingUnits()) {
			ProcessingUnitInstance[] instances = processingUnit.getInstances();
			for (ProcessingUnitInstance processingUnitInstance : instances) {
				assertTrue(
						"wait for PRIMARY mode change for "
								+ ProcessingUnitUtils
										.getProcessingUnitInstanceName(processingUnitInstance),
						processingUnitInstance.getSpaceInstance()
								.waitForMode(SpaceMode.PRIMARY, 60000,
										TimeUnit.MILLISECONDS));
			}
		}
		
		assertPrimaries(admin, 8);
		assertBackups(admin, 0);
	}
	
	//Start GSM A, GSC A1, GSC A2, GSC A3, GSC A4
	//Space instances are started on A1, A2, A3 and/or A4
	private void startGSMAndGSCsOnMachineA() throws Exception {
		
		log("Start GSM A, GSC A1, GSC A2, GSC A3, GSC A4");
		AdminUtils.loadGSM(machineA);
		GridServiceContainer[] gscs = AdminUtils.loadGSCs(machineA, 4);
		
		//8 backups are started on A1, A2, A3 and/or A4
		log("wait for processing units to instantiate ...");
		for (ProcessingUnit processingUnit : admin.getProcessingUnits()) {
			assertEquals("Unexpected deployment status", DeploymentStatus.INTACT, waitForDeploymentStatus(processingUnit, DeploymentStatus.INTACT));
		}
		
		
		//8 Spaces are available both backups
		log("wait until 8 Spaces are available both backups ...");
		for (GridServiceContainer gsc : gscs) {
			for (ProcessingUnitInstance processingUnitInstance : gsc.getProcessingUnitInstances()) {
				assertTrue("wait for BACKUP mode change", processingUnitInstance.getSpaceInstance().waitForMode(SpaceMode.BACKUP, 60000, TimeUnit.MILLISECONDS));
			}
		}
		
		assertPrimaries(admin, 8);
		assertBackups(admin, 8);
	}
}
