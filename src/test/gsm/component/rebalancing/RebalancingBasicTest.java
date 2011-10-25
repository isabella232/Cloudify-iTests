package test.gsm.component.rebalancing;

import java.util.logging.Logger;

import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.ProcessingUnit;
import org.testng.Assert;
import org.testng.annotations.Test;

import test.utils.AdminUtils;

public class RebalancingBasicTest extends AbstractRebalancingSlaEnforcementTest {

	Logger logger = Logger.getLogger(this.getClass().getName());
	
    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1")
    public void doNothingTest() throws InterruptedException {
        Machine machine = gsa.getMachine();
		Machine[] machines = new Machine[] { machine };
        
        
        //deploy pu partitioned(2,1) on 4 GSCs
        Assert.assertEquals(admin.getGridServiceContainers().getSize(), 0);
        GridServiceContainer[] containers = AdminUtils.loadGSCs(gsa, 4, ZONE);
        Assert.assertEquals(containers.length, 4);
        machine.getGridServiceContainers().waitFor(4);
        
        ProcessingUnit pu = RebalancingTestUtils.deployProcessingUnitOnSingleMachine(gsm, ZONE, 2,1);
        
        RebalancingTestUtils.insertPersonObjectsIntoSpace(pu, NUMBER_OF_OBJECTS);

        RebalancingTestUtils.enforceSlaAndWait(rebalancing, pu);    

        RebalancingTestUtils.assertBalancedDeployment(pu, machines);
        
        RebalancingTestUtils.assertNumberOfRelocations(rebalancing, 0);
    }
    
}
