package test.gsm.component.rebalancing.xen;

import java.util.concurrent.TimeUnit;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.ProcessingUnit;
import org.testng.Assert;
import org.testng.annotations.Test;

import test.gsm.component.rebalancing.RebalancingTestUtils;
import test.utils.AdminUtils;

import com.gigaspaces.cluster.activeelection.SpaceMode;

public class RebalancingScaleOutPrimaryRelocationXenTest extends AbstractRebalancingSlaEnforcementXenTest {

    /**
     *  Before Rebalancing:
     *  GSC1{ B1 } , GSC2{ B2 }  , GSC3{ P1,P2 }, GSC4 {}
     *  
     *  After Rebalancing:
     *  GSC1{ P1 } , GSC2{ B2 }  , GSC3{ P2 }, GSC4 { B1 }
     *  
     */
    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1")
    public void scaleOutRebalancingPrimaryRelocationTest() throws InterruptedException {

    	GridServiceAgent gsa = startNewVM(2, 0, OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
        Machine[] machines = new Machine[] { gsa.getMachine()};
        
        //deploy pu partitioned(2,1) on 4 GSCs
        AdminUtils.loadGSCs(gsa, 4, ZONE);
        admin.getGridServiceContainers().waitFor(4);
        GridServiceContainer[] containers = admin.getGridServiceContainers().getContainers();
        Assert.assertEquals(containers.length, 4);
        
        ProcessingUnit pu = RebalancingTestUtils.deployProcessingUnitOnSingleMachine(gsm, ZONE, 2,1);
        
        RebalancingTestUtils.insertPersonObjectsIntoSpace(pu, NUMBER_OF_OBJECTS);
        
        RebalancingTestUtils.relocatePU(pu, 1, SpaceMode.BACKUP, containers[0]);
        RebalancingTestUtils.relocatePU(pu, 2, SpaceMode.BACKUP, containers[1]);
        RebalancingTestUtils.relocatePU(pu, 1, SpaceMode.PRIMARY, containers[2]);
        RebalancingTestUtils.relocatePU(pu, 2, SpaceMode.PRIMARY, containers[2]);
        
        RebalancingTestUtils.assertNumberOfRelocations(rebalancing, 0);
        
        RebalancingTestUtils.enforceSlaAndWait(rebalancing, pu);
        
        RebalancingTestUtils.assertPersonObjectsInSpaceAfterRebalancing(pu, NUMBER_OF_OBJECTS);
        
        RebalancingTestUtils.assertBalancedDeployment(pu, machines);
        
        RebalancingTestUtils.assertNumberOfRelocations(rebalancing, 1);
        
    }
    
}
