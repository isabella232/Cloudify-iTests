package test.esm.datagrid.manual.memory;

import java.util.concurrent.TimeUnit;

import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.elastic.config.ManualCapacityScaleConfigurer;
import org.openspaces.admin.space.ElasticSpaceDeployment;
import org.openspaces.admin.space.Space;
import org.openspaces.core.util.MemoryUnit;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.esm.AbstractFromXenToByonGSMTest;
import framework.utils.GsmTestUtils;

public class DedicatedManualByonDataGridScaleOutAndScaleInTest extends AbstractFromXenToByonGSMTest {

    private final String gridName = "myspace";
    
    @BeforeMethod
    public void beforeTest() {
		super.beforeTestInit();
	}
	
	@BeforeClass
	protected void bootstrap() throws Exception {
		super.bootstrapBeforeClass();
	}
	
	@AfterMethod
    public void afterTest() {
		super.afterTest();
	}
	
	@AfterClass(alwaysRun = true)
	protected void teardownAfterClass() throws Exception {
		super.teardownAfterClass();
	}
    
    /**
     * This test should reproduce a case where after scale-in not enough GSCs are removed,
     * and relocation starts as if everything is ok
     * @author dank
     */
    
    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups="1", enabled = true)
    public void rebalancingAfterScaleInTest() {

    	repetitiveAssertNumberOfGSCsAdded(0, OPERATION_TIMEOUT);
    	repetitiveAssertNumberOfGSAsAdded(1, OPERATION_TIMEOUT);
        
        final ProcessingUnit pu = super.deploy(
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
        
        repetitiveAssertNumberOfGSCsAdded(4, OPERATION_TIMEOUT);
        repetitiveAssertNumberOfGSCsRemoved(0, OPERATION_TIMEOUT);
        
        pu.scale(new ManualCapacityScaleConfigurer()
                 .memoryCapacity(3, MemoryUnit.GIGABYTES)
                 .create());

        int postScaleOutExpectedNumberOfContainers = 16;
        GsmTestUtils.waitForScaleToCompleteIgnoreCpuSla(pu, postScaleOutExpectedNumberOfContainers, OPERATION_TIMEOUT*4);
        
        assertTrue("Failed waiting for space instances", 
                space.waitFor(space.getTotalNumberOfInstances(), 
                              DEFAULT_TEST_TIMEOUT, TimeUnit.MILLISECONDS));

        repetitiveAssertNumberOfGSCsRemoved(0, OPERATION_TIMEOUT);

        pu.scale(new ManualCapacityScaleConfigurer()
                 .memoryCapacity(1536, MemoryUnit.MEGABYTES)
                 .create());
    
        
        int postScaleInExpectedNumberOfContainers = 8;
        GsmTestUtils.waitForScaleToCompleteIgnoreCpuSla(pu, postScaleInExpectedNumberOfContainers, OPERATION_TIMEOUT*4);

        repetitiveAssertNumberOfGSCsAdded(16, OPERATION_TIMEOUT);
        repetitiveAssertNumberOfGSCsRemoved(8, OPERATION_TIMEOUT);
        
        assertTrue("Failed waiting for space instances", 
                space.waitFor(space.getTotalNumberOfInstances(), 
                        OPERATION_TIMEOUT*4, TimeUnit.MILLISECONDS));
        
        assertUndeployAndWait(pu);
        
    }

}
