package test.servicegrid.failover;

import static test.utils.AdminUtils.loadGSCs;
import static test.utils.AdminUtils.loadGSM;
import static test.utils.DistributionUtils.assertBackups;
import static test.utils.DistributionUtils.assertEvenlyDistributed;
import static test.utils.DistributionUtils.assertEvenlyDistributedPrimaries;
import static test.utils.DistributionUtils.assertPrimaries;
import static test.utils.LogUtils.log;
import static test.utils.ProcessingUnitUtils.getProcessingUnitInstanceName;
import static test.utils.ProcessingUnitUtils.waitForDeploymentStatus;
import static test.utils.ProcessingUnitUtils.waitForManaged;
import static test.utils.SLAUtils.assertMaxPerMachine;
import static test.utils.ToStringUtils.gscToString;
import static test.utils.ToStringUtils.machineToString;

import java.util.concurrent.TimeUnit;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.DeploymentStatus;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.openspaces.admin.space.SpaceDeployment;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.AbstractTest;

import com.gigaspaces.cluster.activeelection.SpaceMode;

/**
 * Topology: machine A - 1 GSM, 2 GSCs
 *           machine B - 1 GSM, 2 GSCs
 * 
 * Tests that a new deployment is managed by backup GSM after machine failover.
 * 
 * 1. load 2 machines, each with 1 GSM and 2 GSCs
 * 2. deploy partitioned X 1,1 max-per-machine 1 via GSM A
 * 3. deploy partitioned Y 1,1 max-per-machine 1 via GSM B
 * 4. verify distribution:
 * 		evenly distributed, evenly distributed primaries, max-per-machine SLA is obeyed
 * 5. kill all on machine A: GSM, 2 GSCs
 * 6. backup GSM on machine B should take over X and Y deployments
 * 7. deployment status is compromised (only primaries on machine B)
 * 8. start machine A: GSM, 2 GSCs
 * 9. deployment status is intact - all backup instances instantiated
 * 10. verify distribution after failure:
 * 		evenly distributed, max-per-machine SLA is obeyed
 * 11. deploy Z 1,1 max-per-machine 1 via GSM B
 * 12. verify distribution after failure:
 * 		evenly distributed, max-per-machine SLA is obeyed
 * 13. kill all on machine B: GSM, 2 GSCs
 * 14. backup GSM on machine B should take over X and Y deployments 
 * 15. deployment status is compromised (only primaries on machine A)
 * 16. start machine B: GSM, 2 GSCs
 * 17. deployment status is intact - all backup instances instantiated
 * 18. verify distribution after failure:
 * 		evenly distributed, max-per-machine SLA is obeyed
 * 
 * @author Moran Avigdor
 */
public class NewDeploymentAfterMachineFailureTest extends AbstractTest {
	
	private Machine machineA;
	private Machine machineB;
	private GridServiceManager gsmA, gsmB;
	private GridServiceContainer[] gscsOnA, gscsOnB;

	@BeforeMethod
	public void setup() {
		assertTrue(admin.getMachines().waitFor(2));
		assertTrue(admin.getGridServiceAgents().waitFor(2));
		
		GridServiceAgent[] agents = admin.getGridServiceAgents().getAgents();
		GridServiceAgent gsaA = agents[0];
		GridServiceAgent gsaB = agents[1];
		
		machineA = gsaA.getMachine();
		machineB = gsaB.getMachine();
		
		log("load 1 GSM and 2 GSCs on " + machineToString(machineA));
		gsmA = loadGSM(machineA);
		gscsOnA = loadGSCs(machineA, 2);
		
		log("load 1 GSM and 2 GSCs on " + machineToString(machineB));
		gsmB = loadGSM(machineB);
		gscsOnB = loadGSCs(machineB, 2);
	}
	
	@Test(timeOut=DEFAULT_TEST_TIMEOUT, groups = "2")
	public void test() {
		log("deploy partitioned X 1,1 max-per-machine=1 via GSM A");
		ProcessingUnit puX = gsmA.deploy(new SpaceDeployment("X").partitioned(1, 1).maxInstancesPerMachine(1));
		puX.waitFor(2);
		
		log("deploy partitioned Y 1,1 max-per-machine=1 via GSM B");
		ProcessingUnit puY = gsmB.deploy(new SpaceDeployment("Y").partitioned(1, 1).maxInstancesPerMachine(1));
		puY.waitFor(2);
		
		assertEvenlyDistributed(admin);
		assertEvenlyDistributedPrimaries(admin);
		assertMaxPerMachine(admin);
		
		log("Kill all on machine A: GSM, 2 GSCs");
		gsmA.kill();
		for (GridServiceContainer gsc : gscsOnA) {
			log("kill " + gscToString(gsc) + " containing: " + getProcessingUnitInstanceName(gsc.getProcessingUnitInstances()));
			gsc.kill();
		}
		
		log("wait for backup GSM on machine B to take over X and Y deployments");
		waitForManaged(puX, gsmB);
		waitForManaged(puY, gsmB);
		
		log("deployment status is compromised (only primaries on machine B)");
		assertEquals(DeploymentStatus.COMPROMISED, waitForDeploymentStatus(puX, DeploymentStatus.COMPROMISED));
		assertEquals(DeploymentStatus.COMPROMISED, waitForDeploymentStatus(puY, DeploymentStatus.COMPROMISED));
		waitForMode(puX, SpaceMode.PRIMARY);
		waitForMode(puY, SpaceMode.PRIMARY);
		assertPrimaries(admin, 2);
		assertBackups(admin, 0);
		
		log("load 1 GSM and 2 GSCs on " + machineToString(machineA));
		gsmA = loadGSM(machineA);
		gscsOnA = loadGSCs(machineA, 2);
		
		log("deployment status is intact (backups on machine A)");
		assertEquals(DeploymentStatus.INTACT, waitForDeploymentStatus(puX, DeploymentStatus.INTACT));
		assertEquals(DeploymentStatus.INTACT, waitForDeploymentStatus(puY, DeploymentStatus.INTACT));
		assertPrimaries(admin, 2);
		assertBackups(admin, 2);
		assertEvenlyDistributed(admin);
		assertMaxPerMachine(admin);
		
		log("deploy partitioned Z 1,1 max-per-machine=1 via GSM B");
		ProcessingUnit puZ = gsmB.deploy(new SpaceDeployment("Z").partitioned(1, 1).maxInstancesPerMachine(1));
		puZ.waitFor(2);
		
		assertEvenlyDistributed(admin);
		assertMaxPerMachine(admin);
		assertPrimaries(admin, 3);
		assertBackups(admin, 3);
		
		log("Kill all on machine B: GSM, 2 GSCs");
		gsmB.kill();
		for (GridServiceContainer gsc : gscsOnB) {
			log("kill " + gscToString(gsc) + " containing: " + getProcessingUnitInstanceName(gsc.getProcessingUnitInstances()));
			gsc.kill();
		}
		
		log("wait for backup GSM on machine A to take over X, Y, Z deployments");
		waitForManaged(puX, gsmA);
		waitForManaged(puY, gsmA);
		waitForManaged(puZ, gsmA);
		
		log("deployment status is compromised (only primaries on machine B)");
		assertEquals(DeploymentStatus.COMPROMISED, waitForDeploymentStatus(puX, DeploymentStatus.COMPROMISED));
		assertEquals(DeploymentStatus.COMPROMISED, waitForDeploymentStatus(puY, DeploymentStatus.COMPROMISED));
		assertEquals(DeploymentStatus.COMPROMISED, waitForDeploymentStatus(puZ, DeploymentStatus.COMPROMISED));
		waitForMode(puX, SpaceMode.PRIMARY);
		waitForMode(puY, SpaceMode.PRIMARY);
		waitForMode(puZ, SpaceMode.PRIMARY);
		assertPrimaries(admin, 3);
		assertBackups(admin, 0);
		
		log("load 1 GSM and 2 GSCs on " + machineToString(machineB));
		gsmB = loadGSM(machineB);
		gscsOnB = loadGSCs(machineB, 2);
		
		log("deployment status is intact (backups on machine A)");
		assertEquals(DeploymentStatus.INTACT, waitForDeploymentStatus(puX, DeploymentStatus.INTACT));
		assertEquals(DeploymentStatus.INTACT, waitForDeploymentStatus(puY, DeploymentStatus.INTACT));
		assertEquals(DeploymentStatus.INTACT, waitForDeploymentStatus(puZ, DeploymentStatus.INTACT));
		assertPrimaries(admin, 3);
		assertBackups(admin, 3);
		assertEvenlyDistributed(admin);
		assertMaxPerMachine(admin);
	}

	private void waitForMode(ProcessingUnit pu, SpaceMode mode) {
		for (ProcessingUnitInstance puInstance : pu) {
            log("waitForMode "+puInstance.getName() +" for mode "+mode.name());
			puInstance.getSpaceInstance().waitForMode(SpaceMode.PRIMARY, 60, TimeUnit.SECONDS);
		}
	}
}
