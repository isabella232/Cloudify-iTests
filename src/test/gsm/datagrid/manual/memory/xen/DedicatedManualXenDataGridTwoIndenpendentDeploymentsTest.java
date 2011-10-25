package test.gsm.datagrid.manual.memory.xen;


import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.elastic.config.ManualCapacityScaleConfigurer;
import org.openspaces.admin.space.ElasticSpaceDeployment;
import org.openspaces.admin.space.Space;
import org.openspaces.core.util.MemoryUnit;
import org.testng.annotations.Test;

import test.gsm.AbstractXenGSMTest;
/**
 * Tests deployment of two different space (each space should be (1,1), there should be 4 machines in total
 *
 * @author dank
 */
public class DedicatedManualXenDataGridTwoIndenpendentDeploymentsTest extends AbstractXenGSMTest {

    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1")
    public void manualXenDataGridDeploymentTwoIndenpendentDeploymentsTest() {
   	    	 
    	assertEquals(0, getNumberOfGSCsAdded());
        
        final ProcessingUnit pu = gsm.deploy(new ElasticSpaceDeployment("mygrid")
                .maxMemoryCapacity(512, MemoryUnit.MEGABYTES)
                .memoryCapacityPerContainer(256,MemoryUnit.MEGABYTES)
                .scale(new ManualCapacityScaleConfigurer()
                       .memoryCapacity(512,MemoryUnit.MEGABYTES)
                       .create())
                .dedicatedMachineProvisioning(getMachineProvisioningConfig())
        );
        
        final ProcessingUnit pu2 = gsm.deploy(new ElasticSpaceDeployment("mygrid2")
                .maxMemoryCapacity(512, MemoryUnit.MEGABYTES)
                .memoryCapacityPerContainer(256,MemoryUnit.MEGABYTES)
                .scale(new ManualCapacityScaleConfigurer()
                       .memoryCapacity(512,MemoryUnit.MEGABYTES)
                       .create())
                .dedicatedMachineProvisioning(getMachineProvisioningConfig())
        );
        
        Space space = pu.waitForSpace(DEFAULT_TEST_TIMEOUT, TimeUnit.MILLISECONDS);
        Space space2 = pu2.waitForSpace(DEFAULT_TEST_TIMEOUT, TimeUnit.MILLISECONDS);
        
        assertNotNull("Failed getting space instance", space);
        assertNotNull("Failed getting space2 instance", space2);
        
        assertTrue("Failed waiting for space instances", 
                space.waitFor(space.getTotalNumberOfInstances(), 
                		DEFAULT_TEST_TIMEOUT, TimeUnit.MILLISECONDS));
        assertTrue("Failed waiting for space2 instances", 
                space2.waitFor(space2.getTotalNumberOfInstances(), 
                        DEFAULT_TEST_TIMEOUT, TimeUnit.MILLISECONDS));
        
        assertEquals("Number of GSAs added", 4, getNumberOfGSAsAdded());
        assertEquals("Number of GSCs added", 4, getNumberOfGSCsAdded());
        assertEquals("Number of GSCs removed", 0, getNumberOfGSCsRemoved());
        
        Set<Machine> machines = new HashSet<Machine>();
        
        assertTrue("Machines should be different", machines.add(space.getInstances()[0].getMachine()));
        assertTrue("Machines should be different", machines.add(space.getInstances()[1].getMachine()));
        assertTrue("Machines should be different", machines.add(space2.getInstances()[0].getMachine()));
        assertTrue("Machines should be different", machines.add(space2.getInstances()[1].getMachine()));
        
    }
	
}


