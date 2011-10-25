package test.gsm.datagrid.manual.advanced;

import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.elastic.config.DiscoveredMachineProvisioningConfig;
import org.openspaces.admin.pu.elastic.config.EagerScaleConfig;
import org.openspaces.admin.space.ElasticSpaceDeployment;
import org.openspaces.core.util.MemoryUnit;
import org.testng.annotations.Test;

import test.gsm.AbstractGsmTest;
import test.gsm.GsmTestUtils;

/**
 * @author giladh
 */
public class AdvancedDeployOnManagementMachineTest extends AbstractGsmTest {
    
    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "4")
    public void doTestWithManagementMachine() {
    	boolean allowDeploymentOnManagementMachine = true;
        doTestInternal(allowDeploymentOnManagementMachine);
    }

    /**
     * GS-9482
     */
    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "4", enabled = false)
    public void doTestWithoutManagementMachine() {
        boolean allowDeploymentOnManagementMachine = false;
        doTestInternal(allowDeploymentOnManagementMachine);
    }
    
    private void doTestInternal(boolean allowDeploymentOnManagementMachine) {
     
        admin.getGridServiceAgents().waitFor(4);
        assertEquals(0, getNumberOfGSCsAdded());
        
        final int NUM_CONTAINERS = 4;
        final int MEM_PER_CONTAINER = 256;
               
        DiscoveredMachineProvisioningConfig machineProvisioning = new DiscoveredMachineProvisioningConfig();
        machineProvisioning.setDedicatedManagementMachines(!allowDeploymentOnManagementMachine);
       
        ProcessingUnit pu = gsm.deploy(new ElasticSpaceDeployment("mygrid")
                .maxMemoryCapacity(NUM_CONTAINERS*MEM_PER_CONTAINER, MemoryUnit.MEGABYTES)
                .memoryCapacityPerContainer(MEM_PER_CONTAINER,MemoryUnit.MEGABYTES)
                .dedicatedMachineProvisioning(machineProvisioning)
                .scale(new EagerScaleConfig())
            );
        
        GsmTestUtils.waitForScaleToCompleteIgnoreCpuSla(pu, NUM_CONTAINERS, OPERATION_TIMEOUT);
        assertEquals("Number of GSCs added", NUM_CONTAINERS, getNumberOfGSCsAdded());
        assertEquals("Number of GSCs removed", 0, getNumberOfGSCsRemoved());
        
        
        Machine gsmMachine = admin.getGridServiceManagers().getManagers()[0].getMachine();
        
        boolean containersOnGsmMachine = 
        	gsmMachine.getGridServiceContainers().getSize() > 0;
        	    
        assertEquals("Containers deployed on manager machine", containersOnGsmMachine, allowDeploymentOnManagementMachine);
    }    
}
