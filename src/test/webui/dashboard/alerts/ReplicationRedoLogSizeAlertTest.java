package test.webui.dashboard.alerts;

import static framework.utils.AdminUtils.loadGSM;
import static framework.utils.LogUtils.log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import junit.framework.Assert;

import org.openspaces.admin.alert.Alert;
import org.openspaces.admin.alert.AlertManager;
import org.openspaces.admin.alert.AlertStatus;
import org.openspaces.admin.alert.config.ReplicationRedoLogSizeAlertConfiguration;
import org.openspaces.admin.alert.config.ReplicationRedoLogSizeAlertConfigurer;
import org.openspaces.admin.alert.events.AlertTriggeredEventListener;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.DeploymentStatus;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.openspaces.admin.space.SpaceDeployment;
import org.openspaces.core.GigaSpace;
import org.openspaces.core.SpaceMemoryShortageException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.gigaspaces.cluster.activeelection.SpaceMode;

import framework.utils.AdminUtils;
import framework.utils.LogUtils;
import framework.utils.ProcessingUnitUtils;

import test.data.Message;
import test.webui.AbstractSeleniumTest;
import test.webui.objects.LoginPage;
import test.webui.objects.dashboard.AlertsGrid;
import test.webui.objects.dashboard.DashboardTab;
import test.webui.objects.dashboard.AlertsGrid.WebUIAlert;

public class ReplicationRedoLogSizeAlertTest extends AbstractSeleniumTest {
	
	Machine machineA, machineB, machineC;
	ProcessingUnit Pu;
	GridServiceManager gsmA;
	Alert resolvedAlert;
	
	@BeforeMethod(alwaysRun = true)
	public void startSetUp() {
			
		log("waiting for 1 GSA");
		admin.getGridServiceAgents().waitFor(1);
		
		GridServiceAgent[] agents = admin.getGridServiceAgents().getAgents();
		GridServiceAgent gsaA = agents[0];
		
		machineA = gsaA.getMachine();
		
		log("loading GSM");
		gsmA = loadGSM(machineA);
		
		log("loading 2 GSC's");
		AdminUtils.loadGSCWithSystemProperty(machineA, "-Xmx512m");
		AdminUtils.loadGSCWithSystemProperty(machineA, "-Xmx512m");
		
		log("deploying the processing unit");
		Pu = gsmA.deploy(new SpaceDeployment("Test").numberOfInstances(1)
				.numberOfBackups(1).maxInstancesPerVM(1));
		ProcessingUnitUtils.waitForDeploymentStatus(Pu, DeploymentStatus.INTACT);
		
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT )
	public void replicationRedoLogSize() throws InterruptedException {
		
		final List<Alert> adminAlerts = new ArrayList<Alert>();
		
		final CountDownLatch hiThreshold = new CountDownLatch(1);
		final CountDownLatch lowThreshold = new CountDownLatch(1);
		
		GigaSpace space = Pu.getSpace().getGigaSpace();
		
		final AlertManager alertManager = admin.getAlertManager();
		alertManager.setConfig(new ReplicationRedoLogSizeAlertConfigurer()
			.raiseAlertIfRedoLogSizeAbove(100000).resolveAlertIfRedoLogSizeBelow(1000)
			.create());       

		alertManager.enableAlert(ReplicationRedoLogSizeAlertConfiguration.class);
		
		alertManager.getAlertTriggered().add(new AlertTriggeredEventListener() {
			public void alertTriggered(Alert alert) {
				if (alert.getStatus().equals(AlertStatus.RAISED)) {
					adminAlerts.add(alert);
					log("Hi Threshold :  " + alert);
					hiThreshold.countDown();
				}
				if (alert.getStatus().equals(AlertStatus.RESOLVED)) {
					resolvedAlert = alert;
					log("Low Threshold :  " + alert);
					lowThreshold.countDown();
				}
			}
			
		});	
		
		// get new login page
		LoginPage loginPage = getLoginPage();
		
		// get new topology tab
		DashboardTab dashboardTab = loginPage.login().switchToDashboard();
		
		int i = 0;
		while (i < 120000) {
			try {
				if (i == 1000) {
					ProcessingUnitInstance[] processingUnits = Pu.getInstances();
					for (int j = 0 ; j < processingUnits.length ; j++) {
						ProcessingUnitInstance  puInst = processingUnits[j];
						if (puInst.getSpaceInstance().getMode().equals(SpaceMode.BACKUP)) {
							puInst.getGridServiceContainer().kill();
							break;
						}
					}
				}
				space.write(new Message(new Long(i) , new byte[1024]));
				i++;
			}
			catch (SpaceMemoryShortageException e) {
				Assert.fail("Memory Shortage!");
			}
		}
		
		AdminUtils.loadGSCWithSystemProperty(machineA, "-Xmx512m");
		
		hiThreshold.await(20, TimeUnit.SECONDS);
		lowThreshold.await(20, TimeUnit.SECONDS);	
		
		takeScreenShot(this.getClass(), "replicationRedoLogSize");
		
		log("retrieving alerts from webui...");
		AlertsGrid alertGrid = dashboardTab.getAlertsGrid();
		List<WebUIAlert> redologAlerts = alertGrid.getAlertsByType(AlertsGrid.REDOLOG_SIZE);
		
        log("verifying consistency...");

        alertGrid.assertAlertsConsistency(redologAlerts, adminAlerts);
        LogUtils.log("finished");
	}

}
