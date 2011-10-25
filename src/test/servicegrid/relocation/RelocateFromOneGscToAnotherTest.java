package test.servicegrid.relocation;

import static org.testng.AssertJUnit.assertFalse;
import static test.utils.AdminUtils.loadGSC;
import static test.utils.AdminUtils.loadGSCs;
import static test.utils.AdminUtils.loadGSM;
import static test.utils.LogUtils.log;
import static test.utils.ProcessingUnitUtils.getProcessingUnitInstanceName;
import static test.utils.ProcessingUnitUtils.waitForDeploymentStatus;
import static test.utils.ToStringUtils.gscToString;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.DeploymentStatus;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.openspaces.admin.pu.events.ProcessingUnitInstanceAddedEventListener;
import org.openspaces.admin.space.SpaceDeployment;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.AbstractTest;

/**
 * Topology: 1 machine, 1 GSM and 2 GSCs
 * Tests that relocation is successful when relocating to a GSC
 * 
 * Test-1 test single space relocation
 * 	a. deploy single space
 *  b. relocate single space to empty GSC
 *  c. verify successful instantiation
 *  d. kill GSC with relocated instance
 *  e. instance should instantiate on available GSC
 *  f. start GSC
 *  g. verify no extra instance is instantiated
 * 
 * Test-2 test partition space relocation (max-per-vm clear)
 *  a. deploy partitioned 1,1
 *  b. relocate one instance from one GSC to the other
 *  c. verify successful instantiation
 *  d. kill GSC with relocated instance
 *  e. both instances should be instantiated on available GSC
 *  f. start GSC
 *  g. verify no extra instance is instantiated
 * 
 * @author Moran Avigdor
 */
public class RelocateFromOneGscToAnotherTest extends AbstractTest {

	private Machine machine;
	private GridServiceManager gsm;
	private GridServiceContainer[] gscs;

	@BeforeMethod
	public void setup() {
		GridServiceAgent gsa = admin.getGridServiceAgents().waitForAtLeastOne();
		machine = gsa.getMachine();
		gsm = loadGSM(machine);
		gscs = loadGSCs(machine, 2);
	}
	
	@Test(timeOut=DEFAULT_TEST_TIMEOUT, groups = "1")
	public void testSingleSpaceRelocation() throws Exception {
		ProcessingUnit pu = gsm.deploy(new SpaceDeployment("A"));
		log("wait for processing unit to instantiate");
		pu.waitFor(1);
		
		doRelocateTest(pu);
	}
	
	@Test(timeOut=DEFAULT_TEST_TIMEOUT, groups = "1")
	public void testPartititionedSpaceRelocation() throws Exception {
		ProcessingUnit pu = gsm.deploy(new SpaceDeployment("P").partitioned(1, 1));
		log("wait for processing unit to instantiate");
		pu.waitFor(2);
		
		assertEquals("1 instance on each GSC", 1, gscs[0].getProcessingUnitInstances().length);
		assertEquals("1 instance on each GSC", 1, gscs[1].getProcessingUnitInstances().length);
		
		doRelocateTest(pu);
	}

	private void doRelocateTest(ProcessingUnit pu) throws InterruptedException {

		final int totalInstances = pu.getTotalNumberOfInstances();
		
		//find out where pu is instantiated
		final GridServiceContainer gsc0 = machine.getProcessingUnitInstances()[0].getGridServiceContainer();
		final GridServiceContainer gsc1 = gscs[0].equals(gsc0) ? gscs[1] : gscs[0];
		
		final CountDownLatch gsc0AddedLatch = new CountDownLatch(totalInstances +1);
		gsc0.getProcessingUnitInstanceAdded().add(new ProcessingUnitInstanceAddedEventListener() {
			public void processingUnitInstanceAdded(
					ProcessingUnitInstance processingUnitInstance) {
				log("event: " + getProcessingUnitInstanceName(processingUnitInstance)
						+ " instantiated on " + gscToString(gsc0));
				if (gsc0AddedLatch.getCount()==(totalInstances+1)) {
					log("Relocating "
							+ getProcessingUnitInstanceName(processingUnitInstance)
							+ " from " + gscToString(gsc0) + " to "
							+ gscToString(gsc1));
					processingUnitInstance.relocate(gsc1);
					gsc0AddedLatch.countDown();
				} else {
					assertFalse(gsc0AddedLatch.getCount() == 0);
					gsc0AddedLatch.countDown();
				}
			}
		});
		
		final CountDownLatch gsc1AddedLatch = new CountDownLatch(totalInstances);
		gsc1.getProcessingUnitInstanceAdded().add(new ProcessingUnitInstanceAddedEventListener() {
			public void processingUnitInstanceAdded(
					ProcessingUnitInstance processingUnitInstance) {
						log("event: " + getProcessingUnitInstanceName(processingUnitInstance)
								+ " instantiated on " + gscToString(gsc1));
						assertFalse(gsc1AddedLatch.getCount() == 0);
						gsc1AddedLatch.countDown();
			}
		});
		
		/*
		 * relocate one instance from one GSC to the other - see "added" event
		 */
		log("waiting for instance to be relocated to " + gscToString(gsc1));
		gsc1AddedLatch.await();
		
		/*
		 * kill GSC with relocated instance. Instance/s should be instantiated on available GSC
		 */
		log("kill "+gscToString(gsc1)+" with relocated instance");
		log(gscToString(gsc1)+" contains: " + getProcessingUnitInstanceName(gsc1.getProcessingUnitInstances()));
		gsc1.kill();
		
		log("waiting for instance/s to be instantiated on " + gscToString(gsc0));
		gsc0AddedLatch.await();
		
		assertEquals("deployment status", DeploymentStatus.INTACT, waitForDeploymentStatus(pu, DeploymentStatus.INTACT));
		assertEquals("extra instance!", totalInstances, machine.getProcessingUnitInstances().length);
		
		/*
		 * start GSC, verify no extra instance is instantiated
		 */
		GridServiceContainer newGsc = loadGSC(machine);
		final CountDownLatch newGscAddedLatch = new CountDownLatch(1);
		newGsc.getProcessingUnitInstanceAdded().add(new ProcessingUnitInstanceAddedEventListener() {
			
			public void processingUnitInstanceAdded(
					ProcessingUnitInstance processingUnitInstance) {
				newGscAddedLatch.countDown();
			}
		});
		
		assertFalse("extra instance!", newGscAddedLatch.await(10, TimeUnit.SECONDS));
	}
}
