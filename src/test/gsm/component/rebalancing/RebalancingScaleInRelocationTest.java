package test.gsm.component.rebalancing;

import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.ProcessingUnit;
import org.testng.Assert;
import org.testng.annotations.Test;

import test.utils.AdminUtils;

import com.gigaspaces.cluster.activeelection.SpaceMode;

public class RebalancingScaleInRelocationTest extends AbstractRebalancingSlaEnforcementTest {

    /**
     *  Before Rebalancing:
     *  GSC1{ P1 } , GSC2{ B1 }  , GSC3{ P2 }, GSC4 { B2 }
     *  
     *  After Rebalancing:
     *  GSC1{ } , GSC2{ B1 P2 }  , GSC3{ }, GSC4 { B2 P1 }
     */
    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1")
    public void scaleInRebalancingPrimaryRelocationTest() throws InterruptedException {
        
         ProcessingUnit pu = RebalancingTestUtils.doNothingTest(rebalancing, gsa, gsm, ZONE, NUMBER_OF_OBJECTS);
        
        // scale in to select 2 containers
        //choose the grid service containers of backups, meaning two primaries will have to relocate (concurrently).
        GridServiceContainer[] selectedContainers = new GridServiceContainer[2];
        selectedContainers[0] = RebalancingTestUtils.findProcessingUnitInstanceByInstanceIdAndByMode(pu, 1, SpaceMode.BACKUP).getGridServiceContainer();
        selectedContainers[1] = RebalancingTestUtils.findProcessingUnitInstanceByInstanceIdAndByMode(pu, 2, SpaceMode.BACKUP).getGridServiceContainer();
        
        endScaleInRebalancingTest(pu, selectedContainers);
    }
    
    /**
     *  Before Rebalancing:
     *  GSC1{ P1 } , GSC2{ B1 }  , GSC3{ P2 }, GSC4 { B2 }
     *  
     *  After Rebalancing:
     *  GSC1{ P1 B2 } , GSC2{ }  , GSC3{ P2 B1 }, GSC4 { }
     *  
     */
    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1")
    public void scaleInRebalancingBackupRelocationTest() throws InterruptedException {
        
        ProcessingUnit pu = RebalancingTestUtils.doNothingTest(rebalancing, gsa, gsm, ZONE, NUMBER_OF_OBJECTS);
        
        // scale in to select 2 containers
        //choose the grid service containers of primaries, meaning two backups will have to relocate (concurrently).
        GridServiceContainer[] selectedContainers = new GridServiceContainer[2];
        selectedContainers[0] = RebalancingTestUtils.findProcessingUnitInstanceByInstanceIdAndByMode(pu, 1, SpaceMode.PRIMARY).getGridServiceContainer();
        selectedContainers[1] = RebalancingTestUtils.findProcessingUnitInstanceByInstanceIdAndByMode(pu, 2, SpaceMode.PRIMARY).getGridServiceContainer();
        
        endScaleInRebalancingTest(pu, selectedContainers);
    }
    
    /**
     *  Before Rebalancing:
     *  GSC1{ P1 } , GSC2{ B1 }  , GSC3{ P2 }, GSC4 { B2 }
     *  
     *  After Rebalancing:
     *  GSC1{ P1 B2 } , GSC2{ B1 P2 }  , GSC3{ }, GSC4 { }
     *  
     */
    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1")
    public void scaleInRebalancingBothBackupAndPrimaryRelocationTest() throws InterruptedException {

        ProcessingUnit pu = RebalancingTestUtils.doNothingTest(rebalancing, gsa, gsm, ZONE, NUMBER_OF_OBJECTS);
        
        // scale in to select 2 containers
        //choose the grid service containers of partition 1, meaning partition 2 will have to relocate (one at a time)
        GridServiceContainer[] selectedContainers = new GridServiceContainer[2];
        selectedContainers[0] = RebalancingTestUtils.findProcessingUnitInstanceByInstanceIdAndByMode(pu, 1, SpaceMode.PRIMARY).getGridServiceContainer();
        selectedContainers[1] = RebalancingTestUtils.findProcessingUnitInstanceByInstanceIdAndByMode(pu, 1, SpaceMode.BACKUP).getGridServiceContainer();
        
        endScaleInRebalancingTest(pu, selectedContainers);
    }
    
    /**
     *  Before Rebalancing:     
     *  Machine1: GSC1{ P1,P2 } , GSC2{ B2 }  , GSC3{ } , GSC4 { B1 }
     *       *  
     *  After Rebalancing:
     *  Machine1: GSC1{ } , GSC2{ P2 B1 }  , GSC3{ } , GSC4 { P1 B2 }
     *  
     * @throws InterruptedException 
     */
    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1")
    public void scaleInByRelocatingPrimaryOnSingleMachineTest() throws InterruptedException {
        
        admin.getGridServiceAgents().waitFor(1);
        gsa = admin.getGridServiceAgents().getAgents()[0];
                
        GridServiceContainer[] machine1Containers = AdminUtils.loadGSCs(gsa, 4, ZONE);
        
        ProcessingUnit pu = RebalancingTestUtils.deployProcessingUnitOnSingleMachine(gsm, ZONE, 2,1);
        
        RebalancingTestUtils.relocatePU(pu, 1, SpaceMode.PRIMARY, machine1Containers[0]);
        RebalancingTestUtils.relocatePU(pu, 2, SpaceMode.PRIMARY, machine1Containers[0]);
        RebalancingTestUtils.relocatePU(pu, 1, SpaceMode.BACKUP, machine1Containers[1]);
        RebalancingTestUtils.relocatePU(pu, 2, SpaceMode.BACKUP,  machine1Containers[3]);
        
        RebalancingTestUtils.insertPersonObjectsIntoSpace(pu, NUMBER_OF_OBJECTS);
        
        endScaleInRebalancingTest(pu, new GridServiceContainer[] { machine1Containers[1], machine1Containers[3] });
    }
    
    /**
     *  Before Rebalancing:     
     *  Machine1: GSC1{ P1,P2 } , GSC2{ B2 }  , GSC3{ } , GSC4 { B1 }
     *       *  
     *  After Rebalancing:
     *  Machine1: GSC1{ } , GSC2{ B1 P2 }  , GSC3{ } , GSC4 { P1 B2}
     *  
     * @throws InterruptedException 
     */
    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1")
    public void scaleInAgainAgainByRelocatingPrimaryOnSingleMachineTest() throws InterruptedException {
        
        admin.getGridServiceAgents().waitFor(1);
        gsa = admin.getGridServiceAgents().getAgents()[0];
        
        GridServiceContainer[] machine1Containers = AdminUtils.loadGSCs(gsa, 4, ZONE);
        
        ProcessingUnit pu = RebalancingTestUtils.deployProcessingUnitOnSingleMachine(gsm, ZONE, 2,1);
        
        RebalancingTestUtils.relocatePU(pu, 1, SpaceMode.PRIMARY, machine1Containers[0]);
        RebalancingTestUtils.relocatePU(pu, 2, SpaceMode.PRIMARY, machine1Containers[0]);
        RebalancingTestUtils.relocatePU(pu, 1, SpaceMode.BACKUP, machine1Containers[1]);
        RebalancingTestUtils.relocatePU(pu, 2, SpaceMode.BACKUP,  machine1Containers[3]);
        
        RebalancingTestUtils.insertPersonObjectsIntoSpace(pu, NUMBER_OF_OBJECTS);
        
        endScaleInRebalancingTest(pu, new GridServiceContainer[] { machine1Containers[1], machine1Containers[3] });
    }

    protected void endScaleInRebalancingTest(ProcessingUnit pu,GridServiceContainer[] selectedContainers)
            throws InterruptedException {
        
        Machine[] machines = new Machine[] { gsa.getMachine() };
        
        RebalancingTestUtils.assertNumberOfRelocations(rebalancing, 0);
        
        RebalancingTestUtils.enforceSlaAndWait(rebalancing, pu, selectedContainers);
        
        RebalancingTestUtils.assertPersonObjectsInSpaceAfterRebalancing(pu, NUMBER_OF_OBJECTS);
        
        RebalancingTestUtils.assertBalancedDeployment(pu, selectedContainers, machines);
        for (GridServiceContainer container : admin.getGridServiceContainers()) {
            if (!container.equals(selectedContainers[0]) && !container.equals(selectedContainers[1])) {
                Assert.assertEquals(container.getProcessingUnitInstances().length, 0);
            }
        }
        
        RebalancingTestUtils.assertNumberOfRelocations(rebalancing, 2);
    }
    
}
