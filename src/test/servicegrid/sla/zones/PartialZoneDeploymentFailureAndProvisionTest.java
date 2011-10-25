package test.servicegrid.sla.zones;

import static test.utils.AdminUtils.loadGSC;
import static test.utils.AdminUtils.loadGSCs;
import static test.utils.AdminUtils.loadGSM;
import static test.utils.LogUtils.log;
import static test.utils.ProcessingUnitUtils.waitForDeploymentStatus;
import static test.utils.SLAUtils.assertMaxPerZone;

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
 * Topology: 1 machine, 1 GSM, 3 GSCs
 * Tests that zone SLA is obeyed during deployment.
 * 
 * 1. start GSM
 * 2. start 2 GSCs tagged with zone1
 * 3. start 1 GSC tagged with zone2
 * 4. deploy partitioned 1,1 zone1/1 ,zone3/1
 * 5. only 1 instance deployed on zone1
 * 6. deployment status is compromised
 * 7. start 1 GSC tagged with zone3
 * 8. backup instance is deployed on zone3
 *   
 * @author Moran Avigdor
 */
public class PartialZoneDeploymentFailureAndProvisionTest extends AbstractTest {
	
	private Machine machine;
	private GridServiceManager gsm;

	@BeforeMethod
	public void setup() {
		GridServiceAgent gsa = admin.getGridServiceAgents().waitForAtLeastOne();
		machine = gsa.getMachine();
		
		log("start 1 GSM");
		gsm = loadGSM(machine);
		
		log("start 2 GSCs tagged with zone1");
		loadGSCs(machine, 2, "zone1");
		
		log("start 1 GSC tagged with zone2");
		loadGSC(machine, "zone2");
	}
	
	@Test(timeOut=DEFAULT_TEST_TIMEOUT, groups = "1")
	public void test() throws Exception {
		log("deploy partitioned X 1,1 zone1/1 zone3/1");
		ProcessingUnit pu = gsm.deploy(new SpaceDeployment("X").partitioned(1,
				1).addZone("zone1").maxInstancesPerZone("zone1", 1).addZone(
				"zone3").maxInstancesPerZone("zone3", 1));
        
        log("wait for 1 space instance");
        pu.waitForSpace();
        
        log("wait for compromised deployment status");
        assertEquals(DeploymentStatus.COMPROMISED, waitForDeploymentStatus(pu, DeploymentStatus.COMPROMISED));
        
        for (ProcessingUnitInstance puInstance : pu) {
        	assertTrue("expected zone1", puInstance.getGridServiceContainer().getZones().keySet().contains("zone1"));
        }
        
        log("start 1 GSC tagged with zone3");
        GridServiceContainer gscZone3 = loadGSC(machine, "zone3");
        final CountDownLatch latch = new CountDownLatch(1);
        gscZone3.getProcessingUnitInstanceAdded().add(new ProcessingUnitInstanceAddedEventListener() {
			public void processingUnitInstanceAdded(
					ProcessingUnitInstance processingUnitInstance) {
				assertTrue(latch.getCount() != 0);
				latch.countDown();
			}
		});
        
        log("wait for instance to instantiate on GSC zone3");
        latch.await();
        
        assertMaxPerZone(admin);
	}
}
