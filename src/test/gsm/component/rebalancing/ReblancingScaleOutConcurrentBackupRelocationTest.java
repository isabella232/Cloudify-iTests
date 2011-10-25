package test.gsm.component.rebalancing;

import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.ProcessingUnit;
import org.testng.annotations.Test;

import test.utils.AdminUtils;

public class ReblancingScaleOutConcurrentBackupRelocationTest extends AbstractRebalancingSlaEnforcementTest {

    /**
     *  Before Rebalancing:
     *  GSC1{ P1 B2 } , GSC2{ B3 P2 } , GSC3 { B1 P3 } 
     *  
     *  After Rebalancing:
     *  GSC1{ P1 } , GSC2{ P2 }  , GSC3{ P3 }, GSC4 { B2 } , GSC5 { B3 } , GSC6 { B1 } 
     */

    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1")
    public void scaleOutRebalancingConcurrentBackupRelocationTest() throws InterruptedException {
    
        Machine[] machines = new Machine[] { gsa.getMachine()};
        
        //deploy pu partitioned(3,1) on 3 GSCs
        AdminUtils.loadGSCs(gsa, 3, ZONE);
        admin.getGridServiceContainers().waitFor(3);
        
        ProcessingUnit pu = RebalancingTestUtils.deployProcessingUnitOnSingleMachine(gsm , ZONE, 3,1);
      
        RebalancingTestUtils.insertPersonObjectsIntoSpace(pu, NUMBER_OF_OBJECTS);
        
        RebalancingTestUtils.enforceSlaAndWait(rebalancing, pu);
        
        RebalancingTestUtils.assertBalancedDeployment(pu, machines);

        RebalancingTestUtils.assertNumberOfRelocations(rebalancing, 0);
        
        // scale out to 6 GSCs
        AdminUtils.loadGSCs(gsa, 3, ZONE);
        admin.getGridServiceContainers().waitFor(6);
        
        RebalancingTestUtils.enforceSlaAndWait(rebalancing, pu);
        
        RebalancingTestUtils.assertPersonObjectsInSpaceAfterRebalancing(pu, NUMBER_OF_OBJECTS);
        
        RebalancingTestUtils.assertBalancedDeployment(pu, machines);
        
        RebalancingTestUtils.assertNumberOfRelocations(rebalancing, 3);
    }
    
}
