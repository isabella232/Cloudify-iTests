package test.webui.dashboard.alerts;

import static test.utils.AdminUtils.loadGSCs;
import static test.utils.AdminUtils.loadGSM;
import static test.utils.LogUtils.log;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.openqa.selenium.WebDriver;
import org.openspaces.admin.alert.AlertStatus;
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

import com.gigaspaces.cluster.activeelection.SpaceMode;
import com.thoughtworks.selenium.Selenium;

import framework.tools.SGTestHelper;

public class TwoBrowsersAlertConsistencyTest extends AbstractSeleniumTest {
	
	Machine machineA;
	ProcessingUnit pu;
	GridServiceManager gsmA;
	
	WebDriver secondDriver;
	Selenium secondSelenium;
	
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
	}
	
	 @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = {"cloudify" , "xap"})
	public void twoBrowsersAlertTest() throws InterruptedException {
		
		// get new login page
		LoginPage loginPage = getLoginPage();
		
		// get new topology tab
		DashboardTab dashboardTab = loginPage.login().switchToDashboard();
		
		LogUtils.log("deplying (1,1) space cluster...");
		SpaceDeployment deployment = new SpaceDeployment("Test").partitioned(1, 1).maxInstancesPerVM(1);
		pu = gsmA.deploy(deployment);
		ProcessingUnitUtils.waitForDeploymentStatus(pu, DeploymentStatus.INTACT);
		
		log("waiting for alert to appear in the webui");
		final AlertsGrid alertsGrid = dashboardTab.getAlertsGrid();
		RepetitiveConditionProvider condition = new RepetitiveConditionProvider() {
			public boolean getCondition() {
				List<WebUIAlert> webuiAlerts = alertsGrid.getParentAlertsByType(AlertsGrid.REPLICATION);
				for (WebUIAlert alert : webuiAlerts) {
					if (alert.getStatus().equals(AlertStatus.RESOLVED)){
						return true;
					}
				}
				return false;
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition,30000);
		
		LogUtils.log("disconnecting replication channel...");
		admin.getGridServiceContainers().getContainers()[0].kill();
		admin.getGridServiceContainers().waitFor(1);
		admin.getProcessingUnits().getProcessingUnit("Test").waitFor(1);
		admin.getProcessingUnits().getProcessingUnits()[0].getInstances()[0].getSpaceInstance().waitForMode(SpaceMode.BACKUP, 20, TimeUnit.SECONDS);
		admin.getProcessingUnits().getProcessingUnits()[0].getInstances()[0].getSpaceInstance().waitForMode(SpaceMode.PRIMARY, 20, TimeUnit.SECONDS);
		
		log("waiting for alert to appear in the webui");
		condition = new RepetitiveConditionProvider() {
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
		AssertUtils.repetitiveAssertTrue(null, condition,30000);
		
		takeScreenShot(this.getClass(), "twoBrowsersAlertTest");
		
		LogUtils.log("retrieving alerts from webui...");
		AlertsGrid alertGrid = dashboardTab.getAlertsGrid();
		List<WebUIAlert> replicationAlertsBrowserOne = alertGrid.getAlertsByType(AlertsGrid.REPLICATION);
		
		log("start another browser...");
		// get new login page
		LoginPage loginPage2 = openAndSwitchToNewBrowser(AbstractSeleniumTest.FIREFOX);
		
		// get new topology tab
		DashboardTab dashboardTab2 = loginPage2.login().switchToDashboard();
		
		AlertsGrid alertGrid2 = dashboardTab2.getAlertsGrid();
		
		log("retrieve alerts from current browser");
		List<WebUIAlert> replicationAlertsBrowserTwo = alertGrid2.getAlertsByType(AlertsGrid.REPLICATION);
		
		// TODO check for alerts consistency better
		assertTrue(replicationAlertsBrowserOne.size() == replicationAlertsBrowserTwo.size());
	}

	 @Override
	 @AfterMethod(alwaysRun = true)
	 public void afterTest() {
		 WebUiUtils.useAlertXmlConfigurationFile(originalAlertXml);
		 super.afterTest();
	 }  

}
