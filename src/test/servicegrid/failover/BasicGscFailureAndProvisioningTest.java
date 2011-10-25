package test.servicegrid.failover;

import static test.utils.AdminUtils.loadGSCs;
import static test.utils.AdminUtils.loadGSM;
import static test.utils.DistributionUtils.assertEvenlyDistributed;
import static test.utils.DistributionUtils.assertEvenlyDistributedPrimaries;
import static test.utils.LogUtils.log;
import static test.utils.ProcessingUnitUtils.waitForDeploymentStatus;
import static test.utils.SLAUtils.assertMaxPerMachine;
import static test.utils.SLAUtils.assertMaxPerVM;

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

/**
 * Topology:
 * - 1 GSM and 2 GSC at 2 machines
 * - Cluster A - partitioned-sync2backup 1,1 with max-per-machine=1 
 * - Cluster B - partitioned-sync2backup 1,1 with max-per-machine=1
 * 
 * TEST (1) Basic GSC failure and provisioning
 * 
 * Start GSM A, GSC A1, GSM B, GSC B1, GSC A2, GSC B2
 * Deploy Cluster A to GSM A 
 * Deploy Cluster B to GSM A
 * 
 * a. 2 clusters deployed, 4 spaces total
 * b. Spaces are evenly distributed across all 4 GSCs 
 * c. Kill -9 GSC A1 Space instances from A1 are deployed in GSC A2
 * d. 2 clusters are available, 4 spaces total
 */
public class BasicGscFailureAndProvisioningTest extends AbstractTest {
	
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
		loadGSM(machineB); //GSM B
		loadGSCs(machineB, 2); //GSC B1, GSC B2
		
		//Deploy Cluster A to GSM A
		log("deploying: 2 clusters A and B, partitioned 1,1");
		ProcessingUnit puA = gsmA.deploy(new SpaceDeployment("A").partitioned(1, 1).maxInstancesPerMachine(1).maxInstancesPerVM(0));
		ProcessingUnit puB = gsmA.deploy(new SpaceDeployment("B").partitioned(1, 1).maxInstancesPerMachine(1).maxInstancesPerVM(0));
		assertTrue(puA.waitFor(puA.getTotalNumberOfInstances()));
		assertTrue(puB.waitFor(puB.getTotalNumberOfInstances()));
	}
	
	@Test(timeOut=DEFAULT_TEST_TIMEOUT, groups = "2")
	public void test() {
		testDeploymentStatus();
		testDistribution();
		testKillGscA1();
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
	
	//Kill -9 GSC A1 Space instances from A1 are deployed in GSC A2
	private void testKillGscA1() {
		GridServiceContainer[] containers = machineA.getGridServiceContainers().getContainers();
		GridServiceContainer gscA1 = containers[0];
		GridServiceContainer gscA2 = containers[1];
		
		ProcessingUnitInstance[] instancesOnA1 = gscA1.getProcessingUnitInstances();
		log("killing GSC A1, containing: " + ProcessingUnitUtils.getProcessingUnitInstanceName(instancesOnA1[0]));
		gscA1.kill();

		log("waiting for instantiation on GSC A2");
		for (ProcessingUnitInstance processingUnitInstance : instancesOnA1) {
			assertTrue(gscA2.waitFor(processingUnitInstance.getName(), 1));
		}
	}
}
