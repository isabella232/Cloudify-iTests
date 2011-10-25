package test.alerts;

import static test.utils.AdminUtils.loadGSM;
import static test.utils.LogUtils.log;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import junit.framework.Assert;
import org.openspaces.admin.alert.Alert;
import org.openspaces.admin.alert.AlertManager;
import org.openspaces.admin.alert.AlertSeverity;
import org.openspaces.admin.alert.AlertStatus;
import org.openspaces.admin.alert.alerts.HeapMemoryUtilizationAlert;
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
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import test.AbstractTest;
import test.utils.AdminUtils;
import test.utils.ProcessingUnitUtils;

/**
 * test for Heap memory alert. this test deploys a processing unit with an embedded space instance.
 * it then write objects to that space, and when finished we clean the space and run garbage collection.
 * this test ensures that the expected alerts were triggered.
 * @author elip
 *
 */
public class HeapMemoryUtilizationAlertTest extends AbstractTest {
	
	Machine machineA;
	ProcessingUnit Pu;
	Alert resolvedAlert;
	
	@BeforeMethod
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
		
		log("deploying the processing unit");
		Pu = gsmA.deploy(new SpaceDeployment("Test").numberOfInstances(1)
				.numberOfBackups(0).maxInstancesPerVM(1));
		ProcessingUnitUtils.waitForDeploymentStatus(Pu, DeploymentStatus.INTACT);
		
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT , groups = "1")
	public void memoryAlert() throws InterruptedException {
		
		final List<Alert> hiThresholdAlerts = new ArrayList<Alert>();
		final CountDownLatch hiThreshold = new CountDownLatch(1);
		final CountDownLatch lowThreshold = new CountDownLatch(1);
		
		GigaSpace space = Pu.getSpace().getGigaSpace();
		
		final AlertManager alertManager = admin.getAlertManager();
		alertManager.setConfig(new HeapMemoryUtilizationAlertConfigurer()
			.measurementPeriod(5, TimeUnit.SECONDS)
			.raiseAlertIfHeapAbove(30).resolveAlertIfHeapBelow(20)
			.create());       

		alertManager.enableAlert(HeapMemoryUtilizationAlertConfiguration.class);
		
		alertManager.getAlertTriggered().add(new AlertTriggeredEventListener() {
			public void alertTriggered(Alert alert) {
				if (alert.getStatus().equals(AlertStatus.RAISED)) {
					hiThresholdAlerts.add(alert);
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
		
		/*
		 * Writing objects to the space, causing the heap memory of each JVM to rise, and an alert to pop up
		 */
		int i = 0;
		while (hiThreshold.getCount() > 0) {
			try {
			space.write(new Message(new Long(i) , new byte[10*1024*1024]));
			i++;
			}
			catch (SpaceMemoryShortageException e) {
				Assert.fail("Memory Shortage!");
			}
		}
		
		/*
		 * Cleaning the space and running garbage collection
		 * this causes the heap memory the fall, and an alert to be shown
		 */
		space.clean();
		for (int l = 0 ; l < 5 ; l++) {
			admin.getGridServiceContainers().getContainers()[0].getVirtualMachine().runGc();
		}

		hiThreshold.await(10, TimeUnit.SECONDS);
		lowThreshold.await(10, TimeUnit.SECONDS);
		
		Assert.assertTrue(hiThresholdAlerts.size() > 0);
		Assert.assertNotNull(resolvedAlert);

		for (Alert alert : hiThresholdAlerts) {
			Assert.assertEquals(AlertSeverity.WARNING, alert.getSeverity());
			Assert.assertEquals(AlertStatus.RAISED, alert.getStatus());
			Assert.assertTrue(alert instanceof HeapMemoryUtilizationAlert);
		}
		
		Assert.assertEquals(AlertSeverity.WARNING, resolvedAlert.getSeverity());
		Assert.assertEquals(AlertStatus.RESOLVED, resolvedAlert.getStatus());
		Assert.assertTrue(resolvedAlert instanceof HeapMemoryUtilizationAlert);
	}		
}


