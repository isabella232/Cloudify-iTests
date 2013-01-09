package test.esm.component.rebalancing;

import java.util.concurrent.TimeUnit;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.ProcessingUnit;
import org.testng.annotations.Test;

import com.gigaspaces.cluster.activeelection.SpaceMode;

public class RebalancingRelocatingPrimaryOnSameMachineXenTest extends AbstractRebalancingSlaEnforcementXenTest {

    /**
     *  Before Rebalancing:     
     *  Machine1: GSC1{ P1,P2 } , GSC2{ B2 }  , GSC3{ } , GSC4 { B1 }
     *       *  
     *  After Rebalancing:
     *  Machine1: GSC1{ P1 } , GSC2{ P2 }  , GSC3{ B2 } , GSC4 { B1 }
     * @throws Exception 
     */
    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1")
    public void rebalanceByRelocatingPrimaryOnSingleMachineTest() throws Exception {
        
    	repetitiveAssertNumberOfGSAsAdded(1, OPERATION_TIMEOUT);
    	repetitiveAssertNumberOfGSCsAdded(0, OPERATION_TIMEOUT);
    	
    	GridServiceManager gridServiceManager = admin.getGridServiceManagers().getManagers()[0];
    	GridServiceAgent gsa = startNewByonMachine(getElasticMachineProvisioningCloudifyAdapter(), OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
         
        Machine[] machines = new Machine[] { gsa.getMachine()};
        
        GridServiceContainer[] machine1Containers = loadGSCs(gsa, 4);
        
        ProcessingUnit pu = RebalancingTestUtils.deployProcessingUnitOnSingleMachine(gridServiceManager, ZONE, 2,1);
        
        RebalancingTestUtils.relocatePU(pu, 1, SpaceMode.PRIMARY, machine1Containers[0]);
        RebalancingTestUtils.relocatePU(pu, 2, SpaceMode.PRIMARY, machine1Containers[0]);
        RebalancingTestUtils.relocatePU(pu, 1, SpaceMode.BACKUP, machine1Containers[1]);
        RebalancingTestUtils.relocatePU(pu, 2, SpaceMode.BACKUP,  machine1Containers[3]);
        
        RebalancingTestUtils.insertPersonObjectsIntoSpace(pu, NUMBER_OF_OBJECTS);
        
        RebalancingTestUtils.assertNumberOfRelocations(rebalancing, 0);
        
        RebalancingTestUtils.enforceSlaAndWait(rebalancing, pu, OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
        
        RebalancingTestUtils.assertBalancedDeployment(pu, machines);
        
        RebalancingTestUtils.assertPersonObjectsInSpaceAfterRebalancing(pu, NUMBER_OF_OBJECTS);

        RebalancingTestUtils.assertNumberOfRelocations(rebalancing, 1);
        
        assertUndeployAndWait(pu);
        
    }
    
    
    
}
