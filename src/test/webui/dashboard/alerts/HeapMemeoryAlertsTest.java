package test.webui.dashboard.alerts;

import framework.tools.SGTestHelper;
import framework.utils.AdminUtils;
import framework.utils.AssertUtils;
import framework.utils.LogUtils;
import framework.utils.ProcessingUnitUtils;
import framework.utils.WebUiUtils;
import framework.utils.AssertUtils.RepetitiveConditionProvider;
import junit.framework.Assert;
import org.openspaces.admin.alert.Alert;
import org.openspaces.admin.alert.AlertManager;
import org.openspaces.admin.alert.AlertStatus;
import org.openspaces.admin.alert.config.HeapMemoryUtilizationAlertConfiguration;
import org.openspaces.admin.alert.config.HeapMemoryUtilizationAlertConfigurer;
import org.openspaces.admin.alert.events.AlertTriggeredEventListener;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.DeploymentStatus;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.space.SpaceDeployment;
import org.openspaces.core.GigaSpace;
import org.openspaces.core.SpaceMemoryShortageException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.data.Message;
import test.webui.AbstractSeleniumTest;
import test.webui.objects.dashboard.AlertsGrid;
import test.webui.objects.dashboard.DashboardTab;
import test.webui.objects.dashboard.AlertsGrid.WebUIAlert;
import test.webui.objects.LoginPage;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static framework.utils.AdminUtils.loadGSM;
import static framework.utils.LogUtils.log;

public class HeapMemeoryAlertsTest extends AbstractSeleniumTest {

    Machine machineA;
    ProcessingUnit pu;
    Alert firstAlert;

    @Override
    @BeforeMethod(alwaysRun = true)
    public void beforeTest(){
        String filepath = SGTestHelper.getSGTestRootDir() + "/src/test/webui/resources/alerts/heapAlert.xml";
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

        log("loading 1 GSC on 1 machine");
        AdminUtils.loadGSC(machineA);

        log("deploying the processing unit...");
        pu = gsmA.deploy(new SpaceDeployment("Test").numberOfInstances(1)
                .numberOfBackups(0).maxInstancesPerVM(1));
        ProcessingUnitUtils.waitForDeploymentStatus(pu, DeploymentStatus.INTACT);
        log("finished deploying");
    }

    @Test(timeOut = DEFAULT_TEST_TIMEOUT)
    public void memoryAlert() throws InterruptedException {

        final List<Alert> adminAlerts = new ArrayList<Alert>();
        final CountDownLatch hiThreshold = new CountDownLatch(1);
        final CountDownLatch lowThreshold = new CountDownLatch(1);

        GigaSpace space = pu.waitForSpace(30, TimeUnit.SECONDS).getGigaSpace();

        final AlertManager alertManager = admin.getAlertManager();
        alertManager.setConfig(new HeapMemoryUtilizationAlertConfigurer()
                .measurementPeriod(5, TimeUnit.SECONDS)
                .raiseAlertIfHeapAbove(30).resolveAlertIfHeapBelow(20)
                .create());

        alertManager.enableAlert(HeapMemoryUtilizationAlertConfiguration.class);

        alertManager.getAlertTriggered().add(new AlertTriggeredEventListener() {
            public void alertTriggered(Alert alert) {
                if (alert.getStatus().equals(AlertStatus.RAISED)) {
                    if (firstAlert == null) firstAlert = alert;
                    hiThreshold.countDown();
                    adminAlerts.add(alert);
                }
                if (alert.getStatus().equals(AlertStatus.RESOLVED)) {
                    lowThreshold.countDown();
                    adminAlerts.add(alert);
                }
                System.out.println(alert);
            }
        });

        // get new login page
        LoginPage loginPage = getLoginPage();

        // get new topology tab
        DashboardTab dashboardTab = loginPage.login().switchToDashboard();

        LogUtils.log("Writing to space until an alert is raised...");
        int i = 0;
        while (hiThreshold.getCount() > 0) {
            try {
                space.write(new Message(new Long(i), new byte[10 * 1024 * 1024]));
                i++;
                Thread.sleep(5000);
            } catch (SpaceMemoryShortageException e) {
                Assert.fail("Memory Shortage!");
            }
        }
        hiThreshold.await(10, TimeUnit.SECONDS);

        LogUtils.log("cleaning space until the alert is resolved...");
        while (lowThreshold.getCount() > 0) {
            space.clear(null);
            admin.getGridServiceContainers().getContainers()[0].getVirtualMachine().runGc();
        }

		log("waiting for resolved alert to appear in the web ui...");
		
		final AlertsGrid alertGrid = dashboardTab.getAlertsGrid();
		RepetitiveConditionProvider condition = new RepetitiveConditionProvider() {
			public boolean getCondition() {
				List<WebUIAlert> webuiAlerts = alertGrid.getParentAlertsByType(AlertsGrid.HEAP_MEMORY);
				for (WebUIAlert alert : webuiAlerts) {
					if (alert.getStatus().equals(AlertStatus.RESOLVED)){
						return true;
					}
				}
				return false;
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition,30000);
		
		takeScreenShot(this.getClass(), "memoryAlert");

        log("retrieving alerts from webui...");
        List<WebUIAlert> heapAlerts = alertGrid.getAlertsByType(AlertsGrid.HEAP_MEMORY);
        List<WebUIAlert> otherAlerts = alertGrid.getAlertsAppartFrom(AlertsGrid.HEAP_MEMORY);

        log("verifying consistency...");

        // check no other alerts were triggered, except memory or cpu
        if (otherAlerts.size() != 0) {
            for (WebUIAlert alert : otherAlerts) {
                assertTrue(alert.getName().equals(AlertsGrid.PHYSICAL_MEMORY) ||
                        alert.getName().equals(AlertsGrid.CPU_UTILIZATION));
            }
        }
        alertGrid.assertAlertsConsistency(heapAlerts, adminAlerts);
        LogUtils.log("finished");
    }

    @AfterMethod(alwaysRun = true)
    public void undeployPu() {
        pu.undeploy();
        ProcessingUnitUtils.waitForDeploymentStatus(pu, DeploymentStatus.UNDEPLOYED);
        WebUiUtils.useAlertXmlConfigurationFile(originalAlertXml);
    }

}
