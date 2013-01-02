package test.esm;

import java.io.File;
import java.util.concurrent.TimeUnit;

import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.elastic.ElasticStatefulProcessingUnitDeployment;
import org.openspaces.admin.pu.elastic.config.ManualCapacityScaleConfig;
import org.openspaces.admin.pu.elastic.config.ManualCapacityScaleConfigurer;
import org.openspaces.core.util.MemoryUnit;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import framework.utils.AssertUtils;
import framework.utils.DeploymentUtils;
import framework.utils.LogUtils;



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
		//get pu dir
        File puDir = DeploymentUtils.getArchive("processorPU.jar");      
        

        ManualCapacityScaleConfig manualCapacityScaleConfig = 
            	new ManualCapacityScaleConfigurer()
    			.memoryCapacity(512, MemoryUnit.MEGABYTES)
    			.create();
        
     
        ElasticStatefulProcessingUnitDeployment deployment = 
    			new ElasticStatefulProcessingUnitDeployment(puDir)
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
