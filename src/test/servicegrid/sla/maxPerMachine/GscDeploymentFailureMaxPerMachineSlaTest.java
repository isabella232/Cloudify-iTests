package test.servicegrid.sla.maxPerMachine;

import static org.testng.AssertJUnit.assertNull;
import static test.utils.AdminUtils.loadGSC;
import static test.utils.AdminUtils.loadGSCs;
import static test.utils.AdminUtils.loadGSM;
import static test.utils.LogUtils.log;
import static test.utils.ProcessingUnitUtils.waitForDeploymentStatus;
import static test.utils.SLAUtils.assertMaxPerMachine;

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
 * Topology: 2 machine, 1 GSM 2 GSC on machine 1 
 * 
 * Tests that during deployment, max-per-machine is obeyed until an available GSC is started on second
 * machine.
 * 
 * 1. deploy partition 1,1 max-per-machine = 1 
 * 2. verify that deployment status is compromised 
 * 3. verify 1 instance (primary space) on available GSC, second instance is pending 
 * 4. start additional GSC on second machine
 * 5. verify instance is deployed on this GSC 
 * 6. verify deployment status in intact 
 * 7. verify total instances is 2
 * 
 * @author Moran Avigdor
 */
public class GscDeploymentFailureMaxPerMachineSlaTest extends AbstractTest {
	private Machine machineA, machineB;
	private GridServiceManager gsm;
	
	@BeforeMethod
	public void setup() {
		assertTrue(admin.getMachines().waitFor(2));
		assertTrue(admin.getGridServiceAgents().waitFor(2));
		
		GridServiceAgent[] agents = admin.getGridServiceAgents().getAgents();
		GridServiceAgent gsaA = agents[0];
		GridServiceAgent gsaB = agents[1];
		
		machineA = gsaA.getMachine();
		machineB = gsaB.getMachine();
		
		log("loading 1 GSM, 1 GSC on " + machineA.getHostName());
		gsm = loadGSM(machineA);
		loadGSCs(machineA, 2);
	}
	
	@Test(timeOut=DEFAULT_TEST_TIMEOUT, groups = "2")
	public void test() throws InterruptedException {
		
		log("Deploying cluster A partitioned 1,1 max-per-machine=1");
		ProcessingUnit pu = gsm.deploy(new SpaceDeployment("A").partitioned(1, 1).maxInstancesPerMachine(1));
		assertEquals("unexpected deployment status", DeploymentStatus.COMPROMISED, waitForDeploymentStatus(pu, DeploymentStatus.COMPROMISED));
		pu.waitFor(1);
		assertEquals("Expected to contain 1 instance", 1, machineA.getProcessingUnitInstances().length);
		assertOnlyPrimaryIsDeployed();
		
		log("start additional GSC on " + machineB.getHostName());
		GridServiceContainer gscOnB = loadGSC(machineB);
		pu.waitFor(2);
		log("wait for intact deployment status");
		assertEquals("unexpected deployment status", DeploymentStatus.INTACT, waitForDeploymentStatus(pu, DeploymentStatus.INTACT));
		
		assertEquals("Expected only one instance", 1, machineA.getProcessingUnitInstances().length);
		assertEquals("Expected only one instance", 1, machineB.getProcessingUnitInstances().length);
		assertEquals("Expected only one instance", 1, gscOnB.getProcessingUnitInstances().length);
		assertMaxPerMachine(admin);
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
