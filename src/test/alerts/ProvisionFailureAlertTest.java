package test.alerts;

import static test.utils.AdminUtils.loadGSC;
import static test.utils.AdminUtils.loadGSM;
import static test.utils.LogUtils.log;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import org.openspaces.admin.alert.Alert;
import org.openspaces.admin.alert.AlertManager;
import org.openspaces.admin.alert.AlertStatus;
import org.openspaces.admin.alert.alerts.ProvisionFailureAlert;
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

import test.AbstractTest;
import test.utils.LogUtils;

/**
 * GS-8835
 * Provision failure alert is raised when there are less actual instances than planned.
 * Tests raising alerts when PU is BROKEN, COMPROMISED and resolving alert when INTACT or undeployed.
 * 
 * @author moran
 * @since 8.0.2
 */
public class ProvisionFailureAlertTest extends AbstractTest { 
	
	
	private Machine machine;
	private GridServiceManager gsm;

	@BeforeMethod
	public void startSetUp() {
			
		log("waiting for 1 GSA");
		GridServiceAgent gsa = admin.getGridServiceAgents().waitForAtLeastOne();
		machine = gsa.getMachine();
		
		log("loading GSM");
		gsm = loadGSM(machine);
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT , groups = "1")
	public void test() throws InterruptedException, ExecutionException {
		
		final AlertManager alertManager = admin.getAlertManager();

		alertManager.configure(new ProvisionFailureAlertConfigurer().enable(true).create());  
		
		ProcessingUnit pu = gsm.deploy(new SpaceDeployment("MySpace").numberOfInstances(2)
				.numberOfBackups(1).maxInstancesPerVM(1));
		
		provisionFailureWhenOnlyOneGscAvailable(pu);
		
		provisionFailureWhenNoGSCsAvailable(pu);
		
		provisionFailureWhenOnlyOneGscAvailable(pu);
		
		resolvedProvisionFailureWhenTwoGscAvailable(pu);
		
		provisionFailureWhenOneGscRemoved(pu);
		
		resolvedProvisionFailureWhenUndeployed(pu);
	}

	private ProcessingUnit provisionFailureWhenNoGSCsAvailable(ProcessingUnit pu) throws InterruptedException {
		
		LogUtils.log("provisionFailureWhenNoGSCsAvailable");
		
		CountDownLatch latch = new CountDownLatch(1);
		SafeAlertTriggeredEventListener listener = registerForAlerts(latch);

		LogUtils.log("kill GSC, wait for alert...");
		machine.getGridServiceContainers().getContainers()[0].kill();
		latch.await();
		assertEquals(DeploymentStatus.BROKEN, pu.getStatus());
		
		admin.getAlertManager().getAlertTriggered().remove(listener);
		assertEquals(listener.count.get(), 1);
		
		assertEquals(AlertStatus.RAISED, listener.alertEvent.getStatus());
		assertEquals(pu.getName(), listener.alertEvent.getComponentUid());
		
		return pu;
	}
	
	private void provisionFailureWhenOnlyOneGscAvailable(ProcessingUnit pu) throws InterruptedException {

		LogUtils.log("provisionFailureWhenOnlyOneGscAvailable");
		
		CountDownLatch latch = new CountDownLatch(1);
		SafeAlertTriggeredEventListener listener = registerForAlerts(latch);
		
		LogUtils.log("Load 1 GSC, 2 primaries should be provisioned on it.");
		loadGSC(machine);
		pu.waitFor(2);
		
		LogUtils.log("Load GSC, wait for alert...");
		latch.await();
		assertEquals(DeploymentStatus.COMPROMISED, pu.getStatus());
		
		admin.getAlertManager().getAlertTriggered().remove(listener);
		assertEquals(listener.count.get(), 1);
		
		assertEquals(AlertStatus.RAISED, listener.alertEvent.getStatus());
		assertEquals(pu.getName(), listener.alertEvent.getComponentUid());
	}
	
	private void resolvedProvisionFailureWhenTwoGscAvailable(ProcessingUnit pu) throws InterruptedException {
		
		LogUtils.log("resolvedProvisionFailureWhenTwoGscAvailable");
		
		CountDownLatch latch = new CountDownLatch(1);
		SafeAlertTriggeredEventListener listener = registerForAlerts(latch);
		
		LogUtils.log("Load 1 more GSC, 2 backups should be provisioned on it.");
		loadGSC(machine);
		pu.waitFor(4);
		
		LogUtils.log("Load GSC, wait for alert...");
		latch.await();
		assertEquals(DeploymentStatus.INTACT, pu.getStatus());
		
		admin.getAlertManager().getAlertTriggered().remove(listener);
		assertEquals(listener.count.get(), 1);
		
		assertEquals(AlertStatus.RESOLVED, listener.alertEvent.getStatus());
		assertEquals(pu.getName(), listener.alertEvent.getComponentUid());
	}
	
	private ProcessingUnit provisionFailureWhenOneGscRemoved(ProcessingUnit pu) throws InterruptedException {
		
		LogUtils.log("provisionFailureWhenOneGSCsRemoved");
		
		CountDownLatch latch = new CountDownLatch(1);
		SafeAlertTriggeredEventListener listener = registerForAlerts(latch);

		LogUtils.log("kill GSC, wait for alert...");
		machine.getGridServiceContainers().getContainers()[0].kill();
		latch.await();
		assertEquals(DeploymentStatus.COMPROMISED, pu.getStatus());
		
		admin.getAlertManager().getAlertTriggered().remove(listener);
		assertEquals(listener.count.get(), 1);
		
		assertEquals(AlertStatus.RAISED, listener.alertEvent.getStatus());
		assertEquals(pu.getName(), listener.alertEvent.getComponentUid());
		
		return pu;
	}
	
	private void resolvedProvisionFailureWhenUndeployed(ProcessingUnit pu) throws InterruptedException {
		
		LogUtils.log("resolvedProvisionFailureWhenUndeployed");
		
		CountDownLatch latch = new CountDownLatch(1);
		SafeAlertTriggeredEventListener listener = registerForAlerts(latch);
		
		LogUtils.log("Undeploy pu");
		pu.undeploy();

		LogUtils.log("Undeploy, wait for alert...");
		latch.await();
		assertEquals(DeploymentStatus.UNDEPLOYED, pu.getStatus());
		
		admin.getAlertManager().getAlertTriggered().remove(listener);
		assertEquals(listener.count.get(), 1);
		
		assertEquals(AlertStatus.RESOLVED, listener.alertEvent.getStatus());
		assertEquals(pu.getName(), listener.alertEvent.getComponentUid());
	}
	
	private SafeAlertTriggeredEventListener registerForAlerts(final CountDownLatch latch) {
		SafeAlertTriggeredEventListener listener = new SafeAlertTriggeredEventListener() {
			public void alertTriggered(Alert alert) {
				LogUtils.log("alert= " + alert);
				if (alert instanceof ProvisionFailureAlert) {
					count.incrementAndGet();
					if (latch.getCount() != 0) {
						alertEvent = alert;
						latch.countDown();
					}
				}
			}
		};
		admin.getAlertManager().getAlertTriggered().add(listener, false);
		return listener;
	}
	
	private static abstract class SafeAlertTriggeredEventListener implements AlertTriggeredEventListener {
		AtomicInteger count = new AtomicInteger();
		Alert alertEvent = null;
	}
}
