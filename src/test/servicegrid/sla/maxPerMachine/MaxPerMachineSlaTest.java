package test.servicegrid.sla.maxPerMachine;

import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNull;
import static test.utils.AdminUtils.loadGSC;
import static test.utils.AdminUtils.loadGSM;
import static test.utils.LogUtils.log;

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
import test.utils.AssertUtils.RepetitiveConditionProvider;
import test.utils.DistributionUtils;
import test.utils.SLAUtils;

import com.gigaspaces.cluster.activeelection.SpaceMode;

/**
 * Topology:
 * - 2 machines
 * - 1 GSM and 1 GSC at Machine A
 * - 1 GSC at Machine B
 * - Cluster partitioned 1,1 max-per-machine = 1
 * 
 * Test max-per-machine SLA is obeyed
 * 
 * Start 1 GSM, 1 GSC
 * Deploy partitioned 2,1 max-per-machine=1
 * 
 * a. only primary is deployed, backups pending
 * b. start 1 GSC on machine A, backup is still pending
 * c. start 1 GSC on machine B,
 * c. backup is deployed 
 * 
 * @author Moran Avigdor
 */
public class MaxPerMachineSlaTest extends AbstractTest {
	private Machine machineA, machineB;
	
	@BeforeMethod
	public void setup() {
		log("waiting for 2 machine");
		admin.getMachines().waitFor(2);

		log("waiting for 2 GSAs");
		admin.getGridServiceAgents().waitFor(2);
		
		GridServiceAgent[] agents = admin.getGridServiceAgents().getAgents();
		GridServiceAgent gsaA = agents[0];
		GridServiceAgent gsaB = agents[1];
		
		machineA = gsaA.getMachine();
		machineB = gsaB.getMachine();
		
		log("starting: 1 GSM and 1 GSC");
		GridServiceManager gsm = loadGSM(machineA); //GSM
		loadGSC(machineA); //GSC A1
		
		//Deploy Cluster A to GSM A
		log("deploy cluster A, partitioned 1,1 max-per-machine=1");
		final ProcessingUnit pu = gsm.deploy(new SpaceDeployment("A").partitioned(1, 1).maxInstancesPerMachine(1));
		assertTrue(pu.waitFor(1));
		
		repetitiveAssertTrue("Expected deplyoment status COMPROMISED but was: " + pu.getStatus(), 
		new RepetitiveConditionProvider() {
            public boolean getCondition() {
                return DeploymentStatus.COMPROMISED == pu.getStatus();
            }
        }, OPERATION_TIMEOUT);
	}
	
	@Test(timeOut=DEFAULT_TEST_TIMEOUT, groups = "2")
	public void test() {
		onlyPrimaryIsDeployed();
		
		startGscOnMachineA();
		onlyPrimaryIsDeployed();
		
		startGscOnMachineB();
		obeysSla();
	}
	
	private void onlyPrimaryIsDeployed() {
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
	
	private void startGscOnMachineA() {
		GridServiceContainer gscA2 = loadGSC(machineA);
		assertFalse(gscA2.waitFor(1, 10, TimeUnit.SECONDS));
		assertEquals(0, gscA2.getProcessingUnitInstances().length);
	}
	
	private void startGscOnMachineB() {
		GridServiceContainer gscB1 = loadGSC(machineB);
		assertTrue(gscB1.waitFor(1));
	}
	
	private void obeysSla() {
		SLAUtils.assertMaxPerMachine(admin);
		DistributionUtils.assertEvenlyDistributed(admin);
		DistributionUtils.assertEvenlyDistributedPrimaries(admin);
		DistributionUtils.assertPrimaries(admin, 1);
		DistributionUtils.assertBackups(admin, 1);
	}
}
