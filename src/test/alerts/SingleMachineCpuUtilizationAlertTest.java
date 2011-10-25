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
import org.openspaces.admin.pu.DeploymentStatus;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.space.SpaceDeployment;
import org.openspaces.core.GigaSpace;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.AbstractTest;
import test.utils.AdminUtils;
import test.utils.ProcessingUnitUtils;

/**
 * This test is exactly like {@link MultiMachineCpuUtilizationAlertTest} but with 1 machine only
 * @author elip
 *
 */
public class SingleMachineCpuUtilizationAlertTest extends AbstractTest { 
	
	Machine machineA;
	ProcessingUnit Pu;
	GridServiceManager gsmA;
	GigaSpace space;
	Alert resolvedAlert;
	
	@BeforeMethod
	public void startSetUp() {
			
		GridServiceAgent gsa = null;
		log("waiting for 1 GSA");
		if ("tgrid".equals(System.getProperty("user.name")))	{
			//don't use pc-lab27 since it has 2 CPUs
			admin.getGridServiceAgents().waitFor(2);
			GridServiceAgent[] agents = admin.getGridServiceAgents().getAgents();
			for (GridServiceAgent agent : agents) {
				if (!"pc-lab27".equals(agent.getMachine().getHostName())) {
					gsa = agent;
					break;
				}
			}
		} else {
			gsa = admin.getGridServiceAgents().waitForAtLeastOne();
		}
		
		machineA = gsa.getMachine();
		
		log("loading GSM");
		gsmA = loadGSM(machineA);
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT , groups = "1")
	public void singleMachineCpuUtilization() throws InterruptedException, ExecutionException {
			
		final List<Alert> hiThresholdAlerts = new ArrayList<Alert>();
		final CountDownLatch hiThresholdAlertLatch = new CountDownLatch(1);
		final CountDownLatch lowThresholdAlertLatch = new CountDownLatch(1);
		
		final AlertManager alertManager = admin.getAlertManager();
		alertManager.setConfig(new CpuUtilizationAlertConfigurer()
			.measurementPeriod(5, TimeUnit.SECONDS)
			.raiseAlertIfCpuAbove(20).resolveAlertIfCpuBelow(15)
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
					resolvedAlert = alert;
					log("Low Threshold :  " + alert);
					lowThresholdAlertLatch.countDown();
				}
			}
			
		});	
		
		/*
		 * this causes the CPU utilization on the machines to go up, causing an alert to rise.
		 * once the deployment is over, the utilization should return to normal, and the alert should be resolved.
		 */
		int i = 1;
		while (hiThresholdAlertLatch.getCount() > 0) {
			log("loading GSC");
			AdminUtils.loadGSC(machineA);
			admin.getGridServiceContainers().waitFor(i);
			log("deploying the processing unit");
			Pu = gsmA.deploy(new SpaceDeployment("Test" + i).numberOfInstances(2)
					.numberOfBackups(1));
			ProcessingUnitUtils.waitForDeploymentStatus(Pu, DeploymentStatus.INTACT);
			i++;
		}
		
		hiThresholdAlertLatch.await(60, TimeUnit.SECONDS);
		lowThresholdAlertLatch.await(60, TimeUnit.SECONDS);
		
		Assert.assertTrue(hiThresholdAlerts.size() > 0);
		Assert.assertNotNull(resolvedAlert);
		
		for (Alert alert : hiThresholdAlerts) {
			Assert.assertEquals(AlertSeverity.WARNING, alert.getSeverity());
			Assert.assertEquals(AlertStatus.RAISED, alert.getStatus());
			Assert.assertTrue(alert instanceof CpuUtilizationAlert);
		}
		
		Assert.assertEquals(AlertSeverity.WARNING, resolvedAlert.getSeverity());
		Assert.assertEquals(AlertStatus.RESOLVED, resolvedAlert.getStatus());
		Assert.assertTrue(resolvedAlert instanceof CpuUtilizationAlert);
	}
	
}
