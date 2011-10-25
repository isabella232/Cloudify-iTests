package test.servicegrid.failover;

import static test.utils.AdminUtils.loadGSC;
import static test.utils.AdminUtils.loadGSCs;
import static test.utils.AdminUtils.loadGSM;
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
 * Topology: 1 machine, 1 GSM, 2 GSCs
 * Tests that failover of processing units from one GSC are deployed onto the new clean GSC 
 * 
 * 1. deploy cluster of 2,1
 * 2. each GSC should have at most 2 instances
 * 3. start GSC #3
 * 4. kill one of the GSCs
 * 4. all instances should be instantiated on GSC #3
 *  
 * @author Moran Avigdor
 */
public class GscFailoverToNewestCleanGscTest extends AbstractTest {
	
	private Machine machine;
	private GridServiceManager gsm;
	private GridServiceContainer[] gscs;
	
	@BeforeMethod
	public void setup() {
		GridServiceAgent gsa = admin.getGridServiceAgents().waitForAtLeastOne();
		machine = gsa.getMachine();
		
		log("loading 1 GSM, 2 GSCs on " + machine.getHostName());
		gsm = loadGSM(machine);
		gscs = loadGSCs(machine, 2);
	}
	
	@Test(timeOut=DEFAULT_TEST_TIMEOUT, groups = "1")
	public void test() throws InterruptedException {
		
		log("Deploying cluster A partitioned 2,1");
		ProcessingUnit pu = gsm.deploy(new SpaceDeployment("A").partitioned(2, 1));
		pu.waitFor(pu.getTotalNumberOfInstances());
		for (GridServiceContainer gsc : gscs) {
			assertEquals("Expected each GSC to contain 2 instances", 2, gsc.getProcessingUnitInstances().length);
		}
		
		log("start a new clean GSC (GSC #3)");
		GridServiceContainer gsc3 = loadGSC(machine);
		
		final CountDownLatch addedLatch = new CountDownLatch(gscs[0].getProcessingUnitInstances().length);
		gsc3.getProcessingUnitInstanceAdded().add(new ProcessingUnitInstanceAddedEventListener() {
			
			public void processingUnitInstanceAdded(
					ProcessingUnitInstance processingUnitInstance) {
				addedLatch.countDown();				
			}
		});

		log("killing GSC #1, containing: " + getProcessingUnitInstanceName(gscs[0].getProcessingUnitInstances()));
		gscs[0].kill();
		
		log("waiting for processing units to be instantiated on GSC #3");
		addedLatch.await();
		
		assertEquals("Expected processing units to be provisioned on GSC #3", 2, gsc3.getProcessingUnitInstances().length);
		assertEquals("Expected same managing GSM", gsm, pu.getManagingGridServiceManager());
	}
}
