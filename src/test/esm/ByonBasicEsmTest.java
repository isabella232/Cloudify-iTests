package test.esm;

import java.io.File;
import java.util.concurrent.TimeUnit;

import org.cloudifysource.dsl.cloud.Cloud;
import org.cloudifysource.dsl.cloud.CloudTemplate;
import org.cloudifysource.esc.driver.provisioning.CloudifyMachineProvisioningConfig;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.elastic.ElasticMachineProvisioningConfig;
import org.openspaces.admin.pu.elastic.ElasticStatefulProcessingUnitDeployment;
import org.openspaces.admin.pu.elastic.config.ManualCapacityScaleConfig;
import org.openspaces.admin.pu.elastic.config.ManualCapacityScaleConfigurer;
import org.openspaces.core.util.MemoryUnit;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import test.cli.cloudify.cloud.byon.AbstractByonCloudTest;
import test.cli.cloudify.cloud.services.byon.ByonCloudService;

import com.gigaspaces.webuitf.util.LogUtils;

import framework.utils.AssertUtils;
import framework.utils.DeploymentUtils;



public class ByonBasicEsmTest extends AbstractByonCloudTest {
	public final static long OPERATION_TIMEOUT = 5 * 60 * 1000;

	@BeforeClass
	public void bootstrap() throws Exception {
		super.bootstrap();
	}
	
	@AfterClass(alwaysRun = true)
	protected void teardown() throws Exception {
		super.teardown(admin);
		
	}
	
	@Override
	protected String getCloudName() {
		return "byon-xap";
	}
	
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, enabled = true)
	public void xenStatefulProccesingUnitDeploymentSimulation() throws Exception {
		LogUtils.log("simulating xen stateful proccesing unit deployment");
		
		AssertUtils.assertEquals(0, admin.getGridServiceContainers().getSize());	
		admin.getGridServiceAgents().waitFor(1,OPERATION_TIMEOUT,TimeUnit.MILLISECONDS);
		AssertUtils.assertEquals(1, admin.getGridServiceAgents().getSize());
		
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
        final ProcessingUnit pu = admin.getGridServiceManagers().getManagers()[0].deploy(deployment);
        AssertUtils.assertNotNull(pu); 
        
        // undeploy pu
        LogUtils.log("Undeploying processing unit " + pu.getName());
        boolean success = pu.undeployAndWait(OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
        AssertUtils.assertTrue("Undeployment of "+pu.getName()+"failed",success);
        LogUtils.log("Undeployed processing unit " + pu.getName());
        
        
        LogUtils.log("simulating xen stateful proccesing unit deployment passed");
	}
	
	//config the machine
	private ElasticMachineProvisioningConfig getMachineProvisioningConfig() {
		String templateName = "SMALL_LINUX";
		ByonCloudService cloudService = getService();
		Cloud cloud = cloudService.getCloud();
		final CloudTemplate template = cloud.getTemplates().get(templateName);
		CloudTemplate managementTemplate = cloud.getTemplates().get(cloud.getConfiguration().getManagementMachineTemplate());
		managementTemplate.getRemoteDirectory();
		final CloudifyMachineProvisioningConfig config = new CloudifyMachineProvisioningConfig(
				cloud, template, templateName,
				managementTemplate.getRemoteDirectory());		
		return config;
	}
	

}
