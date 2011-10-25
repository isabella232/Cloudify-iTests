package test.servicegrid.relocation;

import static test.utils.AdminUtils.loadGSCs;
import static test.utils.AdminUtils.loadGSMs;
import static test.utils.LogUtils.log;
import static test.utils.ProcessingUnitUtils.getProcessingUnitInstanceName;
import static test.utils.ProcessingUnitUtils.waitForDeploymentStatus;
import static test.utils.ToStringUtils.puInstanceToString;

import java.util.concurrent.CountDownLatch;

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
 * Topology: 1 machine, 2 GSMs, 2 GSCs
 * Tests that when all GSCs fail, all relocated instances are re-provisioned when GSCs become available.
 * 
 * a. deploy single space "S"
 * b. deploy partitioned space "P" (max-per-vm clear)
 * c. relocate instances from one GSC to the next
 * d. kill all GSCs
 * e. start 2 GSCs
 * f. verify that single space is instantiated
 * g. verify that partitioned primary and backup are instantiated
 * 
 * @author Moran Avigdor
 */
public class RelocateAndProvisioningWhenAvialableTest extends AbstractTest {
	private Machine machine;
	private GridServiceManager gsm;
	private GridServiceContainer[] gscs;

	@BeforeMethod
	public void setup() {
		GridServiceAgent gsa = admin.getGridServiceAgents().waitForAtLeastOne();
		machine = gsa.getMachine();
		gsm = loadGSMs(machine, 2)[0];
		gscs = loadGSCs(machine, 2);
	}
	
	@Test(timeOut=DEFAULT_TEST_TIMEOUT, groups = "1")
	public void test() throws Exception {
		/*
		 * deploy single space
		 */
		ProcessingUnit puS = gsm.deploy(new SpaceDeployment("S"));
		log("wait for " + puS.getName() + " to instantiate");
		puS.waitFor(1);
		
		/*
		 * deploy partitioned space
		 */
		ProcessingUnit puP = gsm.deploy(new SpaceDeployment("P").partitioned(1, 1));
		log("wait for " + puP.getName() + " to instantiate");
		puP.waitFor(2);
		
		final CountDownLatch puSAddedLatch = new CountDownLatch(1);
		final CountDownLatch puSRelocateLatch = new CountDownLatch(1);
		puS.getProcessingUnitInstanceAdded().add(new ProcessingUnitInstanceAddedEventListener() {
			
			public void processingUnitInstanceAdded(
					ProcessingUnitInstance processingUnitInstance) {
				if (puSRelocateLatch.getCount() != 0) {
					log("relocating " + puInstanceToString(processingUnitInstance));
					processingUnitInstance.relocate();
					puSRelocateLatch.countDown();
				}else {
					log("instantiated " + puInstanceToString(processingUnitInstance));
					puSAddedLatch.countDown();
				}
			}
		});
		
		final CountDownLatch puPAddedLatch = new CountDownLatch(2);
		final CountDownLatch puPRelocateLatch = new CountDownLatch(2);
		puP.getProcessingUnitInstanceAdded().add(new ProcessingUnitInstanceAddedEventListener() {
			
			public void processingUnitInstanceAdded(
					ProcessingUnitInstance processingUnitInstance) {
				if (puPRelocateLatch.getCount() != 0) {
					log("relocating " + puInstanceToString(processingUnitInstance));
					processingUnitInstance.relocate();
					puPRelocateLatch.countDown();
				} else {
					log("instantiated " + puInstanceToString(processingUnitInstance));
					puPAddedLatch.countDown();
				}
			}
		});
		
		/*
		 * Relocate instances from one GSC to the next
		 */
		log("wait for " + puS.getName() + " to relocate");
		puSRelocateLatch.await();
		
		log("wait for " + puP.getName() + " to relocate");
		puPRelocateLatch.await();
		
		log("wait for " + puS.getName() + " to instantiate after relocation");
		puSAddedLatch.await();
		
		log("wait for " + puP.getName() + " to instantiate after relocation");
		puPAddedLatch.await();
		
		/*
		 * Kill all GSCs
		 */
		for (GridServiceContainer gsc : gscs) {
			log("killing GSC containing " + getProcessingUnitInstanceName(gsc.getProcessingUnitInstances()));
			gsc.kill();
		}
		
		/*
		 * Start GSCs
		 */
		gscs = loadGSCs(machine, 2);
		puS.waitFor(1);
		puP.waitFor(2);
		
		assertEquals(DeploymentStatus.INTACT, waitForDeploymentStatus(puS, DeploymentStatus.INTACT));
		assertEquals(DeploymentStatus.INTACT, waitForDeploymentStatus(puP, DeploymentStatus.INTACT));
		assertEquals("unexpected number of instances", 3, machine.getProcessingUnitInstances().length);
	}
}
