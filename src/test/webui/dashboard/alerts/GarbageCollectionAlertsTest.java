package test.webui.dashboard.alerts;

import framework.tools.SGTestHelper;
import framework.utils.AdminUtils;
import framework.utils.LogUtils;
import framework.utils.ProcessingUnitUtils;
import framework.utils.WebUiUtils;

import org.openspaces.admin.alert.Alert;
import org.openspaces.admin.alert.AlertManager;
import org.openspaces.admin.alert.AlertStatus;
import org.openspaces.admin.alert.config.GarbageCollectionAlertConfiguration;
import org.openspaces.admin.alert.config.GarbageCollectionAlertConfigurer;
import org.openspaces.admin.alert.events.AlertTriggeredEventListener;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.DeploymentStatus;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.space.SpaceDeployment;
import org.openspaces.core.GigaSpace;
import org.openspaces.core.SpaceMemoryShortageException;
import org.testng.Assert;
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

public class GarbageCollectionAlertsTest extends AbstractSeleniumTest {

    Machine machineA;
    ProcessingUnit pu;
    Alert resolvedAlert;

    @Override
    @BeforeMethod(alwaysRun = true)
    public void beforeTest() {
        String filepath = SGTestHelper.getSGTestRootDir() + "/src/test/webui/resources/alerts/gcAlert.xml";
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
        AdminUtils.loadGSCWithSystemProperty(machineA, "-verbose:gc", "-Xmx128m", "-Xms128m");

        log("deploying the processing uni...t");
        pu = gsmA.deploy(new SpaceDeployment("Test").numberOfInstances(1)
                .numberOfBackups(0).maxInstancesPerVM(1));
        ProcessingUnitUtils.waitForDeploymentStatus(pu, DeploymentStatus.INTACT);
        pu.waitForSpace();
        log("finished deploying");
    }

    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = {"cloudify" , "xap"})
    public void garbageCollection() throws InterruptedException {

        final List<Alert> adminAlerts = new ArrayList<Alert>();

        final CountDownLatch hiThreshold = new CountDownLatch(1);
        final CountDownLatch lowThreshold = new CountDownLatch(1);

        GigaSpace space = pu.getSpace().getGigaSpace();

        // register for alerts
        final AlertManager alertManager = admin.getAlertManager();
        alertManager.setConfig(new GarbageCollectionAlertConfigurer()
                .raiseAlertForGcDurationOf(500, TimeUnit.MILLISECONDS).resolveAlertForGcDurationOf(500, TimeUnit.MILLISECONDS)
                .create());

        alertManager.enableAlert(GarbageCollectionAlertConfiguration.class);

        alertManager.getAlertTriggered().add(new AlertTriggeredEventListener() {
            public void alertTriggered(Alert alert) {
                if (alert.getStatus().equals(AlertStatus.RAISED)) {
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
                space.write(new Message(new Long(i), new byte[1]));
                i++;
            } catch (SpaceMemoryShortageException e) {
                Assert.fail("Memory Shortage!");
            }
        }
        hiThreshold.await(10, TimeUnit.SECONDS);

        LogUtils.log("taking from space until the alert is resolved...");
        while (lowThreshold.getCount() > 0 && i > 0) {
            try {
                space.take(new Message());
                i--;
            } catch (SpaceMemoryShortageException e) {
                Assert.fail("Memory Shortage!");
            }
        }
        lowThreshold.await(10, TimeUnit.SECONDS);

        Thread.sleep(5000);
        
        takeScreenShot(this.getClass(), "garbageCollection");

        log("retrieving alerts from webui...");
        AlertsGrid alertGrid = dashboardTab.getAlertsGrid();
        List<WebUIAlert> gcAlerts = alertGrid.getAlertsByType(AlertsGrid.GC);
        List<WebUIAlert> otherAlerts = alertGrid.getAlertsAppartFrom(AlertsGrid.GC);

        log("verifying consistency...");

        // check no other alerts were triggered, except memory or cpu
        if (otherAlerts.size() != 0) {
            for (WebUIAlert alert : otherAlerts) {
                assertTrue(alert.getName().equals(AlertsGrid.PHYSICAL_MEMORY) ||
                        alert.getName().equals(AlertsGrid.CPU_UTILIZATION));
            }
        }
        alertGrid.assertAlertsConsistency(gcAlerts, adminAlerts);
        LogUtils.log("finished");
    }

    @AfterMethod(alwaysRun = true)
    public void undeployPu() {
        pu.undeploy();
        ProcessingUnitUtils.waitForDeploymentStatus(pu, DeploymentStatus.UNDEPLOYED);
        WebUiUtils.useAlertXmlConfigurationFile(originalAlertXml);
    }

}
