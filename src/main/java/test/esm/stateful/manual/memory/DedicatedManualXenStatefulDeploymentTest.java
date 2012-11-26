package test.esm.stateful.manual.memory;

import java.io.File;
import java.io.IOException;

import org.openspaces.admin.internal.pu.InternalProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.elastic.ElasticStatefulProcessingUnitDeployment;
import org.openspaces.admin.pu.elastic.config.ManualCapacityScaleConfig;
import org.openspaces.admin.pu.elastic.config.ManualCapacityScaleConfigurer;
import org.openspaces.admin.pu.elastic.config.ScaleStrategyConfig;
import org.openspaces.core.util.MemoryUnit;
import org.testng.annotations.Test;

import test.esm.AbstractFromXenToByonGSMTest;
import framework.utils.DeploymentUtils;
import framework.utils.GsmTestUtils;

public class DedicatedManualXenStatefulDeploymentTest extends AbstractFromXenToByonGSMTest {

	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1")
    public void testElasticStatefulProcessingUnitDeploymentScale() throws IOException {
		elasticStatefulProcessingUnitDeployment(true);
	}
	
    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1")
    public void testElasticStatefulProcessingUnitDeployment() throws IOException {
    	elasticStatefulProcessingUnitDeployment(false);
    }
    
    void elasticStatefulProcessingUnitDeployment(boolean scaleCommand) throws IOException {
  
        repetitiveAssertNumberOfGSCsAdded(0, OPERATION_TIMEOUT);
        repetitiveAssertNumberOfGSAsAdded(1, OPERATION_TIMEOUT);
         
        DeploymentUtils.prepareApp("simpledata");
        File puDir = DeploymentUtils.getProcessingUnit("simpledata", "processor");
        
        ManualCapacityScaleConfig manualCapacityScaleConfig = 
        	new ManualCapacityScaleConfigurer()
			.memoryCapacity(512, MemoryUnit.MEGABYTES)
			.create();
        
		ElasticStatefulProcessingUnitDeployment deployment = 
			new ElasticStatefulProcessingUnitDeployment(puDir)
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
		assertTrue(!retrievedScaleConfig.equals(new ManualCapacityScaleConfig()));
        assertEquals(manualCapacityScaleConfig,retrievedScaleConfig);
        
        assertUndeployAndWait(pu);
    } 
}
