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
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.core.GigaSpace;
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

public class PhysicalMemoryAlertTest extends AbstractSeleniumTest {
	
	Machine machineA;
	ProcessingUnit pu;
	GridServiceManager gsmA;
	GigaSpace space;
	Alert resolvedAlert;
	
	@Override
    @BeforeMethod(alwaysRun = true)
    public void beforeTest() {
		
		String filepath = SGTestHelper.getSGTestRootDir() + "/src/test/webui/resources/alerts/physicalMemAlert.xml";
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
	public void physicalMemoryTest() throws InterruptedException {
		
		String methodName = "physicalMemoryTest";
		
		final List<Alert> adminAlerts = new ArrayList<Alert>();
		
		final CountDownLatch hiThresholdAlertLatch = new CountDownLatch(1);
		final CountDownLatch lowThresholdAlertLatch = new CountDownLatch(1);
				
		final AlertManager alertManager = admin.getAlertManager();
		alertManager.setConfig(new PhysicalMemoryUtilizationAlertConfigurer()
			.measurementPeriod(5, TimeUnit.SECONDS)
			.raiseAlertIfMemoryAbove(70).resolveAlertIfMemoryBelow(50)
			.create());       

		alertManager.enableAlert(PhysicalMemoryUtilizationAlertConfiguration.class);
		
		alertManager.getAlertTriggered().add(new AlertTriggeredEventListener() {
			public void alertTriggered(Alert alert) {
				if (alert.getStatus().equals(AlertStatus.RAISED)) {
					adminAlerts.add(alert);
					hiThresholdAlertLatch.countDown();
				}
				else {
					if (alert.getStatus().equals(AlertStatus.RESOLVED)) {
						adminAlerts.add(alert);
						lowThresholdAlertLatch.countDown();
					}	
				}	
			}
		});	
		
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
					hiThresholdAlertLatch.countDown();
				}
				else {
					if (alert.getStatus().equals(AlertStatus.RESOLVED)) {
						adminAlerts.add(alert);
						lowThresholdAlertLatch.countDown();
					}	
				}	
			}
		});	
        
		log("loading gsc's to trigger an alert...");
		int i = 1;
		while (hiThresholdAlertLatch.getCount() > 0) {
			AdminUtils.loadGSC(machineA);
			admin.getGridServiceContainers().waitFor(i++);
			Thread.sleep(5000);
		}
		
		log("waiting for alerts to arriver");
		hiThresholdAlertLatch.await(30, TimeUnit.SECONDS);
		
		log("terminating gsc's to resolve alert...");
		GridServiceContainer [] allGSC = admin.getGridServiceContainers().getContainers();
		for (int j = 0 ; j < allGSC.length ; j ++ ) {
			allGSC[j].kill();		
		}
		admin.getGridServiceContainers().waitFor(0);
		
		log("waiting for alert to be resolved");
		lowThresholdAlertLatch.await(60, TimeUnit.SECONDS);
		assertTrue(lowThresholdAlertLatch.getCount() == 0);
		
		log("waiting for resolved alert to appear in the web ui...");
		
		final AlertsGrid alertGrid = dashboardTab.getAlertsGrid();
		condition = new RepetitiveConditionProvider() {
			public boolean getCondition() {
				List<WebUIAlert> webuiAlerts = alertGrid.getParentAlertsByType(AlertsGrid.PHYSICAL_MEMORY);
				for (WebUIAlert alert : webuiAlerts) {
					if (alert.getStatus().equals(AlertStatus.RESOLVED)){
						return true;
					}
				}
				return false;
			}
		};
		repetitiveAssertTrueWithScreenshot(null, condition,this.getClass(), methodName);
		
		log("retrieving alerts from webui...");
		List<WebUIAlert> physicalMemoryAlerts = alertGrid.getAlertsByType(AlertsGrid.PHYSICAL_MEMORY);
		
        log("verifying consistency...");
        takeScreenShot(this.getClass(), methodName);
        alertGrid.assertAlertsConsistency(physicalMemoryAlerts, adminAlerts);
        LogUtils.log("finished");
        
	}
	
	@Override
	@AfterMethod(alwaysRun = true)
	public void afterTest() {
		WebUiUtils.useAlertXmlConfigurationFile(originalAlertXml);
		super.afterTest();
	} 

}
