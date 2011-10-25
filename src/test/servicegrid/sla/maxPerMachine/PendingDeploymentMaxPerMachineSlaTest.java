package test.servicegrid.sla.maxPerMachine;

import static org.testng.Assert.assertFalse;
import static test.utils.AdminUtils.loadGSCs;
import static test.utils.AdminUtils.loadGSM;
import static test.utils.DistributionUtils.assertBackups;
import static test.utils.DistributionUtils.assertPrimaries;
import static test.utils.LogUtils.log;

import java.util.concurrent.TimeUnit;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.DeploymentStatus;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.space.SpaceDeployment;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.AbstractTest;
import test.utils.DistributionUtils;
import test.utils.SLAUtils;

/**
 * GS-7388 - A GSC which momentarily disconnects from a GSM, while provision
 * attempts are made, may cause max-instances-per-machine SLA not to be obeyed.
 * 
 * Tests that max-instances-per-machine SLA is obeyed when pending provision requests
 * are triggered on new registered GSCs.
 * 
 * 1. Load 1 GSM
 * 2. Deploy large cluster (10,1 max-per-machine = 1)
 * 3. all processing units are in pending
 * 4. load 3 GSCs simultaneously on 1 machine
 * 5. verify that 10 instances are deployed, all primaries
 * 
 * @author Moran Avigdor
 */
public class PendingDeploymentMaxPerMachineSlaTest extends AbstractTest {
	
	private Machine machine;
	private GridServiceManager gsm;
	
	@BeforeMethod
	public void setup() {
		
		log("waiting for 1 machine");
		GridServiceAgent gsa = admin.getGridServiceAgents().waitForAtLeastOne();
		this.machine = gsa.getMachine();
		assertNotNull("Machine is null although GSA was discovered!", machine);
		
		log("start 1 GSM");
		gsm = loadGSM(machine);
	}

	@Test(timeOut=DEFAULT_TEST_TIMEOUT, groups = "1")
	public void test() {
		
		log("deploy cluster partitioned 10,1 max-per-machine=1");
		ProcessingUnit pu = gsm.deploy(new SpaceDeployment("data").partitioned(10, 1).maxInstancesPerMachine(1));
		
		log("assert - all processing units are pending");
		assertTrue(DeploymentStatus.BROKEN.equals(pu.getStatus()));
		assertEquals("Expected all processing units to be pending", pu.getInstances().length, 0);

		log("start 3 GSCs");
		loadGSCs(machine, 3);
		
		log("wait for 10 processing units");
		assertTrue("Wait till at least 10 Processing Unit Instances are up", pu.waitFor(10));
		assertTrue(DeploymentStatus.COMPROMISED.equals(pu.getStatus()));
		
		log("assert - only 10 instances are deployed, all primaries");
		assertEquals(10, pu.getInstances().length);
		assertFalse(pu.waitFor(11, 60, TimeUnit.SECONDS), "Extra Processing Unit instance discovered: " + pu.getInstances().length);
		assertPrimaries(admin, 10);
		assertBackups(admin, 0);
		
		log("assert - max-per-machine is obeyed");
		SLAUtils.assertMaxPerMachine(admin);
		DistributionUtils.assertEvenlyDistributed(admin);
	}
}
