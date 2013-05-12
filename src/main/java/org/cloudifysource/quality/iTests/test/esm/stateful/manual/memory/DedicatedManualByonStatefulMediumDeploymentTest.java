package org.cloudifysource.quality.iTests.test.esm.stateful.manual.memory;

import iTests.framework.utils.GsmTestUtils;
import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
import org.cloudifysource.quality.iTests.test.esm.AbstractFromXenToByonGSMTest;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.elastic.config.ManualCapacityScaleConfigurer;
import org.openspaces.admin.space.ElasticSpaceDeployment;
import org.openspaces.core.util.MemoryUnit;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class DedicatedManualByonStatefulMediumDeploymentTest extends AbstractFromXenToByonGSMTest {
	
	private static final int CONTAINER_CAPACITY = 256;
    private static final int HIGH_NUM_CPU = 8;
    private static final int HIGH_MEM_CAPACITY = 6000;
    
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
    
    @Test(timeOut= AbstractTestSupport.DEFAULT_TEST_TIMEOUT*2 , groups = "1" )
	public void largeDeploymentTest() {
	
	    // make sure no gscs yet created
	    repetitiveAssertNumberOfGSCsAdded(0, OPERATION_TIMEOUT);
	    repetitiveAssertNumberOfGSAsAdded(1, OPERATION_TIMEOUT);
	 
	    final ProcessingUnit pu = super.deploy(
	            new ElasticSpaceDeployment("mygrid")
	            .maxMemoryCapacity(HIGH_MEM_CAPACITY, MemoryUnit.MEGABYTES)
	            .maxNumberOfCpuCores(HIGH_NUM_CPU)
	            .memoryCapacityPerContainer(CONTAINER_CAPACITY, MemoryUnit.MEGABYTES)
	            .dedicatedMachineProvisioning(getMachineProvisioningConfig())
	            .scale(new ManualCapacityScaleConfigurer()
	            	  .memoryCapacity(HIGH_MEM_CAPACITY,MemoryUnit.MEGABYTES)
	            	  .create())
	            .dedicatedMachineProvisioning(getMachineProvisioningConfig())
	    );
	    
	    int expectedNumberOfContainers = (int) Math.ceil(HIGH_MEM_CAPACITY/(1.0*CONTAINER_CAPACITY));
		int expectedNumberOfMachines = 2; // byon machines have approx 5.8gb of ram -> 2 machines for 6gb
	    GsmTestUtils.waitForScaleToCompleteIgnoreCpuSla(pu, expectedNumberOfContainers, expectedNumberOfMachines, 2* AbstractTestSupport.DEFAULT_TEST_TIMEOUT);

	    assertUndeployAndWait(pu);
   }
}
