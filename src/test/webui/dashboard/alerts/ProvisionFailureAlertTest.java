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
import org.openspaces.admin.alert.config.ProvisionFailureAlertConfigurer;
import org.openspaces.admin.alert.events.AlertTriggeredEventListener;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.DeploymentStatus;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.space.SpaceDeployment;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.utils.AdminUtils;
import test.utils.LogUtils;
import test.utils.ProcessingUnitUtils;
import test.webui.AbstractSeleniumTest;
import test.webui.objects.dashboard.AlertsGrid;
import test.webui.objects.dashboard.DashboardTab;
import test.webui.objects.dashboard.AlertsGrid.WebUIAlert;
import test.webui.objects.LoginPage;

public class ProvisionFailureAlertTest extends AbstractSeleniumTest {
	
    Machine machineA;
    ProcessingUnit pu;
    GridServiceManager gsmA;
    
    @BeforeMethod(alwaysRun = true)
    public void startSetUp() {

        log("waiting for 1 GSA");
        admin.getGridServiceAgents().waitFor(1);

        GridServiceAgent[] agents = admin.getGridServiceAgents().getAgents();
        GridServiceAgent gsaA = agents[0];

        machineA = gsaA.getMachine();

        log("loading GSM");
        gsmA = loadGSM(machineA);

        log("loading 2 GSC on 1 machine");
        AdminUtils.loadGSC(machineA);
        AdminUtils.loadGSC(machineA);
    }

    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = {"cloudify" , "xap"})
    public void provisionTest() throws InterruptedException {
    	
    	final CountDownLatch resolvedLatch = new CountDownLatch(2);
    	final List<Alert> adminAlerts = new ArrayList<Alert>(); 
    	
        // get new login page
        LoginPage loginPage = getLoginPage();

        // get new topology tab
        DashboardTab dashboardTab = loginPage.login().switchToDashboard();
        
		final AlertManager alertManager = admin.getAlertManager();

		alertManager.configure(new ProvisionFailureAlertConfigurer().enable(true).create());  
		
        alertManager.getAlertTriggered().add(new AlertTriggeredEventListener() {
            public void alertTriggered(Alert alert) {
                if (alert.getStatus().equals(AlertStatus.RAISED)) {
                    adminAlerts.add(alert);
                }
                if (alert.getStatus().equals(AlertStatus.RESOLVED)) {                    
                    adminAlerts.add(alert);
                    resolvedLatch.countDown();
                }
                System.out.println(alert);
            }

        });
        
        log("deploying the processing unit...");
        pu = gsmA.deploy(new SpaceDeployment("Test").numberOfInstances(2)
                .numberOfBackups(1).maxInstancesPerVM(1));
        ProcessingUnitUtils.waitForDeploymentStatus(pu, DeploymentStatus.INTACT);
        log("finished deploying");
        
        admin.getGridServiceContainers().getContainers()[0].kill();
        admin.getGridServiceContainers().waitFor(1);
        AdminUtils.loadGSC(machineA);
        admin.getGridServiceContainers().waitFor(2);
        ProcessingUnitUtils.waitForDeploymentStatus(pu, DeploymentStatus.INTACT);
        
        resolvedLatch.await(60, TimeUnit.SECONDS);
        
        takeScreenShot(this.getClass(), "provisionTest");
        
        log("retrieving alerts from webui...");
        AlertsGrid alertGrid = dashboardTab.getAlertsGrid();
        List<WebUIAlert> provisionAlerts = alertGrid.getAlertsByType(AlertsGrid.PROVISION);
        
        alertGrid.assertAlertsConsistency(provisionAlerts, adminAlerts);
        LogUtils.log("finished");  	
    }
}
