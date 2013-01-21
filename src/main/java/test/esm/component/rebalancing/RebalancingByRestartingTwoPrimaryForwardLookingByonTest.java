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

public class RebalancingByRestartingTwoPrimaryForwardLookingByonTest extends AbstractRebalancingSlaEnforcementByonTest {

	/**
	 *  Before restarting Primary:     
	 *  Machine1: GSC1 { P1,P2 } [ 8 CPUs ]
	 *  Machine2: GSC2 { B1,P3 } [ 8 CPUs ]
	 *  Machine3: GSC3 { B2,P4 } [ 8 CPUs ]
	 *  Machine4: GSC4 { B3,B4 } [ 8 CPUs ]
	 *  
	 *  After restarting First Primary (still unbalanced):
	 *  Machine1: GSC1 { B1,P2 }
	 *  Machine2: GSC2 { P1,P3 } 
	 *  Machine3: GSC3 { B2,P4 }
	 *  Machine4: GSC4 { B3,B4 }
	 *  
	 *  After restarting Second Primary (balanced):
	 *  Machine1: GSC1 { B1,P2 }
	 *  Machine2: GSC2 { P1,B3 } 
	 *  Machine3: GSC3 { B2,P4 }
	 *  Machine4: GSC4 { P3,B4 }
	 *  
	 *  @throws InterruptedException 
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

	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1")
	public void rebalanceByForwardLookingTwoPrimaryRestartsTest() throws InterruptedException, TimeoutException {

		repetitiveAssertNumberOfGSAsAdded(1, OPERATION_TIMEOUT);
		repetitiveAssertNumberOfGSCsAdded(0, OPERATION_TIMEOUT);

		GridServiceManager gridServiceManager = admin.getGridServiceManagers().getManagers()[0];
		GridServiceAgent[] agents = startNewByonMachines(getElasticMachineProvisioningCloudifyAdapter(), 4, OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);

		GridServiceAgent gsa1 = agents[0];
		GridServiceAgent gsa2 = agents[1];
		GridServiceAgent gsa3 = agents[2];
		GridServiceAgent gsa4 = agents[3];

		Machine[] machines = new Machine[] { gsa1.getMachine(), gsa2.getMachine(), gsa3.getMachine(), gsa4.getMachine() };

		GridServiceContainer[] machine1Containers = loadGSCs(gsa1, 1);
		GridServiceContainer[] machine2Containers = loadGSCs(gsa2, 1);
		GridServiceContainer[] machine3Containers = loadGSCs(gsa3, 1);
		GridServiceContainer[] machine4Containers = loadGSCs(gsa4, 1);

		ProcessingUnit pu = RebalancingTestUtils.deployProcessingUnitOnTwoMachines(gridServiceManager, ZONE, 4,1);

		RebalancingTestUtils.relocatePU(pu, 1, SpaceMode.PRIMARY, machine1Containers[0]);
		RebalancingTestUtils.relocatePU(pu, 2, SpaceMode.PRIMARY, machine1Containers[0]);
		RebalancingTestUtils.relocatePU(pu, 1, SpaceMode.BACKUP,  machine2Containers[0]);
		RebalancingTestUtils.relocatePU(pu, 3, SpaceMode.PRIMARY, machine2Containers[0]);
		RebalancingTestUtils.relocatePU(pu, 2, SpaceMode.BACKUP,  machine3Containers[0]);
		RebalancingTestUtils.relocatePU(pu, 4, SpaceMode.PRIMARY, machine3Containers[0]);
		RebalancingTestUtils.relocatePU(pu, 3, SpaceMode.BACKUP,  machine4Containers[0]);
		RebalancingTestUtils.relocatePU(pu, 4, SpaceMode.BACKUP,  machine4Containers[0]);

		RebalancingTestUtils.insertPersonObjectsIntoSpace(pu, NUMBER_OF_OBJECTS);

		RebalancingTestUtils.enforceSlaAndWait(rebalancing, pu, OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);

		RebalancingTestUtils.assertBalancedDeployment(pu, machines);

		RebalancingTestUtils.assertPersonObjectsInSpaceAfterRebalancing(pu, NUMBER_OF_OBJECTS);

		RebalancingTestUtils.assertNumberOfRelocations(rebalancing, 2);

		assertUndeployAndWait(pu);
	}

}
