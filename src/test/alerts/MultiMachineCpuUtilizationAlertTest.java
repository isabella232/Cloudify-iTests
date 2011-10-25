package test.alerts;

import static test.utils.AdminUtils.loadGSM;
import static test.utils.LogUtils.log;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import junit.framework.Assert;
import org.openspaces.admin.alert.Alert;
import org.openspaces.admin.alert.AlertManager;
import org.openspaces.admin.alert.AlertSeverity;
import org.openspaces.admin.alert.AlertStatus;
import org.openspaces.admin.alert.alerts.CpuUtilizationAlert;
import org.openspaces.admin.alert.config.CpuUtilizationAlertConfiguration;
import org.openspaces.admin.alert.config.CpuUtilizationAlertConfigurer;
import org.openspaces.admin.alert.events.AlertTriggeredEventListener;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.space.SpaceDeployment;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import test.AbstractTest;
import test.utils.AdminUtils;

/**
 * test for the cpu utilization alert, this test causes the cpu usage to jump above a certain threshold, after witch
 * the cpu usage should slowly return back to normal level.
 * It ensures all proper alerts have been triggered
 * @author elip
 *
 */
public class MultiMachineCpuUtilizationAlertTest extends AbstractTest {
	
	Machine machineA, machineB, machineC;
	ProcessingUnit Pu;
	GridServiceManager gsmA;
	
	@BeforeMethod
	public void startSetUp() {
			
		log("waiting for 3 GSA's");
		//don't use pc-lab27 since it has 2 CPUs
		admin.getGridServiceAgents().waitFor(4);
		List<GridServiceAgent> gsas = new ArrayList<GridServiceAgent>();
		GridServiceAgent[] agents = admin.getGridServiceAgents().getAgents();
		for (GridServiceAgent agent : agents) {
			if (!"pc-lab27".equals(agent.getMachine().getHostName())) {
				gsas.add(agent);
				break;
			}
		}
		
		log("Found 3 GSAs (excluding pc-lab27): " + gsas);
		
		GridServiceAgent gsaA = gsas.get(0);
		GridServiceAgent gsaB = gsas.get(1);
		GridServiceAgent gsaC = gsas.get(2);
		
		machineA = gsaA.getMachine();
		machineB = gsaB.getMachine();
		machineC = gsaC.getMachine();
		
		log("loading GSM");
		gsmA = loadGSM(machineA);
		
		log("loading 3 GSC");
		AdminUtils.loadGSC(machineA);
		AdminUtils.loadGSC(machineB);
		AdminUtils.loadGSC(machineC);	
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT , groups = "3")
	public void multiMachineCpuUtilization() throws InterruptedException, ExecutionException {
			
		final List<Alert> hiThresholdAlerts = new ArrayList<Alert>();
		final List<Alert> lowThresholdAlerts = new ArrayList<Alert>();

		
		final CountDownLatch hiThresholdAlertLatch = new CountDownLatch(1);
		final CountDownLatch lowThresholdAlertLatch = new CountDownLatch(1);
		
		final AlertManager alertManager = admin.getAlertManager();
		alertManager.setConfig(new CpuUtilizationAlertConfigurer()
			.measurementPeriod(5, TimeUnit.SECONDS)
			.raiseAlertIfCpuAbove(10).resolveAlertIfCpuBelow(5)
			.create());       

		alertManager.enableAlert(CpuUtilizationAlertConfiguration.class);
		
		alertManager.getAlertTriggered().add(new AlertTriggeredEventListener() {
			public void alertTriggered(Alert alert) {
				if (alert.getStatus().equals(AlertStatus.RAISED)) {
					hiThresholdAlerts.add(alert);
					log("Hi Threshold :  " + alert);
					hiThresholdAlertLatch.countDown();
				}
				if (alert.getStatus().equals(AlertStatus.RESOLVED)) {
					lowThresholdAlerts.add(alert);
					log("Low Threshold :  " + alert);
					lowThresholdAlertLatch.countDown();
				}
			}
			
		});	
		
		/*
		 * this causes the CPU utilization on the machines to go up, causing an alert to rise.
		 * once the deployment is over, the utilization should return to normal, and the alert should be resolved.
		 */
		
		log("deploying the processing unit");
		Pu = gsmA.deploy(new SpaceDeployment("Test").numberOfInstances(3)
				.numberOfBackups(1));
		
		hiThresholdAlertLatch.await();
		lowThresholdAlertLatch.await();
		
		Assert.assertTrue(hiThresholdAlerts.size() > 0);
		Assert.assertTrue(lowThresholdAlerts.size() > 0);
		
		int machineAAlertCount = 0;
		int machineBAlertCount = 0;
		int machineCAlertCount = 0;
		
		for (Alert alert : hiThresholdAlerts) {
			CpuUtilizationAlert cpuAlert = (CpuUtilizationAlert) alert;
			if (cpuAlert.getHostName() == machineA.getHostName()) machineAAlertCount = 1;
			if (cpuAlert.getHostName() == machineB.getHostName()) machineBAlertCount = 1;
			if (cpuAlert.getHostName() == machineC.getHostName()) machineCAlertCount = 1;
			Assert.assertEquals(AlertSeverity.WARNING, alert.getSeverity());
			Assert.assertEquals(AlertStatus.RAISED, alert.getStatus());
			Assert.assertFalse(!(alert instanceof CpuUtilizationAlert));
		}
		for (Alert alert : lowThresholdAlerts) {
			Assert.assertEquals(AlertSeverity.WARNING, alert.getSeverity());
			Assert.assertEquals(AlertStatus.RESOLVED, alert.getStatus());
			Assert.assertFalse(!(alert instanceof CpuUtilizationAlert));
		}
		Assert.assertEquals(lowThresholdAlerts.size(), machineAAlertCount + machineBAlertCount + machineCAlertCount);
	}
}
