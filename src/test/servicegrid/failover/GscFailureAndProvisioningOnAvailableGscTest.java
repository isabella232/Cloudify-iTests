package test.servicegrid.failover;

import static test.utils.AdminUtils.loadGSCs;
import static test.utils.AdminUtils.loadGSMs;
import static test.utils.AdminUtils.waitForRegisteredGSCs;
import static test.utils.LogUtils.log;
import static test.utils.ProcessingUnitUtils.getProcessingUnitInstanceName;

import java.util.concurrent.CountDownLatch;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.openspaces.admin.pu.events.ProcessingUnitInstanceAddedEventListener;
import org.openspaces.admin.space.SpaceDeployment;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.AbstractTest;

/**
 * Topology: 1 machine, 2 GSM 2 GSCs
 * Tests GSC failure and provisioning of it's pu instances on an available GSC.
 * 
 * a. deploy partitioned 1,1
 * b. kill GSC #1
 * c. provisioning on GSC #2
 * 
 * @author Moran Avigdor
 */
public class GscFailureAndProvisioningOnAvailableGscTest extends AbstractTest {

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
		assertTrue("timedout waiting for GSCs to Register with GSM" , waitForRegisteredGSCs(gsm, 2));
	}
	
	@Test(timeOut=DEFAULT_TEST_TIMEOUT, groups = "1")
	public void test() throws InterruptedException {
		
		log("Deploying cluster A partitioned 1,1");
		ProcessingUnit pu = gsm.deploy(new SpaceDeployment("A").partitioned(1, 1));
		pu.waitFor(pu.getTotalNumberOfInstances());
		for (GridServiceContainer gsc : gscs) {
			assertEquals("Expected each GSC to contain 1 instance", 1, gsc.getProcessingUnitInstances().length);
		}
		
		final CountDownLatch addedLatch = new CountDownLatch(pu.getTotalNumberOfInstances());
		gscs[1].getProcessingUnitInstanceAdded().add(new ProcessingUnitInstanceAddedEventListener() {
			
			public void processingUnitInstanceAdded(
					ProcessingUnitInstance processingUnitInstance) {
				addedLatch.countDown();				
			}
		});

		log("killing GSC #1, containing: " + getProcessingUnitInstanceName(gscs[0].getProcessingUnitInstances()));
		gscs[0].kill();
		
		log("waiting for processing units to be instantiated on GSC #2");
		addedLatch.await();
		
		assertEquals("Expected processing units to be provisioned on GSC #2", 2, gscs[1].getProcessingUnitInstances().length);
		assertEquals("Expected same managing GSM", gsm, pu.getManagingGridServiceManager());
	}
}

