package test.esm.component.rebalancing;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.ProcessingUnit;
import org.testng.annotations.Test;

import com.gigaspaces.cluster.activeelection.SpaceMode;

import framework.utils.AdminUtils;

public class RebalancingRestartingPrimaryInstanceXenTest extends AbstractRebalancingSlaEnforcementXenTest {

    /**
     *  Before restarting Primary:     
     *  Machine1: GSC1{ P1,P2} 
     *  Machine2: GSC2{ B1,B2}
     *  
     *  After restarting Primary:
     *  Machine1: GSC1{ B1,P2} 
     *  Machine2: GSC2{ P1,B2}
     *  
     * @throws InterruptedException 
     * @throws TimeoutException 
     */
    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "2")
    public void rebalanceByRestartingPrimaryInstanceTest() throws InterruptedException, TimeoutException {
        
    	repetitiveAssertNumberOfGSAsAdded(1, OPERATION_TIMEOUT);
    	repetitiveAssertNumberOfGSCsAdded(0, OPERATION_TIMEOUT);
    	
    	GridServiceManager gridServiceManager = admin.getGridServiceManagers().getManagers()[0];
    	GridServiceAgent[] agents = startNewByonMachines(getElasticMachineProvisioningCloudifyAdapter(),2, OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
    	
        Machine[] machines = new Machine[] { agents[0].getMachine(), agents[1].getMachine() };
        
        GridServiceContainer[] machine1Containers = AdminUtils.loadGSCs(agents[0], 1, ZONE);
        GridServiceContainer[] machine2Containers = AdminUtils.loadGSCs(agents[1], 1, ZONE);
        
        ProcessingUnit pu = RebalancingTestUtils.deployProcessingUnitOnTwoMachines(gridServiceManager, ZONE, 2,1);
        
        RebalancingTestUtils.relocatePU(pu, 1, SpaceMode.PRIMARY, machine1Containers[0]);
        RebalancingTestUtils.relocatePU(pu, 2, SpaceMode.PRIMARY, machine1Containers[0]);
        RebalancingTestUtils.relocatePU(pu, 1, SpaceMode.BACKUP,  machine2Containers[0]);
        RebalancingTestUtils.relocatePU(pu, 2, SpaceMode.BACKUP,  machine2Containers[0]);
        
        RebalancingTestUtils.insertPersonObjectsIntoSpace(pu, NUMBER_OF_OBJECTS);
        
        RebalancingTestUtils.assertNumberOfRelocations(rebalancing, 0);
        
        RebalancingTestUtils.enforceSlaAndWait(rebalancing, pu, OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
        
        RebalancingTestUtils.assertBalancedDeployment(pu, machines);
        
        RebalancingTestUtils.assertPersonObjectsInSpaceAfterRebalancing(pu, NUMBER_OF_OBJECTS);
        
        RebalancingTestUtils.assertNumberOfRelocations(rebalancing, 1);
        
        assertUndeployAndWait(pu);
    }
    
}
