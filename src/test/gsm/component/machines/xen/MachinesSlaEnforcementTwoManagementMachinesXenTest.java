package test.gsm.component.machines.xen;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsa.GridServiceManagerOptions;
import org.openspaces.grid.gsm.machines.CapacityMachinesSlaPolicy;
import org.openspaces.grid.gsm.sla.exceptions.SlaEnforcementEndpointDestroyedException;
import org.testng.Assert;
import org.testng.annotations.Test;

import test.gsm.component.SlaEnforcementTestUtils;

public class MachinesSlaEnforcementTwoManagementMachinesXenTest extends AbstractMachinesSlaEnforcementTest {
    
    @Test(timeOut = DEFAULT_TEST_TIMEOUT)
    public void oneMachineTest() throws InterruptedException, SlaEnforcementEndpointDestroyedException  {
        
        // the first GSAs is already started in BeginTest
        Assert.assertEquals(admin.getGridServiceAgents().getSize(),1);
        Assert.assertEquals(getNumberOfGSAsAdded()   ,1);
        Assert.assertEquals(getNumberOfGSAsRemoved() ,0);
        
        // Start a seconds machine and put a LUS on it
        GridServiceAgent gsa2 = super.startNewVM(OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
        gsa2.startGridService(new GridServiceManagerOptions());
        Assert.assertEquals(admin.getGridServiceAgents().getSize(),2);
        Assert.assertTrue(admin.getGridServiceManagers().waitFor(2,OPERATION_TIMEOUT,TimeUnit.MILLISECONDS));
        
        
        // enforce numberOfMachines SLA
        endpoint = createEndpoint(pu, machinesSlaEnforcement);
        CapacityMachinesSlaPolicy sla = createSla(1);
        
        String firstApprovedAgentUid = null; 
        for (int i = 0 ; i < 3 ; i++) {

        	SlaEnforcementTestUtils.enforceSlaAndWait(admin, endpoint, sla, machineProvisioning);
        	Collection<String> agentUids = endpoint.getAllocatedCapacity().getAgentUids();
            assertEquals(1, agentUids.size());
        	String allocatedAgentUid = agentUids.iterator().next();
        	if (firstApprovedAgentUid == null) {
        		firstApprovedAgentUid = allocatedAgentUid;
        	}
        	else {
        		Assert.assertEquals(
        				firstApprovedAgentUid,
        				allocatedAgentUid,
        				"MachinesSla keeps changing its mind");
        	}
	        
        	
        }
        
        // make sure no extra machine was started nor terminated
    	// even after SLA has reached steady state
        Assert.assertEquals(getNumberOfGSAsAdded()   ,2);
        Assert.assertEquals(getNumberOfGSAsRemoved() ,0);
        
    }
 }
