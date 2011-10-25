package test.alerts;

import static test.utils.AdminUtils.loadGSCs;
import static test.utils.AdminUtils.loadGSM;
import static test.utils.LogUtils.log;

import java.util.ArrayList;
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
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.AbstractTest;
import test.utils.LogUtils;
import test.utils.ProcessingUnitUtils;

public class TwoListenersAlertConsistencyTest extends AbstractTest {
	
	Machine machineA;
	ProcessingUnit pu;
	GridServiceManager gsmA;
	
	@BeforeMethod
	public void startSetup() {
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
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT)
	public void twoListenersTest() throws InterruptedException {
		
		final List<Alert> listenerOneAlerts = new ArrayList<Alert>();
		final List<Alert> listenerTwoAlerts = new ArrayList<Alert>();
		
		final CountDownLatch listenerOneLatch = new CountDownLatch(2);
		
		// lets register to admin alerts
		final AlertManager alertManager = admin.getAlertManager();
		alertManager.setConfig(new ReplicationChannelDisconnectedAlertConfigurer().create());

		alertManager.enableAlert(ReplicationChannelDisconnectedAlertConfiguration.class);
		
		alertManager.getAlertTriggered().add(new AlertTriggeredEventListener() {
			public void alertTriggered(Alert alert) {
				listenerOneAlerts.add(alert);
				log(alert.toString());
				if (alert.getStatus().equals(AlertStatus.RESOLVED)) listenerOneLatch.countDown();
			}
		});
		
		LogUtils.log("deplying (1,1) space cluster...");
		// this should trigger 2 alerts
		SpaceDeployment deployment = new SpaceDeployment("Test").partitioned(1, 1).maxInstancesPerVM(1);
		pu = gsmA.deploy(deployment);
		ProcessingUnitUtils.waitForDeploymentStatus(pu, DeploymentStatus.INTACT);
		
		LogUtils.log("disconnecting replication channel...");
		admin.getGridServiceContainers().getContainers()[0].kill();
		admin.getGridServiceContainers().waitFor(1);
		admin.getProcessingUnits().getProcessingUnit("Test").waitFor(1);
		
		LogUtils.log("restoring replication channel...");
		loadGSCs(machineA, 1);
		
		admin.getGridServiceContainers().waitFor(2);
		admin.getProcessingUnits().getProcessingUnit("Test").waitFor(2);
		
		ProcessingUnitUtils.waitForDeploymentStatus(pu, DeploymentStatus.INTACT);
		
		
		listenerOneLatch.await(60, TimeUnit.SECONDS);
		
		final CountDownLatch listenerTwoLatch = new CountDownLatch(listenerOneAlerts.size());
		
		alertManager.getAlertTriggered().add(new AlertTriggeredEventListener() {
			public void alertTriggered(Alert alert) {
				listenerTwoAlerts.add(alert);
				log(alert.toString());
				listenerTwoLatch.countDown();
			}
		});
		
		listenerTwoLatch.await(60, TimeUnit.SECONDS);
		
		assertTrue(listenerTwoAlerts.size() == listenerOneAlerts.size());
	}

}
