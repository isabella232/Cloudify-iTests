package org.cloudifysource.quality.iTests.test.esm;

import iTests.framework.utils.AssertUtils;
import iTests.framework.utils.DeploymentUtils;
import iTests.framework.utils.LogUtils;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.elastic.ElasticStatefulProcessingUnitDeployment;
import org.openspaces.admin.pu.elastic.config.ManualCapacityScaleConfig;
import org.openspaces.admin.pu.elastic.config.ManualCapacityScaleConfigurer;
import org.openspaces.core.util.MemoryUnit;
import org.testng.annotations.*;

import java.util.concurrent.TimeUnit;



public class ByonBasicEsmTest extends AbstractFromXenToByonGSMTest {
	public final static long OPERATION_TIMEOUT = 5 * 60 * 1000;

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
	
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, enabled = true)
	public void xenStatefulProccesingUnitDeploymentSimulation() throws Exception {
		LogUtils.log("simulating xen stateful proccesing unit deployment");
		
		repetitiveAssertNumberOfGSAsAdded(1, OPERATION_TIMEOUT);
		repetitiveAssertNumberOfGSCsAdded(0,OPERATION_TIMEOUT);

        ManualCapacityScaleConfig manualCapacityScaleConfig = 
            	new ManualCapacityScaleConfigurer()
    			.memoryCapacity(512, MemoryUnit.MEGABYTES)
    			.create();
        
     
        ElasticStatefulProcessingUnitDeployment deployment = 
    			new ElasticStatefulProcessingUnitDeployment(DeploymentUtils.getProcessingUnit("simpledata", "processor"))
    			.maxMemoryCapacity(512, MemoryUnit.MEGABYTES)
    			.memoryCapacityPerContainer(256,MemoryUnit.MEGABYTES)
    			.dedicatedMachineProvisioning(getMachineProvisioningConfig());
        
        deployment.scale(manualCapacityScaleConfig);
        
        // deploy pu
        final ProcessingUnit pu = deploy(deployment);
        AssertUtils.assertNotNull(pu); 
        
        // undeploy pu
        LogUtils.log("Undeploying processing unit " + pu.getName());
        boolean success = pu.undeployAndWait(OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
        AssertUtils.assertTrue("Undeployment of "+pu.getName()+"failed",success);
        LogUtils.log("Undeployed processing unit " + pu.getName());
        
        
        LogUtils.log("simulating xen stateful proccesing unit deployment passed");
	}
	
}
