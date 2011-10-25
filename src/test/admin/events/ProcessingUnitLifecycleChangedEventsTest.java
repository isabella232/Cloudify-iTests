package test.admin.events;

import static test.utils.AdminUtils.loadGSC;
import static test.utils.AdminUtils.loadGSM;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.DeploymentStatus;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitDeployment;
import org.openspaces.admin.pu.events.BackupGridServiceManagerChangedEvent;
import org.openspaces.admin.pu.events.BackupGridServiceManagerChangedEventListener;
import org.openspaces.admin.pu.events.ManagingGridServiceManagerChangedEvent;
import org.openspaces.admin.pu.events.ManagingGridServiceManagerChangedEventListener;
import org.openspaces.admin.pu.events.ProcessingUnitLifecycleEventListener;
import org.openspaces.admin.pu.events.ProcessingUnitStatusChangedEvent;
import org.openspaces.admin.pu.events.ProcessingUnitStatusChangedEventListener;
import org.openspaces.admin.pu.events.BackupGridServiceManagerChangedEvent.Type;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.AbstractTest;
import test.utils.DeploymentUtils;
import test.utils.LogUtils;
import test.utils.ToStringUtils;
import test.utils.AssertUtils.RepetitiveConditionProvider;

/**
 * Test register for processing unit life cycle events.
 * 
 * @author Moran Avigdor
 * @since 8.0.4
 */
public class ProcessingUnitLifecycleChangedEventsTest extends AbstractTest {

	@BeforeMethod
	public void setup() {
		GridServiceAgent gsa = admin.getGridServiceAgents().waitForAtLeastOne();
		Machine machine = gsa.getMachine();
		loadGSM(machine);
		loadGSM(machine);
		
		loadGSC(machine);
		loadGSC(machine);
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT , groups="1")
	public void test() throws Exception {
		
		File archive = DeploymentUtils.getArchive("simpleStatelessPu.jar");
		final ProcessingUnit pu = admin.getGridServiceManagers().deploy(
				new ProcessingUnitDeployment(archive)
					.numberOfInstances(2)
					.maxInstancesPerVM(1)
					.maxInstancesPerMachine(0));
		pu.waitFor(2);

		//allow admin background thread responsible of changed events to run 
		Thread.sleep(10000);
		LogUtils.log("adding processing units lifecycle listener#1");
		ProcessingUnitsListenerHelper listener1 = new ProcessingUnitsListenerHelper(pu);
		admin.getProcessingUnits().addLifecycleListener(listener1.getEventListener());
		try {
			listener1.runAsserts();
		} finally {
			//cleanup
			admin.getProcessingUnits().removeLifecycleListener(listener1.getEventListener());
		}
		
		//adding a second listener should receive the same events as the first listener
		Thread.sleep(10000);
		LogUtils.log("adding processing units lifecycle listener#2");
		ProcessingUnitsListenerHelper listener2 = new ProcessingUnitsListenerHelper(pu);
		admin.getProcessingUnits().addLifecycleListener(listener2.getEventListener());
		try {
			listener2.runAsserts();
		} finally {
			//cleanup
			admin.getProcessingUnits().removeLifecycleListener(listener2.getEventListener());
		}
		
		ProcessingUnitEventChangedListenersHelper listener3 = new ProcessingUnitEventChangedListenersHelper(pu);
		try {
			listener3.runAsserts();
		}finally {
			//cleanup
			listener3.cleaup();
		}
	}
	
	private static class ProcessingUnitsListenerHelper {
		
		final AtomicInteger processingUnitBackupGridServiceManagerChanged = new AtomicInteger();
		final AtomicInteger processingUnitManagingGridServiceManagerChanged = new AtomicInteger();
		final AtomicInteger processingUnitStatusChanged = new AtomicInteger();
		final AtomicInteger processingUnitRemoved = new AtomicInteger();
		final AtomicInteger processingUnitAdded = new AtomicInteger();

		private final ProcessingUnitLifecycleEventListener eventListener;
		
		public ProcessingUnitsListenerHelper(final ProcessingUnit pu) {
			
			eventListener = new ProcessingUnitLifecycleEventListener() {
				public void processingUnitBackupGridServiceManagerChanged(BackupGridServiceManagerChangedEvent event) {
					LogUtils.log("processingUnitBackupGridServiceManagerChanged event");
					assertEquals(pu, event.getProcessingUnit());
					assertEquals(Type.ADDED, event.getType());
					assertEquals(pu.getBackupGridServiceManagers()[0], event.getGridServiceManager());
					processingUnitBackupGridServiceManagerChanged.incrementAndGet();
				}
				public void processingUnitManagingGridServiceManagerChanged(ManagingGridServiceManagerChangedEvent event) {
					LogUtils.log("processingUnitManagingGridServiceManagerChanged event");
					assertEquals(pu, event.getProcessingUnit());
					Assert.assertNull(event.getPreviousGridServiceManager(), "expected null, but was: " + ToStringUtils.gsmToString(event.getPreviousGridServiceManager()));
					assertEquals(pu.getManagingGridServiceManager(), event.getNewGridServiceManager());
					processingUnitManagingGridServiceManagerChanged.incrementAndGet();
				}
				public void processingUnitStatusChanged(ProcessingUnitStatusChangedEvent event) {
					LogUtils.log("processingUnitStatusChanged event");
					assertEquals(pu, event.getProcessingUnit());
					assertEquals(DeploymentStatus.INTACT, event.getNewStatus());
					assertEquals(pu.getStatus(), event.getNewStatus());
					Assert.assertNull(event.getPreviousStatus());
					processingUnitStatusChanged.incrementAndGet();
				}
				public void processingUnitRemoved(ProcessingUnit processingUnit) {
					LogUtils.log("processingUnitRemoved event");
					processingUnitRemoved.incrementAndGet();
				}
				public void processingUnitAdded(ProcessingUnit processingUnit) {
					LogUtils.log("processingUnitAdded event");
					assertEquals(pu, processingUnit);
					processingUnitAdded.incrementAndGet();
				}
			};
		}
		
		public ProcessingUnitLifecycleEventListener getEventListener() {
			return eventListener;
		}
		
		public void runAsserts() {
			
			repetitiveAssertTrue("expected backup gsm event", new RepetitiveConditionProvider() {
				public boolean getCondition() {
					LogUtils.log("condition: 1 == processingUnitBackupGridServiceManagerChanged == " + processingUnitBackupGridServiceManagerChanged);
					return 1 == processingUnitBackupGridServiceManagerChanged.get();
				}
			}, OPERATION_TIMEOUT);

			repetitiveAssertTrue("expected managing gsm event", new RepetitiveConditionProvider() {
				public boolean getCondition() {
					LogUtils.log("condition 1 == processingUnitManagingGridServiceManagerChanged == " + processingUnitBackupGridServiceManagerChanged);
					return 1 == processingUnitManagingGridServiceManagerChanged.get();
				}
			}, OPERATION_TIMEOUT);

			repetitiveAssertTrue("expected intact status event", new RepetitiveConditionProvider() {
				public boolean getCondition() {
					LogUtils.log("condition 1 == processingUnitStatusChanged == " + processingUnitBackupGridServiceManagerChanged);
					return 1 == processingUnitStatusChanged.get();
				}
			}, OPERATION_TIMEOUT);

			assertEquals("unexpected instance removal", 0, processingUnitRemoved.get());

			repetitiveAssertTrue("expected 1 instance added events", new RepetitiveConditionProvider() {
				public boolean getCondition() {
					LogUtils.log("condition 1 == processingUnitAdded == " + processingUnitBackupGridServiceManagerChanged);
					return 1 == processingUnitAdded.get();
				}
			}, OPERATION_TIMEOUT);
			
		}
	}

	private static class ProcessingUnitEventChangedListenersHelper {
		
		final AtomicInteger processingUnitBackupGridServiceManagerChanged = new AtomicInteger();
		final AtomicInteger processingUnitManagingGridServiceManagerChanged = new AtomicInteger();
		final AtomicInteger processingUnitStatusChanged = new AtomicInteger();
		private final BackupGridServiceManagerChangedEventListener backupGridServiceManagerChangedEventListener;
		private final ManagingGridServiceManagerChangedEventListener managingGridServiceManagerChangedEventListener;
		private final ProcessingUnitStatusChangedEventListener processingUnitStatusChangedEventListener;
		private final ProcessingUnit pu;

		public ProcessingUnitEventChangedListenersHelper(final ProcessingUnit pu) {

			this.pu = pu;
			backupGridServiceManagerChangedEventListener = new BackupGridServiceManagerChangedEventListener() {
				public void processingUnitBackupGridServiceManagerChanged(
						BackupGridServiceManagerChangedEvent event) {
					LogUtils.log("processingUnitBackupGridServiceManagerChanged event");
					assertEquals(pu, event.getProcessingUnit());
					assertEquals(Type.ADDED, event.getType());
					assertEquals(pu.getBackupGridServiceManagers()[0], event.getGridServiceManager());
					processingUnitBackupGridServiceManagerChanged.incrementAndGet();
				}
			};
			pu.getBackupGridServiceManagerChanged().add(backupGridServiceManagerChangedEventListener);

			managingGridServiceManagerChangedEventListener = new ManagingGridServiceManagerChangedEventListener() {
				public void processingUnitManagingGridServiceManagerChanged(
						ManagingGridServiceManagerChangedEvent event) {
					LogUtils.log("processingUnitManagingGridServiceManagerChanged event");
					assertEquals(pu, event.getProcessingUnit());
					Assert.assertNull(event.getPreviousGridServiceManager(), "expected null, but was: " + ToStringUtils.gsmToString(event.getPreviousGridServiceManager()));
					assertEquals(pu.getManagingGridServiceManager(), event.getNewGridServiceManager());
					processingUnitManagingGridServiceManagerChanged.incrementAndGet();
				}
			};
			pu.getManagingGridServiceManagerChanged().add(managingGridServiceManagerChangedEventListener);

			processingUnitStatusChangedEventListener = new ProcessingUnitStatusChangedEventListener() {
				public void processingUnitStatusChanged(
						ProcessingUnitStatusChangedEvent event) {
					LogUtils.log("processingUnitStatusChanged event");
					assertEquals(pu, event.getProcessingUnit());
					assertEquals(DeploymentStatus.INTACT, event.getNewStatus());
					assertEquals(pu.getStatus(), event.getNewStatus());
					Assert.assertNull(event.getPreviousStatus());
					processingUnitStatusChanged.incrementAndGet();
				}
			};
			pu.getProcessingUnitStatusChanged().add(processingUnitStatusChangedEventListener);
		}
		
		public void runAsserts() {
			
			repetitiveAssertTrue("expected backup gsm event", new RepetitiveConditionProvider() {
				public boolean getCondition() {
					LogUtils.log("condition: 1 == processingUnitBackupGridServiceManagerChanged == " + processingUnitBackupGridServiceManagerChanged);
					return 1 == processingUnitBackupGridServiceManagerChanged.get();
				}
			}, OPERATION_TIMEOUT);

			repetitiveAssertTrue("expected managing gsm event", new RepetitiveConditionProvider() {
				public boolean getCondition() {
					LogUtils.log("condition 1 == processingUnitManagingGridServiceManagerChanged == " + processingUnitBackupGridServiceManagerChanged);
					return 1 == processingUnitManagingGridServiceManagerChanged.get();
				}
			}, OPERATION_TIMEOUT);

			repetitiveAssertTrue("expected intact status event", new RepetitiveConditionProvider() {
				public boolean getCondition() {
					LogUtils.log("condition 1 == processingUnitStatusChanged == " + processingUnitBackupGridServiceManagerChanged);
					return 1 == processingUnitStatusChanged.get();
				}
			}, OPERATION_TIMEOUT);
		}
		
		public void cleaup() {
			pu.getManagingGridServiceManagerChanged().remove(managingGridServiceManagerChangedEventListener);
			pu.getBackupGridServiceManagerChanged().remove(backupGridServiceManagerChangedEventListener);
			pu.getProcessingUnitStatusChanged().remove(processingUnitStatusChangedEventListener);
		}
	}

}
