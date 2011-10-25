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

public class RebalancingByRestartingTwoPrimaryForwardLookingXenTest extends AbstractRebalancingSlaEnforcementXenTest {
    
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
     */
    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1")
    public void rebalanceByForwardLookingTwoPrimaryRestartsTest() throws InterruptedException {
        
        GridServiceAgent gsa = startNewVM(8, 0, OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
        GridServiceAgent gsa2 = startNewVM(8, 0, OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
        GridServiceAgent gsa3 = startNewVM(8, 0, OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
        GridServiceAgent gsa4 = startNewVM(8, 0, OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
        
        Machine[] machines = new Machine[] { gsa.getMachine(), gsa2.getMachine(), gsa3.getMachine(), gsa4.getMachine() };
        
        GridServiceContainer[] machine1Containers = AdminUtils.loadGSCs(gsa, 1, ZONE);
        GridServiceContainer[] machine2Containers = AdminUtils.loadGSCs(gsa2, 1, ZONE);
        GridServiceContainer[] machine3Containers = AdminUtils.loadGSCs(gsa3, 1, ZONE);
        GridServiceContainer[] machine4Containers = AdminUtils.loadGSCs(gsa4, 1, ZONE);
        
        ProcessingUnit pu = RebalancingTestUtils.deployProcessingUnitOnTwoMachines(gsm, ZONE, 4,1);
        
        RebalancingTestUtils.relocatePU(pu, 1, SpaceMode.PRIMARY, machine1Containers[0]);
        RebalancingTestUtils.relocatePU(pu, 2, SpaceMode.PRIMARY, machine1Containers[0]);
        RebalancingTestUtils.relocatePU(pu, 1, SpaceMode.BACKUP,  machine2Containers[0]);
        RebalancingTestUtils.relocatePU(pu, 3, SpaceMode.PRIMARY, machine2Containers[0]);
        RebalancingTestUtils.relocatePU(pu, 2, SpaceMode.BACKUP,  machine3Containers[0]);
        RebalancingTestUtils.relocatePU(pu, 4, SpaceMode.PRIMARY, machine3Containers[0]);
        RebalancingTestUtils.relocatePU(pu, 3, SpaceMode.BACKUP,  machine4Containers[0]);
        RebalancingTestUtils.relocatePU(pu, 4, SpaceMode.BACKUP,  machine4Containers[0]);
        
        RebalancingTestUtils.insertPersonObjectsIntoSpace(pu, NUMBER_OF_OBJECTS);
        
        RebalancingTestUtils.enforceSlaAndWait(rebalancing, pu);
        
        RebalancingTestUtils.assertBalancedDeployment(pu, machines);
        
        RebalancingTestUtils.assertPersonObjectsInSpaceAfterRebalancing(pu, NUMBER_OF_OBJECTS);
        
        RebalancingTestUtils.assertNumberOfRelocations(rebalancing, 2);
        
    }
    
    /**
     *  Before restarting Primary:     
     *  Machine1: GSC1 { P1,P2 } [ 8 CPUs ]
     *  Machine2: GSC2 { B1,P3 } [ 8 CPUs ]
     *  Machine3: GSC3 { B2,P4 } [ 2 CPUs ]
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
     */
    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1")
    public void rebalanceByForwardLookingTwoPrimaryRestartsOnMixedTopologyTest() throws InterruptedException {
        
        GridServiceAgent gsa = startNewVM(8, 0, OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
        GridServiceAgent gsa2 = startNewVM(8, 0, OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
        GridServiceAgent gsa3 = startNewVM(2, 0, OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
        GridServiceAgent gsa4 = startNewVM(8, 0, OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
        
        Machine[] machines = new Machine[] { gsa.getMachine(), gsa2.getMachine(), gsa3.getMachine(), gsa4.getMachine() };
        
        GridServiceContainer[] machine1Containers = AdminUtils.loadGSCs(gsa, 1, ZONE);
        GridServiceContainer[] machine2Containers = AdminUtils.loadGSCs(gsa2, 1, ZONE);
        GridServiceContainer[] machine3Containers = AdminUtils.loadGSCs(gsa3, 1, ZONE);
        GridServiceContainer[] machine4Containers = AdminUtils.loadGSCs(gsa4, 1, ZONE);
        
        ProcessingUnit pu = RebalancingTestUtils.deployProcessingUnitOnTwoMachines(gsm, ZONE, 4,1);
        
        RebalancingTestUtils.relocatePU(pu, 1, SpaceMode.PRIMARY, machine1Containers[0]);
        RebalancingTestUtils.relocatePU(pu, 2, SpaceMode.PRIMARY, machine1Containers[0]);
        RebalancingTestUtils.relocatePU(pu, 1, SpaceMode.BACKUP,  machine2Containers[0]);
        RebalancingTestUtils.relocatePU(pu, 3, SpaceMode.PRIMARY, machine2Containers[0]);
        RebalancingTestUtils.relocatePU(pu, 2, SpaceMode.BACKUP,  machine3Containers[0]);
        RebalancingTestUtils.relocatePU(pu, 4, SpaceMode.PRIMARY, machine3Containers[0]);
        RebalancingTestUtils.relocatePU(pu, 3, SpaceMode.BACKUP,  machine4Containers[0]);
        RebalancingTestUtils.relocatePU(pu, 4, SpaceMode.BACKUP,  machine4Containers[0]);
        
        RebalancingTestUtils.insertPersonObjectsIntoSpace(pu, NUMBER_OF_OBJECTS);
        
        RebalancingTestUtils.enforceSlaAndWait(rebalancing, pu);
        
        RebalancingTestUtils.assertBalancedDeployment(pu, machines);
        
        RebalancingTestUtils.assertPersonObjectsInSpaceAfterRebalancing(pu, NUMBER_OF_OBJECTS);
        
        RebalancingTestUtils.assertNumberOfRelocations(rebalancing, 2);
        
    }
    
}
