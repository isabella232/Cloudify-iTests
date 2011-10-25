package test.gsm.stateful.manual.memory.xen;

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

import test.gsm.AbstractXenGSMTest;
import test.gsm.GsmTestUtils;
import test.utils.AssertUtils;
import test.utils.DeploymentUtils;
import test.utils.AssertUtils.RepetitiveConditionProvider;

public class DedicatedManualXenStatefulDeploymentTest extends AbstractXenGSMTest {

	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1")
    public void testElasticStatefulProcessingUnitDeploymentScale() throws IOException {
		elasticStatefulProcessingUnitDeployment(true);
	}
	
    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1")
    public void testElasticStatefulProcessingUnitDeployment() throws IOException {
    	elasticStatefulProcessingUnitDeployment(false);
    }
    
    void elasticStatefulProcessingUnitDeployment(boolean scaleCommand) throws IOException {
  
        assertEquals(0, getNumberOfGSCsAdded());
        assertEquals(1, getNumberOfGSAsAdded());
         
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
		final ProcessingUnit pu = gsm.deploy(deployment);

		if (scaleCommand) {
			pu.scale(manualCapacityScaleConfig);
		}
		
        int expectedNumberOfContainers = 2;
        int expectedNumberOfMachines = 2;
		GsmTestUtils.waitForScaleToCompleteIgnoreCpuSla(pu, expectedNumberOfContainers, expectedNumberOfMachines, OPERATION_TIMEOUT);
        

        assertEquals("Number of GSAs added", expectedNumberOfMachines, getNumberOfGSAsAdded());
        assertEquals("Number of GSAs removed", 0, getNumberOfGSAsRemoved());
        assertEquals("Number of GSCs added", expectedNumberOfContainers, getNumberOfGSCsAdded());
        assertEquals("Number of GSCs removed", 0, getNumberOfGSCsRemoved());

        ScaleStrategyConfig retrievedScaleConfig = ((InternalProcessingUnit)pu).getScaleStrategyConfig();
		assertTrue(!retrievedScaleConfig.equals(new ManualCapacityScaleConfig()));
        assertEquals(manualCapacityScaleConfig,retrievedScaleConfig);
        
        pu.undeploy();
        final RepetitiveConditionProvider condition = new RepetitiveConditionProvider() {
            public boolean getCondition() {
                return admin.getGridServiceContainers().isEmpty() && admin.getMachines().getSize() == 1;
            }
        };
        AssertUtils.repetitiveAssertTrue("Waiting for undeploy to complete", condition, OPERATION_TIMEOUT);
        
    } 
}
