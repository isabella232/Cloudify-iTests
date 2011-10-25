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
import org.openspaces.admin.alert.alerts.ReplicationRedoLogSizeAlert;
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
import test.AbstractTest;
import test.utils.AdminUtils;
import test.utils.ProcessingUnitUtils;


/**
 * /**
 * test for RedoLog size. this test writes object to the space, and then terminates a space backup instance.
 * at witch point objects are replicated to the RedoLog, we set a size capacity limit, and ensure an alert is
 * triggered once the number of objects in greater than that limit. the redoLog starts writing objects to the disk.
 * we then load a gsc so that a connection to the primary is restored and the RedoLog reduces to zero. 
 * the test ensures 2 alerts have triggered, one for going over the limit, and one for returning to normal when the 
 * RedoLog is cleared.
 * 
 * @author elip
 *
 */
public class ReplicationRedoLogSizeAlertTest extends AbstractTest {
	
	Machine machineA, machineB, machineC;
	ProcessingUnit Pu;
	GridServiceManager gsmA;
	Alert resolvedAlert;
	
	@BeforeMethod
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
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT , groups = "1")
	public void replicationRedoLogSize() throws InterruptedException {
		
		final List<Alert> sizeAlerts = new ArrayList<Alert>();
		
		final CountDownLatch hiThreshold = new CountDownLatch(1);
		final CountDownLatch lowThreshold = new CountDownLatch(1);
		
		GigaSpace space = Pu.getSpace().getGigaSpace();
		
		final AlertManager alertManager = admin.getAlertManager();
		alertManager.setConfig(new ReplicationRedoLogSizeAlertConfigurer()
			.raiseAlertIfRedoLogSizeAbove(500).resolveAlertIfRedoLogSizeBelow(100)
			.create());       

		alertManager.enableAlert(ReplicationRedoLogSizeAlertConfiguration.class);
		
		alertManager.getAlertTriggered().add(new AlertTriggeredEventListener() {
			public void alertTriggered(Alert alert) {
				if (alert.getStatus().equals(AlertStatus.RAISED)) {
					sizeAlerts.add(alert);
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
		
		int i = 0;
		while (i < 700) {
			try {
				if (i == 100) {
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
		
		Assert.assertNotNull(resolvedAlert);
		Assert.assertTrue(sizeAlerts.size() > 0);
		
		for (Alert alert : sizeAlerts) {
			Assert.assertEquals(AlertSeverity.WARNING, alert.getSeverity());
			Assert.assertEquals(AlertStatus.RAISED, alert.getStatus());
			Assert.assertTrue(alert instanceof ReplicationRedoLogSizeAlert);
			Assert.assertNotNull(((ReplicationRedoLogSizeAlert)alert).getVirtualMachineUid());
		}
		
		Assert.assertEquals(AlertSeverity.WARNING, resolvedAlert.getSeverity());
		Assert.assertEquals(AlertStatus.RESOLVED, resolvedAlert.getStatus());
		Assert.assertTrue(resolvedAlert instanceof ReplicationRedoLogSizeAlert);	
		Assert.assertNotNull(((ReplicationRedoLogSizeAlert)resolvedAlert).getVirtualMachineUid());
		
	}

}
