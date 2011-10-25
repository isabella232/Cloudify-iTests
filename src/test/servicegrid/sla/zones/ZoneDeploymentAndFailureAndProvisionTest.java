package test.servicegrid.sla.zones;

import static test.utils.AdminUtils.loadGSC;
import static test.utils.AdminUtils.loadGSCs;
import static test.utils.AdminUtils.loadGSMs;
import static test.utils.LogUtils.log;
import static test.utils.ProcessingUnitUtils.waitForDeploymentStatus;
import static test.utils.SLAUtils.assertMaxPerZone;

import java.util.concurrent.CountDownLatch;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.gsc.GridServiceContainers;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.DeploymentStatus;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.openspaces.admin.pu.events.ProcessingUnitInstanceAddedEventListener;
import org.openspaces.admin.pu.events.ProcessingUnitInstanceRemovedEventListener;
import org.openspaces.admin.space.SpaceDeployment;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.AbstractTest;
import test.utils.ToStringUtils;

/**
 * Topology: 1 machine, 2 GSM, 4 GSCs
 * Tests that zone SLA is obeyed during deployment and failover.
 * 
 * 1. start GSM
 * 2. start 2 GSC tagged with zone1
 * 3. start 2 GSC tagged with zone2
 * 4. deploy partitioned 1,1 zone1/1 ,zone2/1
 * 5. deployment status is intact
 * 6. one instance should instantiate on each zone
 * 7. kill each GSC with processing unit instance
 * 8. instances should failover to alternate GSCs of the same zone
 * 9. deployment status is intact
 * 10. one instance should instantiate on each zone
 * 11. kill GSC (zone2),
 * 12. instance on killed GSC should not instantiate
 * 13. deployment status is compromised
 * 14. start GSC tagged with zone2
 * 15. instance should instantiate on GSC zone2
 * 16. deployment status intact
 * 
 * @author Moran Avigdor
 */
public class ZoneDeploymentAndFailureAndProvisionTest extends AbstractTest {
	private Machine machine;
	private GridServiceManager gsm;
	private GridServiceContainer[] zone1;
	private GridServiceContainer[] zone2;

	@BeforeMethod
	public void setup() {
		GridServiceAgent gsa = admin.getGridServiceAgents().waitForAtLeastOne();
		machine = gsa.getMachine();
		
		log("start 2 GSM");
		gsm = loadGSMs(machine, 2)[0];
		
		log("start 2 GSCs tagged with zone1");
		zone1 = loadGSCs(machine, 2, "zone1");
		
		log("start 2 GSC tagged with zone2");
		zone2 = loadGSCs(machine, 2, "zone2");
	}
	
	@Test(timeOut=DEFAULT_TEST_TIMEOUT, groups="1")
	public void test() throws Exception {
		log("deploy partitioned X 1,1 zone1/1 zone2/1");
		ProcessingUnit pu = gsm.deploy(new SpaceDeployment("X").partitioned(1,
				1).addZone("zone1").maxInstancesPerZone("zone1", 1).addZone(
				"zone2").maxInstancesPerZone("zone2", 1));
        
        log("wait for instances");
        pu.waitFor(2);
        
        log("wait for intact deployment status");
        assertEquals(DeploymentStatus.INTACT, waitForDeploymentStatus(pu, DeploymentStatus.INTACT));
        
        assertOneInstancePerZone(pu);
        assertMaxPerZone(admin);
        
        /*
         * kill each GSC with processing unit instance
         * instances should failover to alternate GSCs of the same zone
         */
        final CountDownLatch addedLatch = new CountDownLatch(2);
        for (GridServiceContainer gsc : machine.getGridServiceContainers()) {
        	if (gsc.getProcessingUnitInstances().length != 0)
        		continue;
        	
        	//empty gsc - register for added events
        	gsc.getProcessingUnitInstanceAdded().add(new ProcessingUnitInstanceAddedEventListener() {
    			public void processingUnitInstanceAdded(
    					ProcessingUnitInstance processingUnitInstance) {
    				assertTrue(addedLatch.getCount() != 0);
    				addedLatch.countDown();
    			}
    		});
        }
        
        log("kill each GSC with processing unit instance");
        for (ProcessingUnitInstance puInstance : pu) {
        	GridServiceContainer gsc = puInstance.getGridServiceContainer();
        	log("killing GSC : " + ToStringUtils.gscToString(gsc));
        	gsc.kill();
        }
        
        log("wait for instances to failover to alternate GSCs of the same zone");
        addedLatch.await();
        
        log("wait for intact deployment status");
        assertEquals(DeploymentStatus.INTACT, waitForDeploymentStatus(pu, DeploymentStatus.INTACT));
        
        assertOneInstancePerZone(pu);
        assertMaxPerZone(admin);
        
        final CountDownLatch removedLatch = new CountDownLatch(1);
        pu.getProcessingUnitInstanceRemoved().add(new ProcessingUnitInstanceRemovedEventListener() {
			
			public void processingUnitInstanceRemoved(
					ProcessingUnitInstance processingUnitInstance) {
				assertTrue(removedLatch.getCount() != 0);
				removedLatch.countDown();
			}
		});
        
        log("Kill GSC from zone2");
        killGscZone2();
        
        log("wait for instance to be removed");
        removedLatch.await();
        
        log("wait for compromised deployment status");
        assertEquals(DeploymentStatus.COMPROMISED, waitForDeploymentStatus(pu, DeploymentStatus.COMPROMISED));
        
        log("start new GSC tagged with zone2");
        GridServiceContainer newGscZone2 = loadGSC(machine, "zone2");
        final CountDownLatch latch = new CountDownLatch(1);
        newGscZone2.getProcessingUnitInstanceAdded().add(new ProcessingUnitInstanceAddedEventListener() {
			public void processingUnitInstanceAdded(
					ProcessingUnitInstance processingUnitInstance) {
				assertTrue(latch.getCount() != 0);
				latch.countDown();
			}
		});
        
        log("wait for instance to be instantiated on new GSC/zone2");
        latch.await();
        
        log("wait for intact deployment status");
        assertEquals(DeploymentStatus.INTACT, waitForDeploymentStatus(pu, DeploymentStatus.INTACT));
        
        assertOneInstancePerZone(pu);
        assertMaxPerZone(admin);
	}

	private void killGscZone2() {
		GridServiceContainers gscsInZone2 = admin.getZones().getByName("zone2").getGridServiceContainers();
		assertEquals(1, gscsInZone2.getSize());
		for (GridServiceContainer gsc : gscsInZone2) {
			gsc.kill();
		}
	}

	private void assertOneInstancePerZone(ProcessingUnit pu) {
		assertEquals("one instance per zone", 1, admin.getZones().getByName("zone1").getProcessingUnitInstances().length);
		assertEquals("one instance per zone", 1, admin.getZones().getByName("zone2").getProcessingUnitInstances().length);
	}
}
