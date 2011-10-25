package test.webui.dashboard.alerts;

import static framework.utils.AdminUtils.loadGSCs;
import static framework.utils.AdminUtils.loadGSM;
import static framework.utils.LogUtils.log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.openspaces.admin.alert.Alert;
import org.openspaces.admin.alert.AlertManager;
import org.openspaces.admin.alert.AlertStatus;
import org.openspaces.admin.alert.config.ReplicationChannelDisconnectedAlertConfiguration;
import org.openspaces.admin.alert.config.ReplicationChannelDisconnectedAlertConfigurer;
import org.openspaces.admin.alert.events.AlertTriggeredEventListener;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.DeploymentStatus;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.space.SpaceDeployment;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import framework.utils.LogUtils;
import framework.utils.ProcessingUnitUtils;

import test.webui.AbstractSeleniumTest;
import test.webui.objects.dashboard.AlertsGrid;
import test.webui.objects.dashboard.DashboardTab;
import test.webui.objects.dashboard.AlertsGrid.WebUIAlert;
import test.webui.objects.LoginPage;

public class ReplicationAlertsTest extends AbstractSeleniumTest {
	
	Machine machineA;
	ProcessingUnit pu;
	GridServiceManager gsmA;
	
	@BeforeMethod(alwaysRun = true)
	public void startSetup() {
		log("waiting for 1 machine");
		admin.getMachines().waitFor(1);

		log("waiting for 1 GSA");
		admin.getGridServiceAgents().waitFor(1);

		GridServiceAgent[] agents = admin.getGridServiceAgents().getAgents();
		GridServiceAgent gsaA = agents[0];

		machineA = gsaA.getMachine();

		log("starting: 1 GSM and 2 GSC's on 1 machine");
		gsmA = loadGSM(machineA); 
		loadGSCs(machineA, 2);
	}
	
	 @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = {"cloudify" , "xap"})
	public void replicationDisconnectedAlert() throws InterruptedException {
		
		final List<Alert> adminAlerts = new ArrayList<Alert>();
		
		final CountDownLatch replicationRestored = new CountDownLatch(2);
		
		// lets register to admin alerts
		final AlertManager alertManager = admin.getAlertManager();
		alertManager.setConfig(new ReplicationChannelDisconnectedAlertConfigurer().create());

		alertManager.enableAlert(ReplicationChannelDisconnectedAlertConfiguration.class);
		
		alertManager.getAlertTriggered().add(new AlertTriggeredEventListener() {
			public void alertTriggered(Alert alert) {
				if (alert.getStatus().equals(AlertStatus.RESOLVED)) replicationRestored.countDown();
				adminAlerts.add(alert);
				System.out.println(alert);
			}
		});
		
		// get new login page
		LoginPage loginPage = getLoginPage();
		
		// get new topology tab
		DashboardTab dashboardTab = loginPage.login().switchToDashboard();
		
		LogUtils.log("deplying (1,1) space cluster...");
		// this should trigger 2 alerts
		SpaceDeployment deployment = new SpaceDeployment("Test").partitioned(1, 1).maxInstancesPerVM(1);
		pu = gsmA.deploy(deployment);
		ProcessingUnitUtils.waitForDeploymentStatus(pu, DeploymentStatus.INTACT);
		
		LogUtils.log("disconnecting replication channel...");
		admin.getGridServiceContainers().getContainers()[0].kill();
		admin.getGridServiceContainers().waitFor(1);
		admin.getProcessingUnits().getProcessingUnit("Test").waitFor(1);
		
		LogUtils.log("restoring replication channel...");
		loadGSCs(machineA, 1);
		
		admin.getGridServiceContainers().waitFor(2);
		admin.getProcessingUnits().getProcessingUnit("Test").waitFor(2);
		
		ProcessingUnitUtils.waitForDeploymentStatus(pu, DeploymentStatus.INTACT);
		replicationRestored.await(10, TimeUnit.SECONDS);
		
		takeScreenShot(this.getClass(), "replicationDisconnectedAlert");
		
		LogUtils.log("retrieving alerts from webui...");
		AlertsGrid alertGrid = dashboardTab.getAlertsGrid();
		List<WebUIAlert> replicationAlerts = alertGrid.getAlertsByType(AlertsGrid.REPLICATION);
		List<WebUIAlert> otherAlerts = alertGrid.getAlertsAppartFrom(AlertsGrid.REPLICATION);
		
		alertGrid.assertAlertsConsistency(replicationAlerts, adminAlerts);
		LogUtils.log("finished");
	}
	
	@AfterMethod(alwaysRun = true)
	public void undeployPu() {
		pu.undeploy();
		ProcessingUnitUtils.waitForDeploymentStatus(pu, DeploymentStatus.UNDEPLOYED);
	}
}
