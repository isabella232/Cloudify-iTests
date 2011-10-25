package test.gsm.component.rebalancing;

import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.grid.gsm.rebalancing.RebalancingSlaEnforcementEndpoint;
import org.openspaces.grid.gsm.rebalancing.RebalancingSlaPolicy;
import org.openspaces.grid.gsm.sla.exceptions.SlaEnforcementEndpointDestroyedException;
import org.openspaces.grid.gsm.sla.exceptions.SlaEnforcementException;
import org.testng.annotations.Test;

import test.utils.AdminUtils;

public class RebalancingDestroyTest extends AbstractRebalancingSlaEnforcementTest {

    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", expectedExceptions=SlaEnforcementEndpointDestroyedException.class)
    public void destroyTest() throws InterruptedException, SlaEnforcementException {
        
        AdminUtils.loadGSCs(gsa, 2, ZONE);
        ProcessingUnit pu = RebalancingTestUtils.deployProcessingUnitOnSingleMachine(gsm, ZONE, 1,1);
        RebalancingSlaEnforcementEndpoint endpoint = rebalancing.createEndpoint(pu);
        // destroy endpoint before using it.
        this.rebalancing.destroyEndpoint(pu);
        endpoint.enforceSla(new RebalancingSlaPolicy());
    
    }
    
}
