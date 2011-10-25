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
     */
    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "2")
    public void rebalanceByRestartingPrimaryInstanceTest() throws InterruptedException {
        
        GridServiceAgent gsa = startNewVM(2, 0, OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
        GridServiceAgent gsa2 = startNewVM(2, 0, OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
        
        Machine[] machines = new Machine[] { gsa.getMachine(), gsa2.getMachine() };
        
        GridServiceContainer[] machine1Containers = AdminUtils.loadGSCs(gsa, 1, ZONE);
        GridServiceContainer[] machine2Containers = AdminUtils.loadGSCs(gsa2, 1, ZONE);
        
        ProcessingUnit pu = RebalancingTestUtils.deployProcessingUnitOnTwoMachines(gsm, ZONE, 2,1);
        
        RebalancingTestUtils.relocatePU(pu, 1, SpaceMode.PRIMARY, machine1Containers[0]);
        RebalancingTestUtils.relocatePU(pu, 2, SpaceMode.PRIMARY, machine1Containers[0]);
        RebalancingTestUtils.relocatePU(pu, 1, SpaceMode.BACKUP,  machine2Containers[0]);
        RebalancingTestUtils.relocatePU(pu, 2, SpaceMode.BACKUP,  machine2Containers[0]);
        
        RebalancingTestUtils.insertPersonObjectsIntoSpace(pu, NUMBER_OF_OBJECTS);
        
        RebalancingTestUtils.assertNumberOfRelocations(rebalancing, 0);
        
        RebalancingTestUtils.enforceSlaAndWait(rebalancing, pu);
        
        RebalancingTestUtils.assertBalancedDeployment(pu, machines);
        
        RebalancingTestUtils.assertPersonObjectsInSpaceAfterRebalancing(pu, NUMBER_OF_OBJECTS);
        
        RebalancingTestUtils.assertNumberOfRelocations(rebalancing, 1);
    }
    
}
