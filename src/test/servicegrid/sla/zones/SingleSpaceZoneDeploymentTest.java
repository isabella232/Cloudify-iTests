package test.servicegrid.sla.zones;

import static test.utils.AdminUtils.loadGSC;
import static test.utils.AdminUtils.loadGSM;
import static test.utils.LogUtils.log;
import static test.utils.ProcessingUnitUtils.waitForDeploymentStatus;

import java.util.concurrent.CountDownLatch;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.DeploymentStatus;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.openspaces.admin.pu.events.ProcessingUnitInstanceAddedEventListener;
import org.openspaces.admin.space.SpaceDeployment;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.AbstractTest;

/**
 * Topology: 1 machine, 1 GSM, 1 GSC zone1, 1 GSC zone2
 * Tests that zone SLA is obeyed during deployment for a single space (non-partitioned)
 * 
 * 1. load GSM
 *    load 1 GSC zone2
 * 2. deploy single space into zone1/1
 * 3. deployment should be broken, no instance is instantiated
 * 4. load 1 GSC zone1
 * 5. instance should instantiate on zone1
 * 6. zero instances on zone2
 * 
 * @author Moran Avigdor
 */
public class SingleSpaceZoneDeploymentTest extends AbstractTest {
	private Machine machine;
	private GridServiceManager gsm;
	private GridServiceContainer zone1;
	private GridServiceContainer zone2;

	@BeforeMethod
	public void setup() {
		GridServiceAgent gsa = admin.getGridServiceAgents().waitForAtLeastOne();
		machine = gsa.getMachine();
		
		gsm = loadGSM(machine);
		zone2 = loadGSC(machine, "zone2");
	}
	
	@Test(timeOut=DEFAULT_TEST_TIMEOUT)
	public void test() throws Exception {
		log("deploy single space zone1/1");
		ProcessingUnit pu = gsm.deploy(new SpaceDeployment("X").addZone("zone1").maxInstancesPerZone("zone1", 1));
		
		log("wait for broken deployment status");
		assertEquals("deployment should fail", DeploymentStatus.BROKEN, waitForDeploymentStatus(pu, DeploymentStatus.BROKEN));
		assertEquals("no instance should instantiate", 0, machine.getProcessingUnitInstances().length);
		
		zone1 = loadGSC(machine, "zone1");
		final CountDownLatch addedLatch = new CountDownLatch(1);
		zone1.getProcessingUnitInstanceAdded().add(new ProcessingUnitInstanceAddedEventListener() {
			public void processingUnitInstanceAdded(
					ProcessingUnitInstance processingUnitInstance) {
				assertTrue(addedLatch.getCount() != 0);
				addedLatch.countDown();
			}
		});
		
		log("wait for instance to instantiate on zone1");
		addedLatch.await();
		
		for (ProcessingUnitInstance puInstance : pu) {
			assertTrue("expected zone1", puInstance.getGridServiceContainer().getZones().containsKey("zone1"));
		}
		
		assertEquals("expected 0 instances on this zone", 1, zone1.getProcessingUnitInstances().length);
		assertEquals("expected 0 instances on this zone", 0, zone2.getProcessingUnitInstances().length);
	}
}
