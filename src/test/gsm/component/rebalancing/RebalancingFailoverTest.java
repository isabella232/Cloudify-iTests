package test.gsm.component.rebalancing;

import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.space.Space;
import org.testng.Assert;
import org.testng.annotations.Test;

import test.utils.AdminUtils;

public class RebalancingFailoverTest extends AbstractRebalancingSlaEnforcementTest {

    // TODO: add number of relocations actually performed
    
    /**
     *  Before Failover:
     *  GSC1{ P1 } , GSC2{ B1 }  , GSC3{ P2 }, GSC4 { B2 }
     *  
     *  In Between:
     *  GSC1{ } , GSC2{ P1 }  , GSC3{ P2,B2 }, GSC4 { B2 }
     *  
     *  After Failover of GSC1:
     *  GSC1{ B2 } , GSC2{ P1 }  , GSC3{ P2 }, GSC4 { B2 }
     */
    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1")
    public void failoverRebalancingTest() throws InterruptedException {
        
        Machine[] machines = new Machine[] { gsa.getMachine() };
        
        ProcessingUnit pu = RebalancingTestUtils.doNothingTest(rebalancing, gsa, gsm, ZONE, NUMBER_OF_OBJECTS);
        
        //restart a GSC
        RebalancingTestUtils.killGscAndWait(admin.getGridServiceContainers().iterator().next());
        Assert.assertEquals(admin.getGridServiceContainers().getSize(), 3);

        Space space = pu.getSpace();
        assertTrue(space.waitFor(space.getTotalNumberOfInstances()));
        
        AdminUtils.loadGSCs(gsa, 1, ZONE);
        admin.getGridServiceContainers().waitFor(4);
        
        RebalancingTestUtils.enforceSlaAndWait(rebalancing, pu);
        
        RebalancingTestUtils.assertPersonObjectsInSpaceAfterRebalancing(pu, NUMBER_OF_OBJECTS);
        
        RebalancingTestUtils.assertBalancedDeployment(pu, machines);
        
        // expecting one relocations to take place
        RebalancingTestUtils.assertNumberOfRelocations(rebalancing, 1);

    }
    
}
