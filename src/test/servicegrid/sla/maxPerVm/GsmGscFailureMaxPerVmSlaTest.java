package test.servicegrid.sla.maxPerVm;

import static test.utils.AdminUtils.loadGSC;
import static test.utils.AdminUtils.loadGSCs;
import static test.utils.AdminUtils.loadGSMs;
import static test.utils.LogUtils.log;
import static test.utils.ProcessingUnitUtils.getProcessingUnitInstanceName;
import static test.utils.ProcessingUnitUtils.waitForDeploymentStatus;
import static test.utils.ProcessingUnitUtils.waitForManaged;
import static test.utils.SLAUtils.assertMaxPerVM;

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
 * Topology: 1 machine, 2 GSMs 2 GSCs
 * Tests that when managing GSM fails and GSC fails, max-per-vm SLA is obeyed by backup GSM
 * 
 * 1. deploy partition 1,1 max-per-vm = 1
 * 2. verify 1 instance per GSC
 * 3. kill managing GSM and GSC #1
 * 4. verify that backup GSM is managing the PU
 * 5. verify that instance is not instantiated on GSC #2
 * 6. verify that the deployment status is compromised
 * 7. start GSC #1
 * 8. verify that instance is instantiated on GSC #1
 * 
 * @author Moran Avigdor
 */
public class GsmGscFailureMaxPerVmSlaTest extends AbstractTest {
	private Machine machine;
	private GridServiceManager[] gsms;
	private GridServiceContainer[] gscs;
	
	@BeforeMethod
	public void setup() {
		GridServiceAgent gsa = admin.getGridServiceAgents().waitForAtLeastOne();
		machine = gsa.getMachine();
		
		log("loading 2 GSM, 2 GSCs on " + machine.getHostName());
		gsms = loadGSMs(machine, 2);
		gscs = loadGSCs(machine, 2);
	}
	
	@Test(timeOut=DEFAULT_TEST_TIMEOUT, groups = "1")
	public void test() throws InterruptedException {
		
		log("Deploying cluster A partitioned 1,1 max-per-vm=1");
		ProcessingUnit pu = gsms[0].deploy(new SpaceDeployment("A").partitioned(1, 1).maxInstancesPerVM(1));
		pu.waitFor(pu.getTotalNumberOfInstances());
		for (GridServiceContainer gsc : gscs) {
			assertEquals("Expected each GSC to contain 1 instance", 1, gsc.getProcessingUnitInstances().length);
		}
		
		assertMaxPerVM(admin);
		
		log("killing managing GSM #1");
		assertEquals("expected GSM to manage pu A", gsms[0], pu.getManagingGridServiceManager());
		gsms[0].kill();
		
		log("killing GSC #1, containing: " + getProcessingUnitInstanceName(gscs[0].getProcessingUnitInstances()));
		gscs[0].kill();
		
		log("wait for compromised deployment status");
		assertEquals("unexpected deployment status", DeploymentStatus.COMPROMISED, waitForDeploymentStatus(pu, DeploymentStatus.COMPROMISED));
		assertEquals("Expected only one instance", 1, gscs[1].getProcessingUnitInstances().length);
		
		log("waiting for backup GSM to manage processing unit");
		assertEquals("expected GSM to manage pu A", gsms[1], waitForManaged(pu, gsms[1]));
		
		log("starting GSC #1");
		gscs[0] = loadGSC(machine);
		
		log("wait for instance to instantiate");
		pu.waitFor(pu.getTotalNumberOfInstances());
		for (GridServiceContainer gsc : gscs) {
			assertEquals("Expected each GSC to contain 1 instance", 1, gsc.getProcessingUnitInstances().length);
		}
		
		assertEquals("Expected intact deployment status", DeploymentStatus.INTACT, waitForDeploymentStatus(pu, DeploymentStatus.INTACT));
		assertEquals("Expected all instances to be provisioned", 2, machine.getProcessingUnitInstances().length);
		assertMaxPerVM(admin);
	}
}
