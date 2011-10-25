package test.servicegrid.sla.maxPerVm;

import static test.utils.AdminUtils.loadGSCs;
import static test.utils.AdminUtils.loadGSM;
import static test.utils.LogUtils.log;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.space.SpaceDeployment;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.AbstractTest;
import test.utils.DistributionUtils;
import test.utils.SLAUtils;

/**
 * Topology:
 * - 1 GSM and 2 GSCs at 1 Machine
 * - Cluster partitioned 2,1 max-per-vm = 1
 * 
 * Test max-per-vm SLA is obeyed on single machine
 * 
 * Start 1 GSM, 2 GSCs
 * Deploy partitioned 2,1 max-per-vm=1
 * 
 * a. evenly distributed across 2 GSCs
 * b. primary on each GSC
 * c. 2 primaries, 2 backups
 * 
 * @author Moran Avigdor
 */
public class MaxPerVmSlaOneMachineTest extends AbstractTest {
	private Machine machine;
	
	@BeforeMethod
	public void setup() {
		//1 GSM and 2 GSC at 1 machine
		log("waiting for 1 machine");
		admin.getMachines().waitFor(1);

		log("waiting for 1 GSA on discovered machine");
		admin.getGridServiceAgents().waitForAtLeastOne();
		
		GridServiceAgent[] agents = admin.getGridServiceAgents().getAgents();
		GridServiceAgent gsa = agents[0];
		machine = gsa.getMachine();
		
		log("starting: 1 GSM and 2 GSCs");
		GridServiceManager gsm = loadGSM(machine); //GSM
		loadGSCs(machine, 2); //2 GSCs
		
		//Deploy Cluster A to GSM A
		log("deploy cluster A, partitioned 2,1 max-per-vm=1");
		ProcessingUnit pu = gsm.deploy(new SpaceDeployment("A").partitioned(2, 1).maxInstancesPerVM(1));
		assertTrue(pu.waitFor(pu.getTotalNumberOfInstances()));
	}
	
	@Test(timeOut=DEFAULT_TEST_TIMEOUT, groups = "1")
	public void test() {
		SLAUtils.assertMaxPerVM(admin);
		DistributionUtils.assertEvenlyDistributed(admin);
		DistributionUtils.assertEvenlyDistributedPrimaries(admin);
		DistributionUtils.assertPrimaries(admin, 2);
		DistributionUtils.assertBackups(admin, 2);
	}
}
