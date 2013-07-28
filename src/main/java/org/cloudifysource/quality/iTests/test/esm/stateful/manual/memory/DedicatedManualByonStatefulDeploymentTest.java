package org.cloudifysource.quality.iTests.test.esm.stateful.manual.memory;

import iTests.framework.utils.DeploymentUtils;
import iTests.framework.utils.GsmTestUtils;
import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
import org.cloudifysource.quality.iTests.test.esm.AbstractFromXenToByonGSMTest;
import org.openspaces.admin.internal.pu.InternalProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.elastic.ElasticStatefulProcessingUnitDeployment;
import org.openspaces.admin.pu.elastic.config.ManualCapacityScaleConfig;
import org.openspaces.admin.pu.elastic.config.ManualCapacityScaleConfigurer;
import org.openspaces.admin.pu.elastic.config.ScaleStrategyConfig;
import org.openspaces.core.util.MemoryUnit;
import org.testng.annotations.*;

import java.io.IOException;

public class DedicatedManualByonStatefulDeploymentTest extends AbstractFromXenToByonGSMTest {
	
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
	
	
	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT, groups = "1")
    public void testElasticStatefulProcessingUnitDeploymentScale() throws IOException {
		elasticStatefulProcessingUnitDeployment(true);
	}
	
    @Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT, groups = "1")
    public void testElasticStatefulProcessingUnitDeployment() throws IOException {
    	elasticStatefulProcessingUnitDeployment(false);
    }
    
    void elasticStatefulProcessingUnitDeployment(boolean scaleCommand) throws IOException {
  
        repetitiveAssertNumberOfGSCsAdded(0, OPERATION_TIMEOUT);
        repetitiveAssertNumberOfGSAsAdded(1, OPERATION_TIMEOUT);
        
        ManualCapacityScaleConfig manualCapacityScaleConfig = 
        	new ManualCapacityScaleConfigurer()
			.memoryCapacity(512, MemoryUnit.MEGABYTES)
			.create();
        
		ElasticStatefulProcessingUnitDeployment deployment = 
			new ElasticStatefulProcessingUnitDeployment(DeploymentUtils.getProcessingUnit("simpledata", "processor"))
			.maxMemoryCapacity(512, MemoryUnit.MEGABYTES)
			.memoryCapacityPerContainer(256,MemoryUnit.MEGABYTES)
			.dedicatedMachineProvisioning(getMachineProvisioningConfig());
		
		if (!scaleCommand) {
			deployment.scale(manualCapacityScaleConfig);
		}
		final ProcessingUnit pu = super.deploy(deployment);

		if (scaleCommand) {
			pu.scale(manualCapacityScaleConfig);
		}
		
        int expectedNumberOfContainers = 2;
        int expectedNumberOfMachines = 2;
		GsmTestUtils.waitForScaleToCompleteIgnoreCpuSla(pu, expectedNumberOfContainers, expectedNumberOfMachines, OPERATION_TIMEOUT);
        

        repetitiveAssertNumberOfGSAsAdded(expectedNumberOfMachines, OPERATION_TIMEOUT);
        repetitiveAssertNumberOfGSAsRemoved(0, OPERATION_TIMEOUT);
        repetitiveAssertNumberOfGSCsAdded(expectedNumberOfContainers, OPERATION_TIMEOUT);
        repetitiveAssertNumberOfGSCsRemoved(0, OPERATION_TIMEOUT);

        ScaleStrategyConfig retrievedScaleConfig = ((InternalProcessingUnit)pu).getScaleStrategyConfig();
		AbstractTestSupport.assertTrue(!retrievedScaleConfig.equals(new ManualCapacityScaleConfig()));
        AbstractTestSupport.assertEquals(manualCapacityScaleConfig, retrievedScaleConfig);
        
        assertUndeployAndWait(pu);
    } 
}
