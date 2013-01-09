package test.esm.component.rebalancing;

import java.util.concurrent.TimeUnit;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.ProcessingUnit;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.gigaspaces.cluster.activeelection.SpaceMode;

public class RebalancingScaleOutPrimaryRelocationByonTest extends AbstractRebalancingSlaEnforcementByonTest {

    /**
     *  Before Rebalancing:
     *  GSC1{ B1 } , GSC2{ B2 }  , GSC3{ P1,P2 }, GSC4 {}
     *  
     *  After Rebalancing:
     *  GSC1{ P1 } , GSC2{ B2 }  , GSC3{ P2 }, GSC4 { B1 }
     * @throws Exception 
     *  
     */
    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1")
    public void scaleOutRebalancingPrimaryRelocationTest() throws Exception {
    	repetitiveAssertNumberOfGSAsAdded(1, OPERATION_TIMEOUT);
    	repetitiveAssertNumberOfGSCsAdded(0, OPERATION_TIMEOUT);
    	
    	GridServiceManager gridServiceManager = admin.getGridServiceManagers().getManagers()[0];
    	GridServiceAgent gsa = startNewByonMachine(getElasticMachineProvisioningCloudifyAdapter(), OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
        Machine[] machines = new Machine[] { gsa.getMachine()};
        
        //deploy pu partitioned(2,1) on 4 GSCs
        loadGSCs(gsa, 4);
        admin.getGridServiceContainers().waitFor(4);
        GridServiceContainer[] containers = admin.getGridServiceContainers().getContainers();
        Assert.assertEquals(containers.length, 4);
        
        ProcessingUnit pu = RebalancingTestUtils.deployProcessingUnitOnSingleMachine(gridServiceManager, ZONE, 2,1);
        
        RebalancingTestUtils.insertPersonObjectsIntoSpace(pu, NUMBER_OF_OBJECTS);
        
        RebalancingTestUtils.relocatePU(pu, 1, SpaceMode.BACKUP, containers[0]);
        RebalancingTestUtils.relocatePU(pu, 2, SpaceMode.BACKUP, containers[1]);
        RebalancingTestUtils.relocatePU(pu, 1, SpaceMode.PRIMARY, containers[2]);
        RebalancingTestUtils.relocatePU(pu, 2, SpaceMode.PRIMARY, containers[2]);
        
        RebalancingTestUtils.assertNumberOfRelocations(rebalancing, 0);
        
        RebalancingTestUtils.enforceSlaAndWait(rebalancing, pu, OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
        
        RebalancingTestUtils.assertPersonObjectsInSpaceAfterRebalancing(pu, NUMBER_OF_OBJECTS);
        
        RebalancingTestUtils.assertBalancedDeployment(pu, machines);
        
        RebalancingTestUtils.assertNumberOfRelocations(rebalancing, 1);
        
        assertUndeployAndWait(pu);
        
    }
    
}
