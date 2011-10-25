package test.servicegrid.cleanup;

import static test.utils.AdminUtils.loadGSCs;
import static test.utils.LogUtils.log;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.internal.pu.InternalProcessingUnit;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitDeployment;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.openspaces.admin.pu.events.ProcessingUnitInstanceAddedEventListener;
import org.openspaces.admin.pu.events.ProcessingUnitInstanceRemovedEventListener;
import org.testng.Assert;
import org.testng.annotations.Test;

import test.AbstractTest;
import test.utils.AdminUtils;
import test.utils.DeploymentUtils;
import test.utils.ToStringUtils;
import test.utils.AssertUtils.RepetitiveConditionProvider;

/**
 * Tests decrement planned instances API used by the ESM in eager mode when
 * scaling in.
 * 
 * loads 2 GSMs, 2 GSCs and deploys a stateless PU (2 instances max-per-vm 1)
 * try to decrement a planned instance when planned == maintain,
 * this should not succeed.
 * 
 * kill GSC holding one instance.
 * no try to decrement a planned instance when planned < maintain,
 * this should succeed.
 * 
 * Make sure that no other instance is removed or added.
 * 
 * load GSC, make sure decremented instance was not pending to be provisioned by GSM.
 * 
 * kill managing GSM, make sure backup GSM does not try to instantiate decremented instance.
 * 
 * @author moran
 * @since 8.0.4
 */
public class DecrementPlannedInstancesTest extends AbstractTest {

	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1")
	public void test() throws InterruptedException {

		log("waiting for at least one GSA");
		GridServiceAgent gsa = admin.getGridServiceAgents().waitForAtLeastOne();
		Machine machine = gsa.getMachine();

		log("loading 2 GSM, 2 GSCs on " + machine.getHostName());
		AdminUtils.loadGSM(machine.getGridServiceAgent());
		AdminUtils.loadGSM(machine.getGridServiceAgent());
		admin.getGridServiceManagers().waitFor(2);
		assertEquals(2, admin.getGridServiceManagers().getSize());
		
		GridServiceContainer[] gscs = loadGSCs(machine, 2);
		admin.getGridServiceContainers().waitFor(2);
		assertEquals(2, admin.getGridServiceContainers().getSize());

		log("deploy stateless PU - 2 GSCs already running");
		File archive = DeploymentUtils.getArchive("simpleStatelessPu.jar");
		final ProcessingUnit pu = deploy2InstancesOn2Containers(archive);
		log("waiting for " + pu.getTotalNumberOfInstances() + " to be deployed.");
		pu.waitFor(pu.getTotalNumberOfInstances());

		// When decrementing a planned instance, the request will be ignored if
		// planned == maintain
		log("trying an unsuccessful decrement invocation");
		Assert.assertFalse(((InternalProcessingUnit) pu).decrementPlannedInstances(), 
								"planned should not have been decremented");

		/*
		 * Listen for removal events, and expect the instance on the killed GSC to be removed, but no
		 * other instance should be removed when invoking the decrementPlannedInstances() in the "condition" below.
		 */
		final ProcessingUnitInstance instance = gscs[0].getProcessingUnitInstances()[0];
		assertNotNull(instance);
		final CountDownLatch killedLatch = new CountDownLatch(1);
		final AtomicInteger removedCount = new AtomicInteger();
		ProcessingUnitInstanceRemovedEventListener removedEventListener = new ProcessingUnitInstanceRemovedEventListener() {
			public void processingUnitInstanceRemoved(
					ProcessingUnitInstance processingUnitInstance) {
				log("received event of pu instance removal: " + ToStringUtils.puInstanceToString(processingUnitInstance));
				if (processingUnitInstance.equals(instance)) {
					killedLatch.countDown();
				} else {
					removedCount.incrementAndGet();
				}
			}
		};
		admin.getProcessingUnits().getProcessingUnitInstanceRemoved().add(removedEventListener);

		final AtomicInteger addedCount = new AtomicInteger();
		ProcessingUnitInstanceAddedEventListener addedEventListener = new ProcessingUnitInstanceAddedEventListener() {
			public void processingUnitInstanceAdded(
					ProcessingUnitInstance processingUnitInstance) {
				log("received event of pu instance added: " + ToStringUtils.puInstanceToString(processingUnitInstance));
				addedCount.incrementAndGet();
			}
		};
		admin.getProcessingUnits().getProcessingUnitInstanceAdded().add(addedEventListener, false /*existing*/);
		
		// Kill one GSC, processing unit instance should be removed and added to
		// pending queue.
		log("killing GSC " + ToStringUtils.gscToString(gscs[0]) + " with pu " + ToStringUtils.puInstanceToString(instance));
		gscs[0].kill();


		try {
			log("waiting for processing unit instance removal event");
			killedLatch.await();
			
			//planned instance should decrement by one
			repetitiveAssertTrue(
					"Expected planned instances to be decremented", new RepetitiveConditionProvider() {
						public boolean getCondition() {
							//decrement the instance, and return true if instance was decremented.
							boolean decremented = ((InternalProcessingUnit) pu).decrementPlannedInstances();
							return decremented;
						}
					}, OPERATION_TIMEOUT);
			
			//pu instance count should be one
			repetitiveAssertTrue("Expected 1 pu instance", new RepetitiveConditionProvider() {
				public boolean getCondition() {
					return pu.getTotalNumberOfInstances() == 1;
				}
			}, OPERATION_TIMEOUT);
			
			//no other pu instance (except the one that was killed with the GSC) should be removed.
			assertEquals(0, removedCount.get());
			//no pu instance should be added.
			assertEquals(0, addedCount.get());
			
			/*
			 * load a GSC and make sure that an instance isn't being provisioned on it.
			 * Check also that when backup GSM becomes 'active', it does not provision the instance on it. 
			 */
			loadGSCs(machine, 1);
			admin.getGridServiceContainers().waitFor(2);
			assertEquals(2, admin.getGridServiceContainers().getSize());
			
			//no pu instance should be added.
			Thread.sleep(30000);
			assertEquals(0, addedCount.get());
			
			//kill managing GSM
			final GridServiceManager backupGridServiceManager = pu.getBackupGridServiceManagers()[0];
			assertNotNull(backupGridServiceManager);
			
			GridServiceManager managingGridServiceManager = pu.getManagingGridServiceManager();
			log("killing managing GSM: " + ToStringUtils.gsmToString(managingGridServiceManager));
			managingGridServiceManager.kill();
			
			repetitiveAssertTrue("Expected backup GSM to become active", new RepetitiveConditionProvider() {
				public boolean getCondition() {
					return backupGridServiceManager.equals(pu.getManagingGridServiceManager());
				}
			}, OPERATION_TIMEOUT);
			
			//no pu instance should be added.
			Thread.sleep(30000);
			assertEquals(0, addedCount.get());
			
		} finally {
			//cleanup
			admin.getProcessingUnits().getProcessingUnitInstanceRemoved().remove(removedEventListener);
			admin.getProcessingUnits().getProcessingUnitInstanceAdded().remove(addedEventListener);
		}
	}

	private ProcessingUnit deploy2InstancesOn2Containers(File archive) {
		final ProcessingUnit pu = admin.getGridServiceManagers().deploy(
				new ProcessingUnitDeployment(archive)
					.numberOfInstances(2)
					.maxInstancesPerVM(1)
					.maxInstancesPerMachine(0));

		pu.waitFor(2);
		return pu;
	}

}
