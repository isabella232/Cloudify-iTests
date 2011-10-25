package test.servicegrid.sla.maxPerMachine;

import static test.utils.AdminUtils.loadGSC;
import static test.utils.AdminUtils.loadGSCs;
import static test.utils.AdminUtils.loadGSM;
import static test.utils.LogUtils.log;
import static test.utils.ProcessingUnitUtils.getProcessingUnitInstanceName;
import static test.utils.ProcessingUnitUtils.waitForDeploymentStatus;
import static test.utils.SLAUtils.assertMaxPerMachine;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.DeploymentStatus;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.space.SpaceDeployment;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.AbstractTest;

/**
 * Topology: 2 machines, 1 GSM, 2 GSC per machine
 * Tests that when GSC fails, max-per-machine SLA is obeyed
 * 
 * 1. deploy partition 1,1 max-per-machine = 1
 * 2. verify 1 instance per GSC
 * 3. kill GSC #1
 * 4. verify that instance is not instantiated on GSC #2
 * 5. start GSC #1
 * 6. verify that instance is instantiated on GSC #1
 * 
 * @author Moran Avigdor
 */
public class GscFailureMaxPerMachineSlaTest extends AbstractTest {
	private Machine machineA, machineB;
	private GridServiceManager gsm;
	
	@BeforeMethod
	public void setup() {
		assertTrue(admin.getMachines().waitFor(2));
		assertTrue(admin.getGridServiceAgents().waitFor(2));

        GridServiceAgent[] agents = admin.getGridServiceAgents().getAgents();
        GridServiceAgent gsaA = agents[0];
        GridServiceAgent gsaB = agents[1];
        
        machineA = gsaA.getMachine();
        machineB = gsaB.getMachine();
		
		log("loading 1 GSM, 1 GSC on " + machineA.getHostName());
		gsm = loadGSM(machineA);
		loadGSCs(machineA,2); //GSC #1,#2 on machine A
		
		log("loading, 1 GSC on " + machineB.getHostName());
		loadGSCs(machineB, 2); //GSC #1,#2 on machine B
	}
	
	@Test(timeOut=DEFAULT_TEST_TIMEOUT, groups = "2")
	public void test() throws InterruptedException {
		
		log("Deploying cluster A partitioned 1,1 max-per-machine=1");
		ProcessingUnit pu = gsm.deploy(new SpaceDeployment("A").partitioned(1, 1).maxInstancesPerMachine(1));
		pu.waitFor(pu.getTotalNumberOfInstances());
		assertEquals("Expected only one instance deployed on machine A", 1, machineA.getProcessingUnitInstances().length);
		assertEquals("Expected only one instance deployed on machine B", 1, machineB.getProcessingUnitInstances().length);
		
		assertMaxPerMachine(admin);
		
		GridServiceContainer gscOnA = machineA.getProcessingUnitInstances()[0].getGridServiceContainer();
		assertEquals("Expected 1 instance on this GSC", 1, gscOnA.getProcessingUnitInstances().length);
		log("killing GSC #1, containing: " + getProcessingUnitInstanceName(gscOnA.getProcessingUnitInstances()));
		gscOnA.kill();
		
		log("wait for scheduled deployment status");
		assertEquals("unexpected deployment status", DeploymentStatus.SCHEDULED, waitForDeploymentStatus(pu, DeploymentStatus.SCHEDULED));
		assertEquals("Expected only one instance", 1, machineB.getProcessingUnitInstances().length);
		
		log("starting GSC #1");
		loadGSC(machineA);
		
		log("wait for instance to instantiate");
		pu.waitFor(pu.getTotalNumberOfInstances());
		assertEquals("Expected only one instance deployed on machine A", 1, machineA.getProcessingUnitInstances().length);
		assertEquals("Expected only one instance deployed on machine B", 1, machineB.getProcessingUnitInstances().length);
		
		assertMaxPerMachine(admin);
	}
}
