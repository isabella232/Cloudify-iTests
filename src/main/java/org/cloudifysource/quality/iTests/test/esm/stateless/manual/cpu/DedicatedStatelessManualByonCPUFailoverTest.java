package org.cloudifysource.quality.iTests.test.esm.stateless.manual.cpu;


import java.io.File;

import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
import org.cloudifysource.quality.iTests.test.esm.AbstractFromXenToByonGSMTest;
import org.openspaces.admin.gsc.GridServiceContainer;
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
import org.cloudifysource.quality.iTests.framework.utils.DeploymentUtils;
import org.cloudifysource.quality.iTests.framework.utils.GsmTestUtils;

public class DedicatedStatelessManualByonCPUFailoverTest extends AbstractFromXenToByonGSMTest {
	
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
    public void doTest() {
    	                  
        int numberOfCpuCores = 6;
        
	    // make sure no gscs yet created
	    repetitiveAssertNumberOfGSCsAdded(0, OPERATION_TIMEOUT);
	    repetitiveAssertNumberOfGSAsAdded(1, OPERATION_TIMEOUT);	    
	    
	    final int expectedNumberOfMachines = (int)  
    		Math.ceil(numberOfCpuCores/super.getMachineProvisioningConfig().getMinimumNumberOfCpuCoresPerMachine());

	    int expectedNumberOfContainers = (int) Math.ceil(numberOfCpuCores/super.getMachineProvisioningConfig().getMinimumNumberOfCpuCoresPerMachine());
	    
	    File archive = DeploymentUtils.getArchive("servlet.war");
	    
		final ProcessingUnit pu = super.deploy(
				new ElasticStatelessProcessingUnitDeployment(archive)
	            .memoryCapacityPerContainer(1, MemoryUnit.GIGABYTES)
	            .dedicatedMachineProvisioning(getMachineProvisioningConfig()).

	            scale(new ManualCapacityScaleConfigurer()
                      .numberOfCpuCores(numberOfCpuCores)
                      .create())
	    );
	    
	    pu.waitFor(expectedNumberOfContainers);
	    	    
	    repetitiveAssertNumberOfGSCsAdded(expectedNumberOfContainers, OPERATION_TIMEOUT);
	    repetitiveAssertNumberOfGSCsRemoved(0, OPERATION_TIMEOUT);
	    
               
        GridServiceContainer container = admin.getGridServiceContainers().getContainers()[0];
        GsmTestUtils.killContainer(container);
        
        pu.waitFor(expectedNumberOfContainers);
        
 
	    repetitiveAssertNumberOfGSCsRemoved(1, OPERATION_TIMEOUT);
	    repetitiveAssertNumberOfGSCsAdded(expectedNumberOfContainers+1, OPERATION_TIMEOUT);
	    repetitiveAssertNumberOfGSAsAdded(expectedNumberOfMachines, OPERATION_TIMEOUT);
	    repetitiveAssertNumberOfGSAsRemoved(0, OPERATION_TIMEOUT);
	    
	    assertUndeployAndWait(pu);
	    
	}
}