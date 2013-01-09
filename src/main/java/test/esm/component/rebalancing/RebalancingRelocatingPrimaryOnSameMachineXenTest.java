package test.gsm.component.rebalancing.xen;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.cloud.xenserver.XenServerMachineProvisioningConfig;
import org.testng.annotations.Test;

import test.gsm.component.rebalancing.RebalancingTestUtils;

import com.gigaspaces.cluster.activeelection.SpaceMode;

public class RebalancingRelocatingPrimaryOnSameMachineXenTest extends AbstractRebalancingSlaEnforcementXenTest {

    /**
     *  Before Rebalancing:     
     *  Machine1: GSC1{ P1,P2 } , GSC2{ B2 }  , GSC3{ } , GSC4 { B1 }
     *       *  
     *  After Rebalancing:
     *  Machine1: GSC1{ P1 } , GSC2{ P2 }  , GSC3{ B2 } , GSC4 { B1 }
     *  
     * @throws InterruptedException 
     * @throws TimeoutException 
     */
    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1")
    public void rebalanceByRelocatingPrimaryOnSingleMachineTest() throws InterruptedException, TimeoutException {
        

        GridServiceAgent gsa = startNewVM(2, 0, OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
        
        Machine[] machines = new Machine[] { gsa.getMachine()};
        
        GridServiceContainer[] machine1Containers = loadGSCs(gsa, 4);
        
        ProcessingUnit pu = RebalancingTestUtils.deployProcessingUnitOnSingleMachine(super.getGridServiceManager(), ZONE, 2,1);
        
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
    
    /**
     *  Before Rebalancing:     
     *  Machine1: GSC1{ P1,P2,P3,P4 } , GSC2{ B5,B6 }  , GSC3{ B7,B8 }
     *  Machine2: GSC4{ P5,P6,P7 }   , GSC5{ P8,B1,B2 }, GSC6{ B3,B4 }
     *  
     *  After Rebalancing:
     *  Machine1: GSC1{ P2,P3,P4 } , GSC2{ B1,B5,B6 }  , GSC3{ B7,B8 }
     *  Machine2: GSC4{ P5,P6,P7 }   , GSC5{ P8,P1,B2 }, GSC6{ B3,B4 }
     *  
     *  After Restart primary:
     *  Machine1: GSC1{ P2,P3,P4 } , GSC2{ B1,P5,B6 }  , GSC3{ B7,B8 }
     *  Machine2: GSC4{ B5,P6,P7 } , GSC5{ P8,P1,B2 }  , GSC6{ B3,B4 }
     *  
     * @throws InterruptedException 
     * @throws TimeoutException 
     */
    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "2")
    public void rebalanceByRelocatingPrimaryOnSameMachineTest() throws InterruptedException, TimeoutException {
      
    	XenServerMachineProvisioningConfig twoCoreMachineConfig = super.cloneMachineProvisioningConfig();
    	twoCoreMachineConfig.setNumberOfCpuCores(2);
    	GridServiceAgent[] agents = startNewVMs(new XenServerMachineProvisioningConfig[]{twoCoreMachineConfig,twoCoreMachineConfig}, OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
    	
        Machine[] machines = new Machine[] { agents[0].getMachine(), agents[1].getMachine() };
        
        GridServiceContainer[] machine1Containers = loadGSCs(agents[0], 3);
        GridServiceContainer[] machine2Containers = loadGSCs(agents[1], 3);
        
        ProcessingUnit pu = RebalancingTestUtils.deployProcessingUnitOnTwoMachines(super.getGridServiceManager(), ZONE, 8,1);
        
        RebalancingTestUtils.relocatePU(pu, 1, SpaceMode.PRIMARY, machine1Containers[0]);
        RebalancingTestUtils.relocatePU(pu, 2, SpaceMode.PRIMARY, machine1Containers[0]);
        RebalancingTestUtils.relocatePU(pu, 3, SpaceMode.PRIMARY, machine1Containers[0]);
        RebalancingTestUtils.relocatePU(pu, 4, SpaceMode.PRIMARY, machine1Containers[0]);
        RebalancingTestUtils.relocatePU(pu, 5, SpaceMode.BACKUP,  machine1Containers[1]);
        RebalancingTestUtils.relocatePU(pu, 6, SpaceMode.BACKUP,  machine1Containers[1]);
        RebalancingTestUtils.relocatePU(pu, 7, SpaceMode.BACKUP,  machine1Containers[2]);
        RebalancingTestUtils.relocatePU(pu, 8, SpaceMode.BACKUP,  machine1Containers[2]);
        RebalancingTestUtils.relocatePU(pu, 5, SpaceMode.PRIMARY, machine2Containers[0]);
        RebalancingTestUtils.relocatePU(pu, 6, SpaceMode.PRIMARY, machine2Containers[0]);
        RebalancingTestUtils.relocatePU(pu, 7, SpaceMode.PRIMARY, machine2Containers[0]);
        RebalancingTestUtils.relocatePU(pu, 8, SpaceMode.PRIMARY, machine2Containers[1]);
        RebalancingTestUtils.relocatePU(pu, 1, SpaceMode.BACKUP,  machine2Containers[1]);
        RebalancingTestUtils.relocatePU(pu, 2, SpaceMode.BACKUP,  machine2Containers[1]);
        RebalancingTestUtils.relocatePU(pu, 3, SpaceMode.BACKUP,  machine2Containers[2]);
        RebalancingTestUtils.relocatePU(pu, 4, SpaceMode.BACKUP,  machine2Containers[2]);
        
        RebalancingTestUtils.insertPersonObjectsIntoSpace(pu, NUMBER_OF_OBJECTS);
        
        RebalancingTestUtils.assertNumberOfRelocations(rebalancing, 0);
        
        RebalancingTestUtils.enforceSlaAndWait(rebalancing, pu, OPERATION_TIMEOUT*2, TimeUnit.MILLISECONDS);
        
        RebalancingTestUtils.assertBalancedDeployment(pu, machines);
        
        RebalancingTestUtils.assertPersonObjectsInSpaceAfterRebalancing(pu, NUMBER_OF_OBJECTS);

        RebalancingTestUtils.assertNumberOfRelocations(rebalancing, 2);

        assertUndeployAndWait(pu);
    }
    
    
}
