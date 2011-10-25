package test.alerts.component;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import org.openspaces.admin.alert.Alert;
import org.openspaces.admin.alert.AlertFactory;
import org.openspaces.admin.alert.AlertSeverity;
import org.openspaces.admin.alert.AlertStatus;
import org.openspaces.admin.alert.events.AlertTriggeredEventListener;
import org.testng.annotations.Test;

import test.AbstractTest;
import test.utils.LogUtils;

/**
 * GS-8816
 * Register for alerts with one listener, and assert that second listener receives the exact amount of events as the first.
 * 
 * @author Moran Avigdor
 * @since 8.0.2
 */
public class AlertTriggeredEventListenerComponentTest extends AbstractTest {
	
	private final int nALERTS = 10;
	private final int nTOTAL = nALERTS * 3;

	@Test(timeOut = DEFAULT_TEST_TIMEOUT , groups = "0")
	public void test() throws InterruptedException, ExecutionException {
		
		final AtomicInteger totalCount = new AtomicInteger();
		final CountDownLatch bothLatchCount = new CountDownLatch(nTOTAL*2);
		
		final CountDownLatch latch1 = new CountDownLatch(nTOTAL);
		admin.getAlertManager().getAlertTriggered().add(new AlertTriggeredEventListener() {
			public void alertTriggered(Alert alert) {
				int count = totalCount.incrementAndGet();
				latch1.countDown();
				bothLatchCount.countDown();
				LogUtils.log("#"+count+" listener1: " + alert);
			}
		});
		
		triggerAlerts();
		latch1.await();

		final CountDownLatch latch2 = new CountDownLatch(nTOTAL);
		admin.getAlertManager().getAlertTriggered().add(new AlertTriggeredEventListener() {
			public void alertTriggered(Alert alert) {
				int count = totalCount.incrementAndGet();
				latch2.countDown();
				bothLatchCount.countDown();
				LogUtils.log("#"+count+" listener2: " + alert);
			}
		});
		
		latch2.await();
		bothLatchCount.await();
		
		assertEquals(nTOTAL*2, totalCount.get());
	}

	private void triggerAlerts() {
		
		//raise n alerts
		for (int i=0; i<nALERTS; ++i) {
			Alert alert = new AlertFactory().status(AlertStatus.RAISED)
			.severity(AlertSeverity.SEVERE).groupUid("group_" + i)
			.description("alert_"+i)
			.create();
			admin.getAlertManager().triggerAlert(alert);

		}
		
		//in each group, raise another alert
		for (int i=0; i<nALERTS; ++i) {
			Alert alert = new AlertFactory().status(AlertStatus.RAISED)
			.severity(AlertSeverity.SEVERE).groupUid("group_" + i)
			.description("alert_"+i+"_"+i)
			.create();
			admin.getAlertManager().triggerAlert(alert);
		}
		
		//resolve alerts in group
		for (int i=0; i<nALERTS; ++i) {
			Alert alert = new AlertFactory().status(AlertStatus.RESOLVED)
					.severity(AlertSeverity.SEVERE).groupUid("group_"+i)
					.description("alert_"+i+"_"+i)
					.create();
			admin.getAlertManager().triggerAlert(alert);
		}
	}
}
