package test.servicegrid.failover;

import static test.utils.AdminUtils.loadGSCs;
import static test.utils.AdminUtils.loadGSMs;
import static test.utils.LogUtils.log;
import static test.utils.ProcessingUnitUtils.getProcessingUnitInstanceName;
import static test.utils.ProcessingUnitUtils.waitForDeploymentStatus;
import static test.utils.ProcessingUnitUtils.waitForManaged;

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
 * Topology: 1 machine, 2 GSM 2 GSCs
 * Tests that when managing GSM fails and all GSCs fail, all instances are re-provisioned by backup GSM when GSCs become available.
 * 
 * 1. deploy cluster A 1,1 and cluster B 1,1
 * 2. kill managing GSM and all GSCs
 * 3. verify that cluster A and B deployment status is broken
 * 4. verify that backup GSM takes over management of failed service
 * 5. start GSCs
 * 6. verify that cluster A and B are fully instantiated
 * 7. verify that deployment status is intact
 * 
 * @author Moran Avigdor
 */
public class GsmGscsFailureAndProvisioningWhenAvailableTest extends AbstractTest {
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
		
		log("Deploying cluster A partitioned 1,1");
		ProcessingUnit puA = gsms[0].deploy(new SpaceDeployment("A").partitioned(1, 1));
		puA.waitFor(puA.getTotalNumberOfInstances());
		
		log("Deploying cluster B partitioned 1,1");
		ProcessingUnit puB = gsms[0].deploy(new SpaceDeployment("B").partitioned(1, 1));
		puB.waitFor(puB.getTotalNumberOfInstances());
		
		for (GridServiceContainer gsc : gscs) {
			assertEquals("Expected each GSC to contain 2 instance", 2, gsc.getProcessingUnitInstances().length);
		}
		
		log("killing managing GSM #1");
		assertEquals("expected GSM to manage pu A", gsms[0], puA.getManagingGridServiceManager());
		assertEquals("expected GSM to manage pu B", gsms[0], puB.getManagingGridServiceManager());
		gsms[0].kill();
		
		log("killing GSCs #1 and #2");
		for (GridServiceContainer gsc : gscs) {
			log("killing GSC, containing: " + getProcessingUnitInstanceName(gsc.getProcessingUnitInstances()));
			gsc.kill();
		}
		
		log("wait for broken deployment status");
		assertEquals("Expected broken deployment status", DeploymentStatus.BROKEN, waitForDeploymentStatus(puA, DeploymentStatus.BROKEN));
		assertEquals("Expected broken deployment status", DeploymentStatus.BROKEN, waitForDeploymentStatus(puB, DeploymentStatus.BROKEN));
		
		log("waiting for backup GSM to manage processing unit");
		assertEquals("expected GSM to manage pu A", gsms[1], waitForManaged(puA, gsms[1]));
		assertEquals("expected GSM to manage pu B", gsms[1], waitForManaged(puB, gsms[1]));
		
		log("starting 2 GSCs");
		gscs = loadGSCs(machine, 2);
		
		log("waiting for PUs to instantiate");
		puA.waitFor(puA.getTotalNumberOfInstances());
		puB.waitFor(puB.getTotalNumberOfInstances());
		
		assertEquals("Expected intact deployment status", DeploymentStatus.INTACT, waitForDeploymentStatus(puA, DeploymentStatus.INTACT));
		assertEquals("Expected intact deployment status", DeploymentStatus.INTACT, waitForDeploymentStatus(puB, DeploymentStatus.INTACT));
		assertEquals("Expected all instances to be provisioned", 4, machine.getProcessingUnitInstances().length);
	}
}
