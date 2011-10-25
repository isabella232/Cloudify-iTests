package test.servicegrid.failover;

import static test.utils.AdminUtils.loadGSCs;
import static test.utils.AdminUtils.loadGSMs;
import static test.utils.LogUtils.log;
import static test.utils.ProcessingUnitUtils.getProcessingUnitInstanceName;
import static test.utils.ProcessingUnitUtils.waitForDeploymentStatus;

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
 * Topology: 1 machine, 2 GSM 2 GSCs, all GSCs fail, provisioning when started GSC#1/#2
 * Tests that when all GSCs fail, all instances are re-provisioned when GSCs become available.
 * 
 * a. deploy cluster A 1,1 and cluster B 1,1
 * b. kill GSCs
 * c. verify that cluster A and B deployment status is broken
 * d. start GSCs
 * e. verify that cluster A and B are fully instantiated
 * 
 * @author Moran Avigdor
 */
public class GscsFailureAndProvisioningWhenAvailableTest extends AbstractTest {
	private Machine machine;
	private GridServiceManager gsm;
	private GridServiceContainer[] gscs;
	
	@BeforeMethod
	public void setup() {
		GridServiceAgent gsa = admin.getGridServiceAgents().waitForAtLeastOne();
		machine = gsa.getMachine();
		
		log("loading 2 GSM, 2 GSCs on " + machine.getHostName());
		gsm = loadGSMs(machine, 2)[0];
		gscs = loadGSCs(machine, 2);
	}
	
	@Test(timeOut=DEFAULT_TEST_TIMEOUT, groups = "1")
	public void test() throws InterruptedException {
		
		log("Deploying cluster A partitioned 1,1");
		ProcessingUnit puA = gsm.deploy(new SpaceDeployment("A").partitioned(1, 1));
		puA.waitFor(puA.getTotalNumberOfInstances());
		
		log("Deploying cluster B partitioned 1,1");
		ProcessingUnit puB = gsm.deploy(new SpaceDeployment("B").partitioned(1, 1));
		puB.waitFor(puB.getTotalNumberOfInstances());
		
		for (GridServiceContainer gsc : gscs) {
			assertEquals("Expected each GSC to contain 2 instance", 2, gsc.getProcessingUnitInstances().length);
		}
		
		log("killing GSCs #1 and #2");
		for (GridServiceContainer gsc : gscs) {
			log("killing GSC, containing: " + getProcessingUnitInstanceName(gsc.getProcessingUnitInstances()));
			gsc.kill();
		}
		
		log("wait for broken deployment status");
		assertEquals(DeploymentStatus.BROKEN, waitForDeploymentStatus(puA, DeploymentStatus.BROKEN));
		assertEquals(DeploymentStatus.BROKEN, waitForDeploymentStatus(puB, DeploymentStatus.BROKEN));
		
		log("starting 2 GSCs");
		gscs = loadGSCs(machine, 2);
		
		log("waiting for PUs to instantiate");
		puA.waitFor(puA.getTotalNumberOfInstances());
		puB.waitFor(puB.getTotalNumberOfInstances());
		
		assertEquals("Expected same managing GSM", gsm, puA.getManagingGridServiceManager());
		assertEquals("Expected same managing GSM", gsm, puB.getManagingGridServiceManager());
	}
}
