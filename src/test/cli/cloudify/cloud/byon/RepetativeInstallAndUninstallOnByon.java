package test.cli.cloudify.cloud.byon;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.jclouds.compute.RunNodesException;
import org.openspaces.admin.AdminFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import test.cli.cloudify.cloud.AbstractCloudTest;
import test.cli.cloudify.cloud.services.byon.ByonCloudService;
import framework.tools.SGTestHelper;
import framework.utils.LogUtils;
import framework.utils.ScriptUtils;




/**
 * <p> 1. bootstrap byon.
 * <p> 2. repeat steps 3-4 three times.
 * <p> 3. install petclinic-simple and assert installation.
 * <p> 4. uninstall petclinic-simple and assert successful uninstall.
 * 
 */
public class RepetativeInstallAndUninstallOnByon extends AbstractCloudTest{

	private ByonCloudService byonService;
	private static final String CLOUD_NAME = BYON;
	private final static int REPETITIONS = 3;
	private static final String CLOUD_SERVICE_UNIQUE_NAME = "RepetativeInstallAndUninstallOnByon";
	
	@BeforeClass(enabled = true)
	public void before() throws RunNodesException, IOException, InterruptedException{
		
		// get the cached service
		setCloudService(CLOUD_NAME, CLOUD_SERVICE_UNIQUE_NAME, false);
		byonService = (ByonCloudService)getService();
		if ((byonService != null) && byonService.isBootstrapped()) {
			byonService.teardownCloud(); // tear down the existing byon cloud since we need a new bootstrap			
		}
		
		// replace the default bootstrap-management.sh with a custom version one
		File standardBootstrapManagement = new File(byonService.getPathToCloudFolder() + "/upload", "bootstrap-management.sh");
		File bootstrapManagementWithMulticast = new File(SGTestHelper.getSGTestRootDir() + "/apps/cloudify/cloud/byon/bootstrap-management-" + byonService.getServiceFolder() + ".sh");
		Map<File, File> filesToReplace = new HashMap<File, File>();
		filesToReplace.put(standardBootstrapManagement, bootstrapManagementWithMulticast);
		byonService.addFilesToReplace(filesToReplace);

		LogUtils.log("creating admin");
		AdminFactory factory = new AdminFactory();
		factory.addGroup(CLOUD_SERVICE_UNIQUE_NAME);
		admin = factory.createAdmin();
		
		LogUtils.log("bootstrapping byon cloud");
		byonService.setMachinePrefix(this.getClass().getName());
		byonService.bootstrapCloud();
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, enabled = true)
	public void testPetclinic() throws Exception {
			
		for(int i=0; i < REPETITIONS; i++){
			LogUtils.log("petclinic install number " + (i+1));
			installApplicationAndWait(ScriptUtils.getBuildPath() + "/recipes/apps/petclinic-simple", "petclinic");

			Assert.assertTrue(admin.getProcessingUnits().getProcessingUnit("petclinic.mongod").waitFor(1, 10, TimeUnit.MINUTES), "petclinic.mongod is not up - install failed");		
			Assert.assertTrue(admin.getProcessingUnits().getProcessingUnit("petclinic.tomcat").waitFor(1, 10, TimeUnit.MINUTES), "petclinic.tomcat is not up - install failed");
			
			LogUtils.log("petclinic uninstall number " + (i+1));
			uninstallApplicationAndWait("petclinic");
			Assert.assertTrue(admin.getProcessingUnits().getProcessingUnit("petclinic.mongod") == null, "petclinic.mongod is up - uninstall failed");		
			Assert.assertTrue(admin.getProcessingUnits().getProcessingUnit("petclinic.tomcat") == null, "petclinic.tomcat is up - uninstall failed");
		}
	}
	
	@AfterClass(alwaysRun = true)
	public void teardown() throws IOException, InterruptedException {

		if (admin != null) {
			admin.close();
			admin = null;
		}
		
		LogUtils.log("tearing down byon cloud");
		byonService.teardownCloud();
		
	}
}
	