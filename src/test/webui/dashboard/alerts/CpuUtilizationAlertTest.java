package test.webui.dashboard.alerts;

import static framework.utils.AdminUtils.loadGSM;
import static framework.utils.LogUtils.log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.openspaces.admin.alert.Alert;
import org.openspaces.admin.alert.AlertManager;
import org.openspaces.admin.alert.AlertStatus;
import org.openspaces.admin.alert.config.CpuUtilizationAlertConfiguration;
import org.openspaces.admin.alert.config.CpuUtilizationAlertConfigurer;
import org.openspaces.admin.alert.events.AlertTriggeredEventListener;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.core.GigaSpace;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.webui.AbstractSeleniumTest;
import test.webui.objects.LoginPage;
import test.webui.objects.dashboard.AlertsGrid;
import test.webui.objects.dashboard.AlertsGrid.WebUIAlert;
import test.webui.objects.dashboard.DashboardTab;
import framework.tools.SGTestHelper;
import framework.utils.AdminUtils;
import framework.utils.AssertUtils;
import framework.utils.LogUtils;
import framework.utils.WebUiUtils;
import framework.utils.AssertUtils.RepetitiveConditionProvider;

public class CpuUtilizationAlertTest extends AbstractSeleniumTest {
	
	Machine machineA;
	ProcessingUnit pu;
	GridServiceManager gsmA;
	GigaSpace space;
	Alert resolvedAlert;
	
	@Override
    @BeforeMethod(alwaysRun = true)
    public void beforeTest() {
		
		String filepath = SGTestHelper.getSGTestRootDir() + "/src/test/webui/resources/alerts/cpuAlert.xml";
		LogUtils.log("preparing appropriate alert.xml...");
		WebUiUtils.useAlertXmlConfigurationFile(filepath);	
		super.beforeTest();
		
		log("waiting for 1 GSA");
		admin.getGridServiceAgents().waitFor(1);
		
		GridServiceAgent[] agents = admin.getGridServiceAgents().getAgents();
		GridServiceAgent gsaA = agents[0];
		
		machineA = gsaA.getMachine();
		
		log("loading GSM");
		gsmA = loadGSM(machineA);
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = {"cloudify" , "xap"})
	public void cpuUtilizationTest() throws InterruptedException {
		
		final List<Alert> adminAlerts = new ArrayList<Alert>();
		final CountDownLatch lowThresholdAlertLatch = new CountDownLatch(1);
		final CountDownLatch hiThresholdAlertLatch = new CountDownLatch(1);
		
		final AlertManager alertManager = admin.getAlertManager();
		
		// get new login page
		LoginPage loginPage = getLoginPage();
		
		// get new topology tab
		final DashboardTab dashboardTab = loginPage.login().switchToDashboard();
		
		
		RepetitiveConditionProvider condition = new RepetitiveConditionProvider() {
			public boolean getCondition() {
				double perc = dashboardTab.getStatusGrid().getCpuCores().getCount();
				System.out.println(perc);
				return ((perc < 80) && (perc > 0));
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition,10000);

		alertManager.setConfig(new CpuUtilizationAlertConfigurer()
		.raiseAlertIfCpuAbove(80).resolveAlertIfCpuBelow(65).measurementPeriod(5, TimeUnit.SECONDS)
		.create());       

		alertManager.enableAlert(CpuUtilizationAlertConfiguration.class);

		alertManager.getAlertTriggered().add(new AlertTriggeredEventListener() {
			public void alertTriggered(Alert alert) {
				if (alert.getStatus().equals(AlertStatus.RAISED)) {
					adminAlerts.add(alert);
					hiThresholdAlertLatch.countDown();
				}
				if (alert.getStatus().equals(AlertStatus.RESOLVED)) {
					adminAlerts.add(alert);
					lowThresholdAlertLatch.countDown();
				}
				LogUtils.log(alert.toString());
			}

		});	
		
		/*
		 * this causes the CPU utilization on the machines to go up, causing an alert to rise.
		 * once the loading is over, the utilization should return to normal, and the alert should be resolved.
		 */
		log("loading gsc, waiting for alert to arive...");
		AdminUtils.loadGSC(machineA);
		
		lowThresholdAlertLatch.await(60, TimeUnit.SECONDS);
		
		alertManager.disableAlert(CpuUtilizationAlertConfiguration.class);
		
		condition = new RepetitiveConditionProvider() {
			public boolean getCondition() {
				double perc = dashboardTab.getStatusGrid().getCpuCores().getCount();
				System.out.println(perc);
				return ((perc < 65) && (perc > 0));
			}
		};
		repetitiveAssertTrueWithScreenshot(null, condition,this.getClass(), "cpuUtilizationTest");
		
		takeScreenShot(this.getClass(), "cpuUtilizationTest");
		
		log("retrieving alerts from webui...");
		AlertsGrid alertGrid = dashboardTab.getAlertsGrid();
		List<WebUIAlert> cpuAlerts = alertGrid.getAlertsByType(AlertsGrid.CPU_UTILIZATION);
		
		log("verifying consistency...");
		takeScreenShot(this.getClass(), "cpuUtilizationTest");
		alertGrid.assertAlertsConsistency(cpuAlerts, adminAlerts);
		LogUtils.log("finished");
	}
	
	@AfterMethod(alwaysRun = true)
	public void after() {
		WebUiUtils.useAlertXmlConfigurationFile(originalAlertXml);
	}

}
