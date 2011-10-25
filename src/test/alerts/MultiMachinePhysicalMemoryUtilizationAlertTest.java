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
 * test for physical memory alert. this test loads gsc's onto a machine until the physical memory reaches a certain 
 * threshold, at witch point an alert should be triggered. then we kill all gsc's, witch should cause a resolved alert
 * to be triggered
 * @author elip
 *
 */
public class MultiMachinePhysicalMemoryUtilizationAlertTest extends AbstractTest {
	
	Machine machineA, machineB, machineC;
	ProcessingUnit Pu;
	GridServiceManager gsmA;
	
	@BeforeMethod
	public void startSetUp() {
		
		log("waiting for 3 GSA");
		admin.getGridServiceAgents().waitFor(3);
		
		GridServiceAgent[] agents = admin.getGridServiceAgents().getAgents();
		GridServiceAgent gsaA = agents[0];
		GridServiceAgent gsaB = agents[1];
		GridServiceAgent gsaC = agents[2];
		
		machineA = gsaA.getMachine();
		machineB = gsaB.getMachine();
		machineC = gsaC.getMachine();
		
		log("loading GSM");
		gsmA = loadGSM(machineA);
		admin.getGridServiceManagers().waitFor(1);
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT , groups = "3")
	public void multiMachinePhysicalMemoryUtilization() throws InterruptedException {
		
		final List<Alert> hiThresholdAlerts = new ArrayList<Alert>();
		final List<Alert> lowThresholdAlerts = new ArrayList<Alert>();
		final List<String> hostsNames = new ArrayList<String>();
		
		final CountDownLatch hiThresholdAlertLatch = new CountDownLatch(3);
		final CountDownLatch lowThresholdAlertLatch = new CountDownLatch(3);
		
		
		double maxPhysicalMemoryUsage = Math.max(Math.max(machineA.
				getOperatingSystem().getStatistics().getPhysicalMemoryUsedPerc(), machineB.
				getOperatingSystem().getStatistics().getPhysicalMemoryUsedPerc()), machineC.
				getOperatingSystem().getStatistics().getPhysicalMemoryUsedPerc());
		
		final AlertManager alertManager = admin.getAlertManager();
		alertManager.setConfig(new PhysicalMemoryUtilizationAlertConfigurer()
			.measurementPeriod(5, TimeUnit.SECONDS)
			.raiseAlertIfMemoryAbove((int) (maxPhysicalMemoryUsage + 5)).resolveAlertIfMemoryBelow((int) maxPhysicalMemoryUsage + 5)
			.create());       

		alertManager.enableAlert(PhysicalMemoryUtilizationAlertConfiguration.class);
		
		alertManager.getAlertTriggered().add(new AlertTriggeredEventListener() {
			public void alertTriggered(Alert alert) {
				if (alert.getStatus().equals(AlertStatus.RAISED)) {
					log("Hi Threshold : " + alert);
					PhysicalMemoryUtilizationAlert cpAlert = (PhysicalMemoryUtilizationAlert) alert;
					if (!hostsNames.contains(cpAlert.getHostName())) {
						hiThresholdAlerts.add(alert);
						hostsNames.add(cpAlert.getHostName());
						hiThresholdAlertLatch.countDown();
					}
					
				}
				else {
					if (alert.getStatus().equals(AlertStatus.RESOLVED)) {
						log("Low Threshold :  " + alert);
						lowThresholdAlerts.add(alert);
						lowThresholdAlertLatch.countDown();
					}	
				}	
			}
		});	
		
		assertTrue(hiThresholdAlerts.size() == 0);
		log("loading gsc's to trigger alert...");
		int i = 1;
		while (hiThresholdAlertLatch.getCount() > 0) {
			if (!hostsNames.contains(machineA.getHostName())) AdminUtils.loadGSC(machineA);
			if (!hostsNames.contains(machineB.getHostName())) AdminUtils.loadGSC(machineB);
			if (!hostsNames.contains(machineC.getHostName())) AdminUtils.loadGSC(machineC);
			i++;
			Thread.sleep(5000);
		}
		
		hiThresholdAlertLatch.await(60, TimeUnit.SECONDS);
		
		log("Terminaitng gsc's to resolve alert...");
		GridServiceContainer [] allGSC = admin.getGridServiceContainers().getContainers();
		for (int j = 0 ; j < allGSC.length ; j ++ ) {
			allGSC[j].kill();
		}
		
		admin.getGridServiceContainers().waitFor(0);
		
		log("waiting for alert to be resolved...");
		lowThresholdAlertLatch.await(60, TimeUnit.SECONDS);	
		
		Assert.assertTrue(hiThresholdAlerts.size() > 0);
		Assert.assertTrue(lowThresholdAlerts.size() > 0);
		int machineAAlertCount = 0;
		int machineBAlertCount = 0;
		int machineCAlertCount = 0;
		
		for (Alert alert : hiThresholdAlerts) {
			PhysicalMemoryUtilizationAlert cpuAlert = (PhysicalMemoryUtilizationAlert) alert;
			if (cpuAlert.getHostName() == machineA.getHostName()) machineAAlertCount = 1;
			if (cpuAlert.getHostName() == machineB.getHostName()) machineBAlertCount = 1;
			if (cpuAlert.getHostName() == machineC.getHostName()) machineCAlertCount = 1;
			Assert.assertEquals(AlertSeverity.WARNING, alert.getSeverity());
			Assert.assertEquals(AlertStatus.RAISED, alert.getStatus());
			Assert.assertTrue(alert instanceof PhysicalMemoryUtilizationAlert);
		}
		for (Alert alert : lowThresholdAlerts) {
			Assert.assertEquals(AlertSeverity.WARNING, alert.getSeverity());
			Assert.assertEquals(AlertStatus.RESOLVED, alert.getStatus());
			Assert.assertTrue(alert instanceof PhysicalMemoryUtilizationAlert);
		}
		Assert.assertEquals(lowThresholdAlerts.size(), machineAAlertCount + machineBAlertCount + machineCAlertCount);

	}

}
