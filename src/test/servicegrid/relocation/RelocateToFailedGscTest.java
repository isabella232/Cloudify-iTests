package test.servicegrid.relocation;

import static org.testng.AssertJUnit.assertFalse;
import static test.utils.AdminUtils.loadGSC;
import static test.utils.AdminUtils.loadGSCs;
import static test.utils.AdminUtils.loadGSMs;
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
import org.openspaces.admin.pu.events.ProcessingUnitInstanceRemovedEventListener;
import org.openspaces.admin.space.SpaceDeployment;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.AbstractTest;

/**
 * Topology: 1 machine, 2 GSMs, 2 GSCs
 * Tests that if relocated to a GSC that was killed, instance is re-instantiated at origin.
 * 
 * a. Deploy single space
 * c. kill empty GSC
 * b. relocate to killed GSC
 * d. relocation should fail, instance should instantiate at origin
 * e. start GSC
 * f. verify no extra instance is instantiated 
 * 
 * @author Moran Avigdor
 */
public class RelocateToFailedGscTest extends AbstractTest {
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
		ProcessingUnit pu = gsm.deploy(new SpaceDeployment("S"));
		log("wait for " + pu.getName() + " to instantiate");
		pu.waitFor(1);
		
		ProcessingUnitInstance puInstance = machine.getProcessingUnitInstances(pu.getName())[0];
		GridServiceContainer gscWithPu = puInstance.getGridServiceContainer();
		GridServiceContainer gscToRelocateTo = gscWithPu.equals(gscs[0]) ? gscs[1] : gscs[0];
		assertEquals(0, gscToRelocateTo.getProcessingUnitInstances().length);
		
		/*
		 * register for removed event upon relocation
		 */
		final CountDownLatch removedLatch = new CountDownLatch(1);
		gscWithPu.getProcessingUnitInstanceRemoved().add(new ProcessingUnitInstanceRemovedEventListener() {
			
			public void processingUnitInstanceRemoved(
					ProcessingUnitInstance processingUnitInstance) {
				removedLatch.countDown();				
			}
		});
		
		/*
		 * relocate to killed GSC
		 */
		log("killing " + gscToString(gscToRelocateTo) + " containing: " + getProcessingUnitInstanceName(gscToRelocateTo.getProcessingUnitInstances()));
		gscToRelocateTo.kill();
		log("relocating " + getProcessingUnitInstanceName(puInstance)
				+ " from: " + gscToString(gscWithPu)
				+ " to " + gscToString(gscToRelocateTo));
		puInstance.relocate(gscToRelocateTo);
		
		log("waiting for instance to be relocated");
		removedLatch.await();
		
		/*
		 * register for added event upon instantiation
		 */
		final CountDownLatch addedLatch = new CountDownLatch(1);
		gscWithPu.getProcessingUnitInstanceAdded().add(new ProcessingUnitInstanceAddedEventListener() {
			
			public void processingUnitInstanceAdded(
					ProcessingUnitInstance processingUnitInstance) {
				addedLatch.countDown();
			}
		});
		
		log("waiting for instance to be instantiated");
		addedLatch.await();
		pu.waitFor(1);
		assertEquals(DeploymentStatus.INTACT, waitForDeploymentStatus(pu, DeploymentStatus.INTACT));
		
		/*
		 * Start GSC, verify no extra instances
		 */
		log("start new GSC, verify no extra instances");
		GridServiceContainer newGsc = loadGSC(machine);
		final CountDownLatch newAddedLatch = new CountDownLatch(1);
		newGsc.getProcessingUnitInstanceAdded().add(new ProcessingUnitInstanceAddedEventListener() {
			
			public void processingUnitInstanceAdded(
					ProcessingUnitInstance processingUnitInstance) {
				newAddedLatch.countDown();
			}
		});
		assertFalse("extra instance!", newAddedLatch.await(10, TimeUnit.SECONDS));
		assertEquals("extra instance!", 1, machine.getProcessingUnitInstances().length);
	}
}
