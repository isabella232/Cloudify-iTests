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
 * - 2 Machines
 * - 1 GSM and 2 GSCs on Machine A
 * - 2 GSCs on Machine B
 * - Cluster A partitioned 2,1 max-per-vm = 1
 * - Cluster B partitioned 2,1 max-per-vm = 1
 * 
 * Test max-per-vm SLA is obeyed across 2 machines
 * 
 * Start 1 GSM, 2 GSCs on machine A
 * Start 2 GSCs on machine B
 * Deploy Cluster A partitioned 2,1 max-per-vm=1
 * Deploy Cluster B partitioned 2,1 max-per-vm=1
 * 
 * a. evenly distributed across 4 GSCs
 * b. primary on each GSC
 * c. 2 primaries, 2 backups for each cluster
 * 
 * @author Moran Avigdor
 */
public class MaxPerVmSlaTwoMachinesTest extends AbstractTest {
	private Machine machine;
	
	@BeforeMethod
	public void setup() {
		log("waiting for 2 machines");
		admin.getMachines().waitFor(2);

		log("waiting for 2 GSAs");
		admin.getGridServiceAgents().waitFor(2);
		
		GridServiceAgent[] agents = admin.getGridServiceAgents().getAgents();
		GridServiceAgent gsaA = agents[0];
		GridServiceAgent gsaB = agents[1];
		machine = gsaA.getMachine();
		Machine machineB = gsaB.getMachine();
		
		log("starting: 1 GSM and 4 GSCs");
		GridServiceManager gsm = loadGSM(machine); //GSM
		loadGSCs(machine, 2); //2 GSCs on machine A
		loadGSCs(machineB, 2); //2 GSCs on machine B
		
		//Deploy Cluster A to GSM A
		log("deploy cluster A, partitioned 2,1 max-per-vm=1");
		ProcessingUnit puA = gsm.deploy(new SpaceDeployment("A").partitioned(2, 1).maxInstancesPerVM(1));
		assertTrue(puA.waitFor(puA.getTotalNumberOfInstances()));
		
		log("deploy cluster B, partitioned 2,1 max-per-vm=1");
		ProcessingUnit puB = gsm.deploy(new SpaceDeployment("B").partitioned(2, 1).maxInstancesPerVM(1));
		assertTrue(puB.waitFor(puB.getTotalNumberOfInstances()));
	}
	
	@Test(timeOut=DEFAULT_TEST_TIMEOUT, groups = "2")
	public void test() {
		SLAUtils.assertMaxPerVM(admin);
		DistributionUtils.assertEvenlyDistributed(admin);
		DistributionUtils.assertEvenlyDistributedPrimaries(admin);
		DistributionUtils.assertPrimaries(admin, 4);
		DistributionUtils.assertBackups(admin, 4);
	}
}
