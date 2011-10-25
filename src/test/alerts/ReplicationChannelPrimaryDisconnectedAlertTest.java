package test.alerts;

import static test.utils.AdminUtils.loadGSM;
import static test.utils.LogUtils.log;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import junit.framework.Assert;

import org.openspaces.admin.alert.Alert;
import org.openspaces.admin.alert.AlertManager;
import org.openspaces.admin.alert.AlertSeverity;
import org.openspaces.admin.alert.AlertStatus;
import org.openspaces.admin.alert.alerts.ReplicationChannelDisconnectedAlert;
import org.openspaces.admin.alert.config.ReplicationChannelDisconnectedAlertConfiguration;
import org.openspaces.admin.alert.config.ReplicationChannelDisconnectedAlertConfigurer;
import org.openspaces.admin.alert.events.AlertTriggeredEventListener;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.DeploymentStatus;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.openspaces.admin.space.SpaceDeployment;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.gigaspaces.cluster.activeelection.SpaceMode;

import test.AbstractTest;
import test.utils.AdminUtils;
import test.utils.LogUtils;
import test.utils.ProcessingUnitUtils;

public class ReplicationChannelPrimaryDisconnectedAlertTest extends AbstractTest {
	
	Machine machineA;
	ProcessingUnit Pu;
	GridServiceManager gsmA;
	Alert resolvedAlert;
	Alert raisedAlert;

	@BeforeMethod
	public void startSetUp() {
			
		log("waiting for 1 GSA");
		admin.getGridServiceAgents().waitFor(1);
		
		GridServiceAgent[] agents = admin.getGridServiceAgents().getAgents();
		GridServiceAgent gsaA = agents[0];
		
		machineA = gsaA.getMachine();
		
		log("loading GSM");
		gsmA = loadGSM(machineA);
		
		log("loading 2 GSC's on 1 machine");
		AdminUtils.loadGSCs(machineA , 2);
		
		log("deploying the processing unit");
		Pu = gsmA.deploy(new SpaceDeployment("Test").numberOfInstances(1)
				.numberOfBackups(1).maxInstancesPerVM(1));
		ProcessingUnitUtils.waitForDeploymentStatus(Pu, DeploymentStatus.INTACT);
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT , groups = "1")
	public void replicationChannelBackupDisconnected() throws InterruptedException {
		
		final CountDownLatch connectionLost = new CountDownLatch(1);
		final CountDownLatch connectionRecovered = new CountDownLatch(1);
		
		final AlertManager alertManager = admin.getAlertManager();
		alertManager.setConfig(new ReplicationChannelDisconnectedAlertConfigurer().create());

		alertManager.enableAlert(ReplicationChannelDisconnectedAlertConfiguration.class);
		
		alertManager.getAlertTriggered().add(new AlertTriggeredEventListener() {
			public void alertTriggered(Alert alert) {
				if (alert.getStatus().equals(AlertStatus.RAISED)) {
					raisedAlert = alert;
					connectionLost.countDown();
				}
				if (alert.getStatus().equals(AlertStatus.RESOLVED)) {
					resolvedAlert = alert;
					connectionRecovered.countDown();
				}
				LogUtils.log(alert.toString());
			}
		});
		
		ProcessingUnitUtils.waitForActiveElection(Pu);
		ProcessingUnitInstance[] processingUnits = Pu.getInstances();
		for (int i = 0 ; i < processingUnits.length ; i++) {
			ProcessingUnitInstance  puInst = processingUnits[i];
			if (puInst.getSpaceInstance().getMode().equals(SpaceMode.PRIMARY)) {
				LogUtils.log("Killing primary space instance: " + puInst.getSpaceInstance());
				puInst.getGridServiceContainer().kill();
				break;
			}
		}
		
		LogUtils.log("Waiting 60 seconds for raised alert to arrive");
		Assert.assertTrue(connectionLost.await(60, TimeUnit.SECONDS));
		Assert.assertNotNull(raisedAlert);
		
		LogUtils.log("Loading GSC");
		AdminUtils.loadGSC(machineA);
		LogUtils.log("Waiting 60 seconds for resolved alert to arrive");
		Assert.assertTrue(connectionRecovered.await(60, TimeUnit.SECONDS));
		Assert.assertNotNull(resolvedAlert);
		
		Assert.assertEquals(AlertSeverity.SEVERE, resolvedAlert.getSeverity());
		Assert.assertEquals(AlertSeverity.SEVERE, raisedAlert.getSeverity());
		Assert.assertEquals(AlertStatus.RESOLVED, resolvedAlert.getStatus());
		Assert.assertEquals(AlertStatus.RAISED, raisedAlert.getStatus());
		Assert.assertTrue(raisedAlert instanceof ReplicationChannelDisconnectedAlert);
		Assert.assertTrue(resolvedAlert instanceof ReplicationChannelDisconnectedAlert);			
	}

}
