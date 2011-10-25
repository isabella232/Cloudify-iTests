package test.alerts;

import static test.utils.AdminUtils.loadGSM;
import static test.utils.LogUtils.log;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import junit.framework.Assert;
import org.openspaces.admin.alert.Alert;
import org.openspaces.admin.alert.AlertManager;
import org.openspaces.admin.alert.AlertSeverity;
import org.openspaces.admin.alert.AlertStatus;
import org.openspaces.admin.alert.alerts.PhysicalMemoryUtilizationAlert;
import org.openspaces.admin.alert.config.PhysicalMemoryUtilizationAlertConfiguration;
import org.openspaces.admin.alert.config.PhysicalMemoryUtilizationAlertConfigurer;
import org.openspaces.admin.alert.events.AlertTriggeredEventListener;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.ProcessingUnit;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import test.AbstractTest;
import test.utils.AdminUtils;

/**
 * this test is exactly like {@link MultiMachinePhysicalMemoryUtilizationAlertTest}, but with 1 machine
 * @author elip
 *
 */
public class SingleMachinePhysicalMemoryUtilizationAlertTest extends AbstractTest {
	
	Machine machineA;
	ProcessingUnit Pu;
	GridServiceManager gsmA;
	Alert resolvedAlert;
	
	@BeforeMethod
	public void startSetUp() {
		
		log("waiting for 1 GSA");
		admin.getGridServiceAgents().waitFor(1);
		
		GridServiceAgent[] agents = admin.getGridServiceAgents().getAgents();
		GridServiceAgent gsaA = agents[0];
		
		machineA = gsaA.getMachine();
		
		log("loading GSM");
		gsmA = loadGSM(machineA); 
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT , groups = "1")
	public void singleMachinePhysicalMemoryUtilization() throws InterruptedException {
		
		final List<Alert> hiThresholdAlerts = new ArrayList<Alert>();
		
		final CountDownLatch hiThresholdAlertLatch = new CountDownLatch(1);
		final CountDownLatch lowThresholdAlertLatch = new CountDownLatch(1);
		
		final AlertManager alertManager = admin.getAlertManager();
		alertManager.setConfig(new PhysicalMemoryUtilizationAlertConfigurer()
			.measurementPeriod(5, TimeUnit.SECONDS)
			.raiseAlertIfMemoryAbove(80).resolveAlertIfMemoryBelow(80)
			.create());       

		alertManager.enableAlert(PhysicalMemoryUtilizationAlertConfiguration.class);
		
		alertManager.getAlertTriggered().add(new AlertTriggeredEventListener() {
			public void alertTriggered(Alert alert) {
				if (alert.getStatus().equals(AlertStatus.RAISED)) {
					hiThresholdAlerts.add(alert);
					log("Hi Threshold : " + alert);
					hiThresholdAlertLatch.countDown();
				}
				else {
					if (alert.getStatus().equals(AlertStatus.RESOLVED)) {
						resolvedAlert = alert;
						log("Low Threshold :  " + alert);
						lowThresholdAlertLatch.countDown();
					}	
				}	
			}
		});	
		
		assertTrue(hiThresholdAlerts.size() == 0);
		log("loading gsc's to trigger an alert...");
		int i = 1;
		while (hiThresholdAlertLatch.getCount() > 0) {
			AdminUtils.loadGSC(machineA);
			admin.getGridServiceContainers().waitFor(i++);
			Thread.sleep(5000);
		}
		
		log("terminating gsc's to resolve alert...");
		GridServiceContainer [] allGSC = admin.getGridServiceContainers().getContainers();
		for (int j = 0 ; j < allGSC.length ; j ++ ) {
			allGSC[j].kill();		
		}
		admin.getGridServiceContainers().waitFor(0);
	
		hiThresholdAlertLatch.await(60, TimeUnit.SECONDS);
		lowThresholdAlertLatch.await(60, TimeUnit.SECONDS);	
		
		Assert.assertTrue(hiThresholdAlerts.size() > 0);
		Assert.assertNotNull(resolvedAlert);
		
		for (Alert alert : hiThresholdAlerts) {
			Assert.assertEquals(AlertSeverity.WARNING, alert.getSeverity());
			Assert.assertEquals(AlertStatus.RAISED, alert.getStatus());
			Assert.assertTrue(alert instanceof PhysicalMemoryUtilizationAlert);
		}
		
		Assert.assertEquals(AlertSeverity.WARNING, resolvedAlert.getSeverity());
		Assert.assertEquals(AlertStatus.RESOLVED, resolvedAlert.getStatus());
		Assert.assertTrue(resolvedAlert instanceof PhysicalMemoryUtilizationAlert);
	}
}
