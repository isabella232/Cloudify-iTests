package org.cloudifysource.quality.iTests.test.esm.stateless.manual.cpu;

import java.io.File;
import java.io.IOException;

import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.elastic.ElasticStatelessProcessingUnitDeployment;
import org.openspaces.admin.pu.elastic.config.ManualCapacityScaleConfigurer;
import org.openspaces.core.util.MemoryUnit;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.cloudifysource.quality.iTests.test.esm.AbstractFromXenToByonGSMTest;
import org.cloudifysource.quality.iTests.framework.utils.AssertUtils.RepetitiveConditionProvider;
import org.cloudifysource.quality.iTests.framework.utils.DeploymentUtils;
import org.cloudifysource.quality.iTests.framework.utils.LogUtils;

public class DedicatedStatelessManualByonCPUScaleInTest extends AbstractFromXenToByonGSMTest {
	
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
	
    @Test(timeOut = DEFAULT_TEST_TIMEOUT*2, groups = "1")
    public void doTest() throws IOException {
    	         
	    // make sure no gscs yet created
	    repetitiveAssertNumberOfGSCsAdded(0, OPERATION_TIMEOUT);
	    repetitiveAssertNumberOfGSAsAdded(1, OPERATION_TIMEOUT);
	    int numberOfCpuCores = 8;
	    
	    File archive = DeploymentUtils.getArchive("servlet.war");
        
        final ProcessingUnit pu = super.deploy(
                new ElasticStatelessProcessingUnitDeployment(archive)
                .memoryCapacityPerContainer(1, MemoryUnit.GIGABYTES)
                .dedicatedMachineProvisioning(getMachineProvisioningConfig()).

                scale(new ManualCapacityScaleConfigurer()
                      .numberOfCpuCores(numberOfCpuCores)
                      .create())
        );
        
	    int expectedNumberOfContainers = (int) Math.ceil(numberOfCpuCores/super.getMachineProvisioningConfig().getMinimumNumberOfCpuCoresPerMachine());
	    int expectedNumberOfMachines = expectedNumberOfContainers;
	    
	    pu.waitFor(expectedNumberOfContainers);
	    
	    repetitiveAssertNumberOfGSCsAdded(expectedNumberOfContainers, OPERATION_TIMEOUT);
	    repetitiveAssertNumberOfGSCsRemoved(0, OPERATION_TIMEOUT);
	    repetitiveAssertNumberOfGSAsAdded(expectedNumberOfMachines, OPERATION_TIMEOUT);
	    repetitiveAssertNumberOfGSAsRemoved(0, OPERATION_TIMEOUT);
	    	    
	    // compress 4 gscs into 2 machines	    
	    numberOfCpuCores = 4;
	    pu.scale(new ManualCapacityScaleConfigurer()
	    		 .numberOfCpuCores(numberOfCpuCores)
	    		 .create());
	    	    
	    final int expectedNumberOfContainersAfterScaleIn = (int) Math.ceil(numberOfCpuCores/super.getMachineProvisioningConfig().getMinimumNumberOfCpuCoresPerMachine());
	    final int expectedRemoved = expectedNumberOfContainers - expectedNumberOfContainersAfterScaleIn;

	    repetitiveAssertTrue("Scaling in two instances",new RepetitiveConditionProvider() {

            public boolean getCondition() {
            	boolean condition = true;
            	int actualNumberOfInstances = pu.getInstances().length;
				if (actualNumberOfInstances != expectedNumberOfContainersAfterScaleIn) {
            		LogUtils.log("Waiting for " + expectedNumberOfContainersAfterScaleIn + " instances, currently " + actualNumberOfInstances + " instances.");
            		condition = false;
            	}
				return condition;
            }
	    },OPERATION_TIMEOUT);
	        
	    repetitiveAssertNumberOfGSCsAdded(expectedNumberOfContainers, OPERATION_TIMEOUT);
	    repetitiveAssertNumberOfGSCsRemoved(expectedRemoved , OPERATION_TIMEOUT);
	    repetitiveAssertNumberOfGSAsAdded(expectedNumberOfMachines, OPERATION_TIMEOUT);
	    repetitiveAssertNumberOfGSAsRemoved(expectedRemoved, OPERATION_TIMEOUT);
	    
	    assertUndeployAndWait(pu);
	}

}


