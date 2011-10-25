package test.servicegrid.sla.maxPerVm;

import static org.testng.AssertJUnit.assertNull;
import static test.utils.AdminUtils.loadGSC;
import static test.utils.AdminUtils.loadGSMs;
import static test.utils.LogUtils.log;
import static test.utils.ProcessingUnitUtils.waitForDeploymentStatus;
import static test.utils.ProcessingUnitUtils.waitForManaged;
import static test.utils.SLAUtils.assertMaxPerVM;

import java.util.concurrent.TimeUnit;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.DeploymentStatus;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitPartition;
import org.openspaces.admin.space.SpaceDeployment;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.AbstractTest;

import com.gigaspaces.cluster.activeelection.SpaceMode;

/**
 * Topology: 1 machine, 2 GSMs 1 GSC
 * Tests that during deployment, if managing GSM is killed, max-per-vm is obeyed until an available GSC is started.
 * 
 * 1. deploy partition 1,1 max-per-vm = 1
 * 2. verify that deployment status is compromised
 * 3. verify 1 instance (primary space) on available GSC, second instance is pending
 * 4. kill managing GSM
 * 5. verify that backup GSM takes over
 * 6. verify second instance is still pending
 * 7. start additional GSC
 * 8. verify instance is deployed on this GSC
 * 9. verify deployment status in intact
 * 10. verify total instances is 2
 * 
 * @author Moran Avigdor
 */
public class GsmGscDeploymentFailureMaxPerVmSlaTest extends AbstractTest {
	private Machine machine;
	private GridServiceManager[] gsms;
	private GridServiceContainer gsc1;
	
	@BeforeMethod
	public void setup() {
		GridServiceAgent gsa = admin.getGridServiceAgents().waitForAtLeastOne();
		machine = gsa.getMachine();
		
		log("loading 2 GSM, 1 GSC on " + machine.getHostName());
		gsms = loadGSMs(machine, 2);
		gsc1 = loadGSC(machine);
	}
	
	@Test(timeOut=DEFAULT_TEST_TIMEOUT, groups = "1")
	public void test() throws InterruptedException {
		
		log("Deploying cluster A partitioned 1,1 max-per-vm=1");
		ProcessingUnit pu = gsms[0].deploy(new SpaceDeployment("A").partitioned(1, 1).maxInstancesPerVM(1));
		assertEquals("unexpected deployment status", DeploymentStatus.COMPROMISED, waitForDeploymentStatus(pu, DeploymentStatus.COMPROMISED));
		pu.waitFor(1);
		assertEquals("Expected GSC to contain 1 instance", 1, gsc1.getProcessingUnitInstances().length);
		assertOnlyPrimaryIsDeployed();
		
		log("killing managing GSM #1");
		assertEquals("expected GSM to manage pu A", gsms[0], pu.getManagingGridServiceManager());
		gsms[0].kill();
		
		log("waiting for backup GSM to manage processing unit");
		assertEquals("expected GSM to manage pu A", gsms[1], waitForManaged(pu, gsms[1]));
		assertEquals("second instance should still be pending due to max-per-vm", 1, gsc1.getProcessingUnitInstances().length);
		
		log("start additional GSC");
		GridServiceContainer gsc2 = loadGSC(machine);
		pu.waitFor(2);
		log("wait for intact deployment status");
		assertEquals("unexpected deployment status", DeploymentStatus.INTACT, waitForDeploymentStatus(pu, DeploymentStatus.INTACT));
		
		assertEquals("Expected only one instance", 1, gsc1.getProcessingUnitInstances().length);
		assertEquals("Expected only one instance", 1, gsc2.getProcessingUnitInstances().length);
		assertMaxPerVM(admin);
	}
	
	private void assertOnlyPrimaryIsDeployed() {
		assertTrue(admin.getProcessingUnits().getSize() > 0);
		for (ProcessingUnit pu : admin.getProcessingUnits()) {
			assertTrue(pu.getPartitions().length > 0);
			for (ProcessingUnitPartition puPartition : pu.getPartitions()) {
				assertEquals(1, puPartition.getInstances().length);
				pu.waitForSpace().getInstances()[0].waitForMode(SpaceMode.PRIMARY, 10, TimeUnit.SECONDS);
				assertNotNull(puPartition.getPrimary());
				assertNull(puPartition.getBackup());
			}
		}
	}
}
