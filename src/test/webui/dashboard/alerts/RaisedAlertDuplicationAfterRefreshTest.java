package test.webui.dashboard.alerts;

import static test.utils.AdminUtils.loadGSCs;
import static test.utils.AdminUtils.loadGSM;
import static test.utils.LogUtils.log;

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

import test.utils.AssertUtils;
import test.utils.AssertUtils.RepetitiveConditionProvider;
import test.utils.LogUtils;
import test.utils.ProcessingUnitUtils;
import test.utils.WebUiUtils;
import test.webui.AbstractSeleniumTest;
import test.webui.objects.LoginPage;
import test.webui.objects.dashboard.AlertsGrid;
import test.webui.objects.dashboard.AlertsGrid.WebUIAlert;
import test.webui.objects.dashboard.DashboardTab;
import framework.tools.SGTestHelper;

public class RaisedAlertDuplicationAfterRefreshTest extends AbstractSeleniumTest {

	Machine machineA;
	ProcessingUnit pu;
	GridServiceManager gsmA;
	
	@Override
	@BeforeMethod(alwaysRun = true)
	public void beforeTest() {
		
		String filepath = SGTestHelper.getSGTestRootDir() + "/src/test/webui/resources/alerts/replicationAlert.xml";
		LogUtils.log("preparing appropriate alert.xml...");
		WebUiUtils.useAlertXmlConfigurationFile(filepath);	
		super.beforeTest();
		
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
		
		LogUtils.log("deplying (1,1) space cluster...");
		SpaceDeployment deployment = new SpaceDeployment("Test").partitioned(1, 1).maxInstancesPerVM(1);
		pu = gsmA.deploy(deployment);
		ProcessingUnitUtils.waitForDeploymentStatus(pu, DeploymentStatus.INTACT);
	}
	
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = {"cloudify" , "xap"})
	public void duplicateAlerts() throws InterruptedException {

		final CountDownLatch raisedReplicationAlertLatch = new CountDownLatch(1);


		// lets register to admin alerts
		final AlertManager alertManager = admin.getAlertManager();
		alertManager.setConfig(new ReplicationChannelDisconnectedAlertConfigurer().create());

		alertManager.enableAlert(ReplicationChannelDisconnectedAlertConfiguration.class);	

		alertManager.getAlertTriggered().add(new AlertTriggeredEventListener() {
			public void alertTriggered(Alert alert) {
				if (alert.getStatus().equals(AlertStatus.RAISED)) raisedReplicationAlertLatch.countDown();
			}
		});


		// get new login page
		LoginPage loginPage = getLoginPage();

		// get new topology tab
		DashboardTab dashboardTab = loginPage.login().switchToDashboard();

		LogUtils.log("disconnecting replication channel...");
		admin.getGridServiceContainers().getContainers()[0].kill();
		admin.getGridServiceContainers().waitFor(1);
		admin.getProcessingUnits().getProcessingUnit("Test").waitFor(1);

		LogUtils.log("waiting 1 minute for alerts to arrive");
		raisedReplicationAlertLatch.await(60, TimeUnit.SECONDS);
		
		log("waiting for alert to appear in the webui");
		final AlertsGrid alertsGrid = dashboardTab.getAlertsGrid();
		RepetitiveConditionProvider condition = new RepetitiveConditionProvider() {
			public boolean getCondition() {
				List<WebUIAlert> webuiAlerts = alertsGrid.getParentAlertsByType(AlertsGrid.REPLICATION);
				for (WebUIAlert alert : webuiAlerts) {
					if (alert.getStatus().equals(AlertStatus.RAISED)){
						return true;
					}
				}
				return false;
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition,10000);

		LogUtils.log("retrieving alerts before refresh...");
		List<WebUIAlert> alertsBeforeRefresh = alertsGrid.getAlertsByStatus(AlertStatus.RAISED);
		if (alertsBeforeRefresh.size() == 0) {
			LogUtils.log("no alerts found");
		}
		
		LogUtils.log("refreshing page");
		refreshPage();
		
		takeScreenShot(this.getClass(), "duplicateAlerts");
		
		LogUtils.log("retrieving alerts after refresh...");
		List<WebUIAlert> alertsAfterRefresh = alertsGrid.getAlertsByStatus(AlertStatus.RAISED);
		if (alertsAfterRefresh.size() == 0) {
			LogUtils.log("no alerts found");
		}
		
		log("number of alerts before refresh = " + alertsBeforeRefresh.size());
		log("number of alerts after refresh = " + alertsAfterRefresh.size());
		assertTrue(alertsAfterRefresh.size() == alertsBeforeRefresh.size());	
	}
	
	@Override
	@AfterMethod(alwaysRun = true)
	public void afterTest() {
		WebUiUtils.useAlertXmlConfigurationFile(originalAlertXml);
		super.afterTest();
	} 
	 
}
