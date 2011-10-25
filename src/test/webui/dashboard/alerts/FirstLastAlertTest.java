package test.webui.dashboard.alerts;

import static test.utils.AdminUtils.loadGSM;
import static test.utils.LogUtils.log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.openspaces.admin.alert.Alert;
import org.openspaces.admin.alert.AlertManager;
import org.openspaces.admin.alert.AlertStatus;
import org.openspaces.admin.alert.config.PhysicalMemoryUtilizationAlertConfiguration;
import org.openspaces.admin.alert.config.PhysicalMemoryUtilizationAlertConfigurer;
import org.openspaces.admin.alert.events.AlertTriggeredEventListener;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.machine.Machine;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.utils.AdminUtils;
import test.utils.AssertUtils.RepetitiveConditionProvider;
import test.utils.LogUtils;
import test.utils.WebUiUtils;
import test.webui.AbstractSeleniumTest;
import test.webui.objects.LoginPage;
import test.webui.objects.dashboard.AlertsGrid;
import test.webui.objects.dashboard.AlertsGrid.WebUIAlert;
import test.webui.objects.dashboard.DashboardTab;
import test.webui.objects.dashboard.StatusGrid;
import test.webui.objects.dashboard.StatusGrid.Memory;
import framework.tools.SGTestHelper;

public class FirstLastAlertTest extends AbstractSeleniumTest {
	
	Machine machineA;
	
	@Override
    @BeforeMethod(alwaysRun = true)
    public void beforeTest() {
		String filepath = SGTestHelper.getSGTestRootDir() + "/src/test/webui/resources/alerts/physicalMemAlert.xml";
		LogUtils.log("preparing appropriate alert.xml...");
		WebUiUtils.useAlertXmlConfigurationFile(filepath);
		super.beforeTest();
	}
	

	@BeforeMethod(alwaysRun = true)
	public void startSetUp() {
		
		log("waiting for 1 GSA");
		admin.getGridServiceAgents().waitFor(1);
		
		GridServiceAgent[] agents = admin.getGridServiceAgents().getAgents();
		GridServiceAgent gsaA = agents[0];

		machineA = gsaA.getMachine();
		
		log("loading GSM");
		GridServiceManager gsmA = loadGSM(machineA);
	}
	
	 @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = {"cloudify" , "xap"})
	public void physicalMemTest() throws InterruptedException {
		 
		String methodName = "physicalMemTest";	
		
		final List<Alert> adminAlerts = new ArrayList<Alert>();
		
		final CountDownLatch hiThresholdAlertLatch = new CountDownLatch(1);
		final CountDownLatch hiThresholdChildrenAlertLatch = new CountDownLatch(8);
		final CountDownLatch lowThresholdAlertLatch = new CountDownLatch(1);
		
		final AlertManager alertManager = admin.getAlertManager();
		alertManager.setConfig(new PhysicalMemoryUtilizationAlertConfigurer()
			.measurementPeriod(5, TimeUnit.SECONDS)
			.raiseAlertIfMemoryAbove(70).resolveAlertIfMemoryBelow(50)
			.create()); 	
		
		// get new login page
		LoginPage loginPage = getLoginPage();
		
		// get new topology tab
		DashboardTab dashboardTab = loginPage.login().switchToDashboard();
		
        StatusGrid statusGrid = dashboardTab.getStatusGrid();
        final Memory memory = statusGrid.getMemory();
        
		RepetitiveConditionProvider condition = new RepetitiveConditionProvider() {
			public boolean getCondition() {
				return (memory.getCount() < 55);
			}
		};
		repetitiveAssertTrueWithScreenshot(null, condition,this.getClass(), methodName);
			
		alertManager.enableAlert(PhysicalMemoryUtilizationAlertConfiguration.class);
		
		alertManager.getAlertTriggered().add(new AlertTriggeredEventListener() {
			public void alertTriggered(Alert alert) {
				if (alert.getStatus().equals(AlertStatus.RAISED)) {
					adminAlerts.add(alert);
					log("Hi Threshold : " + alert);
					hiThresholdChildrenAlertLatch.countDown();
					hiThresholdAlertLatch.countDown();
				}
				else {
					if (alert.getStatus().equals(AlertStatus.RESOLVED)) {
						adminAlerts.add(alert);
						log("Low Threshold :  " + alert);
						lowThresholdAlertLatch.countDown();
					}	
				}	
			}
		});
		
		
		assertTrue(adminAlerts.size() == 0);
		log("loading gsc's to trigger an alert...");
		int i = 1;
		while (hiThresholdAlertLatch.getCount() > 0) {
			AdminUtils.loadGSC(machineA);
			admin.getGridServiceContainers().waitFor(i++);
			Thread.sleep(5000);
		}
		
		hiThresholdChildrenAlertLatch.await(60, TimeUnit.SECONDS);
		
		log("terminating gsc's to resolve alert...");
		GridServiceContainer [] allGSC = admin.getGridServiceContainers().getContainers();
		for (int j = 0 ; j < allGSC.length ; j ++ ) {
			allGSC[j].kill();		
		}
		admin.getGridServiceContainers().waitFor(0);
		lowThresholdAlertLatch.await(60, TimeUnit.SECONDS);
		
		takeScreenShot(this.getClass(), "physicalMemTest");
		
		log("retrieving alerts from webui...");
		AlertsGrid alertGrid = dashboardTab.getAlertsGrid();
		List<WebUIAlert> physMemAlerts = alertGrid.getAlertsByType(AlertsGrid.PHYSICAL_MEMORY);
		
		WebUIAlert firstAlert = physMemAlerts.get(physMemAlerts.size() - 1);
		WebUIAlert lastAlert = physMemAlerts.get(0);
		
		int differenceInSeconds = (physMemAlerts.size() - 1) * 5;
	
		boolean con = (lastAlert.getTimeStampInSecond() - firstAlert.getTimeStampInSecond()) > differenceInSeconds;
		assertTrueWithScreenshot(con, this.getClass(), methodName);	
		
	}
	
	@AfterMethod(alwaysRun = true)
	public void undeployPu() {
		WebUiUtils.useAlertXmlConfigurationFile(originalAlertXml);
	}

}
