package test.gsm.component.rebalancing.xen;

import java.util.concurrent.TimeUnit;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.ProcessingUnit;
import org.testng.annotations.Test;

import test.gsm.component.rebalancing.RebalancingTestUtils;
import test.utils.AdminUtils;

import com.gigaspaces.cluster.activeelection.SpaceMode;

public class RebalancingByRelocatingBackupThenPrimaryForwardLookingXenTest extends AbstractRebalancingSlaEnforcementXenTest {
    
    /**
     *  Before restarting Primary:     
     *  Machine1: GSC1{ P1,P4 } , GSC2 { B8 } , GSC3 { P6 }, GSC4 { B5 }, GSC5 { B2 } 
     *  Machine2: GSC1{ P2 } , GSC2 { B3 } , GSC3 { P7 }, GSC4 { B4 }, GSC5 { Empty }, GSC6 { B1 } 
     *  Machine3: GSC1{ P3 } , GSC2 { P8 } , GSC3 { B7 }, GSC4 { P5 }, GSC5 { B6 } 
     *  
     *  After restarting Two Primaries concurrently:
     *  Assert deployment is balanced (Machine1, GSC1 has 1 instance)(Machine2, GSC5 has 1 instance)
     *  
     * @throws InterruptedException 
     */
    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "3")
    public void rebalancingTest() throws InterruptedException {
        
        GridServiceAgent gsa = startNewVM(2, 0, OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
        GridServiceAgent gsa2 = startNewVM(2, 0, OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
        GridServiceAgent gsa3 = startNewVM(2, 0, OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
        
        Machine[] machines = new Machine[] { 
                gsa.getMachine(), 
                gsa2.getMachine(), 
                gsa3.getMachine()
        };
        
        GridServiceContainer[] machine1Containers = AdminUtils.loadGSCs(gsa, 5, ZONE);
        GridServiceContainer[] machine2Containers = AdminUtils.loadGSCs(gsa2, 6, ZONE);
        GridServiceContainer[] machine3Containers = AdminUtils.loadGSCs(gsa3, 5, ZONE);
        
        ProcessingUnit pu = RebalancingTestUtils.deployProcessingUnitOnTwoMachines(gsm, ZONE, 8,1);
        
        RebalancingTestUtils.relocatePU(pu, 1, SpaceMode.PRIMARY, machine1Containers[0]);
        RebalancingTestUtils.relocatePU(pu, 4, SpaceMode.PRIMARY, machine1Containers[0]);
        RebalancingTestUtils.relocatePU(pu, 8, SpaceMode.BACKUP,  machine1Containers[1]);
        RebalancingTestUtils.relocatePU(pu, 6, SpaceMode.PRIMARY, machine1Containers[2]);
        RebalancingTestUtils.relocatePU(pu, 5, SpaceMode.BACKUP,  machine1Containers[3]);
        RebalancingTestUtils.relocatePU(pu, 2, SpaceMode.BACKUP,  machine1Containers[4]);
        
        RebalancingTestUtils.relocatePU(pu, 2, SpaceMode.PRIMARY, machine2Containers[0]);
        RebalancingTestUtils.relocatePU(pu, 3, SpaceMode.BACKUP,  machine2Containers[1]);
        RebalancingTestUtils.relocatePU(pu, 7, SpaceMode.PRIMARY, machine2Containers[2]);
        RebalancingTestUtils.relocatePU(pu, 4, SpaceMode.BACKUP,  machine2Containers[3]);
        RebalancingTestUtils.relocatePU(pu, 1, SpaceMode.BACKUP,  machine2Containers[5]);

        RebalancingTestUtils.relocatePU(pu, 3, SpaceMode.PRIMARY, machine3Containers[0]);
        RebalancingTestUtils.relocatePU(pu, 8, SpaceMode.PRIMARY, machine3Containers[1]);
        RebalancingTestUtils.relocatePU(pu, 7, SpaceMode.BACKUP,  machine3Containers[2]);
        RebalancingTestUtils.relocatePU(pu, 5, SpaceMode.PRIMARY, machine3Containers[3]);
        RebalancingTestUtils.relocatePU(pu, 6, SpaceMode.BACKUP,  machine3Containers[4]);
        
        RebalancingTestUtils.insertPersonObjectsIntoSpace(pu, NUMBER_OF_OBJECTS);
        RebalancingTestUtils.assertNumberOfRelocations(rebalancing, 0);
        RebalancingTestUtils.enforceSlaAndWait(rebalancing, pu);
        RebalancingTestUtils.assertBalancedDeployment(pu, machines);
        RebalancingTestUtils.assertPersonObjectsInSpaceAfterRebalancing(pu, NUMBER_OF_OBJECTS);
        RebalancingTestUtils.assertNumberOfRelocations(rebalancing, 2);
    }
    
}
