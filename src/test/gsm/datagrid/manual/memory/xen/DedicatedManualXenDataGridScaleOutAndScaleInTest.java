package test.gsm.datagrid.manual.memory.xen;

import java.util.concurrent.TimeUnit;

import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.elastic.config.ManualCapacityScaleConfigurer;
import org.openspaces.admin.space.ElasticSpaceDeployment;
import org.openspaces.admin.space.Space;
import org.openspaces.core.util.MemoryUnit;
import org.testng.annotations.Test;

import test.gsm.AbstractXenGSMTest;
import test.gsm.GsmTestUtils;

public class DedicatedManualXenDataGridScaleOutAndScaleInTest extends AbstractXenGSMTest {

    private final String gridName = "myspace";
    
    /**
     * This test should reproduce a case where after scale-in not enough GSCs are removed,
     * and relocation starts as if everything is ok
     * @author dank
     */
    
    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups="1", enabled = true)
    public void rebalancingAfterScaleInTest() {

        assertEquals(0, getNumberOfGSCsAdded());
        assertEquals(1, getNumberOfGSAsAdded());
        
        final ProcessingUnit pu = gsm.deploy(
        		new ElasticSpaceDeployment(gridName)
                .maxMemoryCapacity(3, MemoryUnit.GIGABYTES)
                .memoryCapacityPerContainer(192,MemoryUnit.MEGABYTES)
                .scale(new ManualCapacityScaleConfigurer()
                       .memoryCapacity(768,MemoryUnit.MEGABYTES)
                       .create())
                .dedicatedMachineProvisioning(getMachineProvisioningConfig())
        );

        int preScaleOutExpectedNumberOfContainers = 4;
        GsmTestUtils.waitForScaleToCompleteIgnoreCpuSla(pu, preScaleOutExpectedNumberOfContainers, OPERATION_TIMEOUT*4);
        
        Space space = pu.waitForSpace(OPERATION_TIMEOUT*4, TimeUnit.MILLISECONDS);
        
        assertNotNull("Failed getting space instance", space);
        
        assertTrue("Failed waiting for space instances", 
                space.waitFor(space.getTotalNumberOfInstances(), 
                        OPERATION_TIMEOUT*4, TimeUnit.MILLISECONDS));
        
        assertEquals("Number of GSCs added", 4, getNumberOfGSCsAdded());
        assertEquals("Number of GSCs removed", 0, getNumberOfGSCsRemoved());
        
        pu.scale(new ManualCapacityScaleConfigurer()
                 .memoryCapacity(3, MemoryUnit.GIGABYTES)
                 .create());

        int postScaleOutExpectedNumberOfContainers = 16;
        GsmTestUtils.waitForScaleToCompleteIgnoreCpuSla(pu, postScaleOutExpectedNumberOfContainers, OPERATION_TIMEOUT*4);
        
        assertTrue("Failed waiting for space instances", 
                space.waitFor(space.getTotalNumberOfInstances(), 
                              DEFAULT_TEST_TIMEOUT, TimeUnit.MILLISECONDS));

        assertEquals("Number of GSCs removed", 0, getNumberOfGSCsRemoved());

        pu.scale(new ManualCapacityScaleConfigurer()
                 .memoryCapacity(1536, MemoryUnit.MEGABYTES)
                 .create());
    
        
        int postScaleInExpectedNumberOfContainers = 8;
        GsmTestUtils.waitForScaleToCompleteIgnoreCpuSla(pu, postScaleInExpectedNumberOfContainers, OPERATION_TIMEOUT*4);

        assertEquals("Number of GSCs addes", 16, getNumberOfGSCsAdded());
        assertEquals("Number of GSCs removed", 8, getNumberOfGSCsRemoved());
        
        assertTrue("Failed waiting for space instances", 
                space.waitFor(space.getTotalNumberOfInstances(), 
                        OPERATION_TIMEOUT*4, TimeUnit.MILLISECONDS));
        
    }

}
