package test.esm.component.rebalancing;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.openspaces.admin.AdminFactory;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.internal.InternalAdminFactory;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.grid.gsm.rebalancing.RebalancingSlaEnforcement;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.gigaspaces.cluster.activeelection.SpaceMode;

public class RebalancingByRestartingTwoPrimariesByonTest extends AbstractRebalancingSlaEnforcementByonTest {

	/**
	 *  Before restarting Primary:     
	 *  Machine1: GSC1{ P1,P2} , GSC2 { P3, P4}
	 *  Machine2: GSC3{ B1,B2} , GSC4 { B3, B4}
	 *  
	 *  After restarting Two Primaries concurrently:
	 *  Machine1: GSC1{ B1,P2} , GSC2 { B3, P4}
	 *  Machine2: GSC3{ P1,B2} , GSC4 { P3, B4}
	 *  
	 * @throws InterruptedException 
	 * @throws TimeoutException 
	 */
	@BeforeMethod
	public void beforeTest() {
		super.beforeTestInit();
		rebalancing = new RebalancingSlaEnforcement();
		rebalancing.enableTracing();

	}

	@BeforeClass
	protected void bootstrap() throws Exception {
		super.bootstrapBeforeClass();
	}


	@AfterClass(alwaysRun = true)
	protected void teardownAfterClass() throws Exception {
		super.teardownAfterClass();
	}

	// single threaded admin instead of default admin
	@Override
	protected AdminFactory createAdminFactory() {
		return ((InternalAdminFactory) super.createAdminFactory()).singleThreadedEventListeners();
	}


	@Override
	@AfterMethod
	public void afterTest() {
		try {
			rebalancing.destroy();
		}
		finally {
			super.afterTest();
		}
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "boris1")
	public void rebalanceTest() throws InterruptedException, TimeoutException {

		repetitiveAssertNumberOfGSAsAdded(1, OPERATION_TIMEOUT);
		repetitiveAssertNumberOfGSCsAdded(0, OPERATION_TIMEOUT);

		GridServiceManager gridServiceManager = admin.getGridServiceManagers().getManagers()[0];
		GridServiceAgent[] agents = startNewByonMachines(getElasticMachineProvisioningCloudifyAdapter(), 2, OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);

		Machine[] machines = new Machine[] { agents[0].getMachine(), agents[1].getMachine() };

		GridServiceContainer[] machine1Containers = loadGSCs(agents[0], 2);
		GridServiceContainer[] machine2Containers = loadGSCs(agents[1], 2);

		ProcessingUnit pu = RebalancingTestUtils.deployProcessingUnitOnTwoMachines(gridServiceManager, ZONE, 4,1);

		RebalancingTestUtils.relocatePU(pu, 1, SpaceMode.PRIMARY, machine1Containers[0]);
		RebalancingTestUtils.relocatePU(pu, 2, SpaceMode.PRIMARY, machine1Containers[0]);
		RebalancingTestUtils.relocatePU(pu, 3, SpaceMode.PRIMARY, machine1Containers[1]);
		RebalancingTestUtils.relocatePU(pu, 4, SpaceMode.PRIMARY, machine1Containers[1]);
		RebalancingTestUtils.relocatePU(pu, 1, SpaceMode.BACKUP,  machine2Containers[0]);
		RebalancingTestUtils.relocatePU(pu, 2, SpaceMode.BACKUP,  machine2Containers[0]);
		RebalancingTestUtils.relocatePU(pu, 3, SpaceMode.BACKUP,  machine2Containers[1]);
		RebalancingTestUtils.relocatePU(pu, 4, SpaceMode.BACKUP,  machine2Containers[1]);

		RebalancingTestUtils.insertPersonObjectsIntoSpace(pu, NUMBER_OF_OBJECTS);

		RebalancingTestUtils.assertNumberOfRelocations(rebalancing, 0);

		RebalancingTestUtils.enforceSlaAndWait(rebalancing, pu, OPERATION_TIMEOUT*2, TimeUnit.MILLISECONDS);

		RebalancingTestUtils.assertBalancedDeployment(pu, machines);

		RebalancingTestUtils.assertPersonObjectsInSpaceAfterRebalancing(pu, NUMBER_OF_OBJECTS);

		RebalancingTestUtils.assertNumberOfRelocations(rebalancing, 2);

		assertUndeployAndWait(pu);
	}

}
