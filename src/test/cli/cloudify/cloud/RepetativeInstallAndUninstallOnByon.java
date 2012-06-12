package test.cli.cloudify.cloud;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.jclouds.compute.RunNodesException;
import org.openspaces.admin.AdminFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

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

	private File byonCloudConfDir = new File(ScriptUtils.getBuildPath() , "tools/cli/plugins/esc/byon");
	private File byonUploadDir = new File(byonCloudConfDir , "upload");
	private File originialBootstrapManagement = new File(byonUploadDir, "bootstrap-management.sh");
	private File backupStartManagementFile = new File(byonUploadDir, "bootstrap-management.sh.backup");
	private ByonCloudService byonService;
	private String cloudName = BYON;
	private final static int REPETITIONS = 3;
	private static final String LOOKUPGROUP = "byon_group";
	private static final String CLOUD_SERVICE_UNIQUE_NAME = "RepetativeInstallAndUninstallOnByon";
	
	@BeforeClass(enabled = true)
	public void before() throws RunNodesException, IOException, InterruptedException{
		
		backupAndReplaceOriginalFile(originialBootstrapManagement,SGTestHelper.getSGTestRootDir() + "/apps/cloudify/cloud/byon/bootstrap-management-multicast-and-byon-java-home.sh");

		LogUtils.log("creating admin");
		AdminFactory factory = new AdminFactory();
		factory.addGroup(LOOKUPGROUP);
		admin = factory.createAdmin();
		
		setCloudService(cloudName, CLOUD_SERVICE_UNIQUE_NAME, false);
		byonService = (ByonCloudService)getService();
		if ((byonService != null) && byonService.isBootstrapped()) {
			byonService.teardownCloud(); // tear down the existing byon cloud since we need a new bootstrap			
		}
		
		LogUtils.log("bootstrapping byon cloud");
		byonService.setMachinePrefix(this.getClass().getName());
		byonService.bootstrapCloud();		
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, enabled = true)
	public void testPetclinic() throws Exception{
			
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
		
		//restore bootstrap management file
		originialBootstrapManagement.delete();
		FileUtils.moveFile(backupStartManagementFile, originialBootstrapManagement);
		
		LogUtils.log("tearing down byon cloud");
		byonService.teardownCloud();
		
	}
	
	private File backupAndReplaceOriginalFile(File originalFile, String replacementFilePath) throws IOException {
		// replace the default originalFile with a different one
		// first make a backup of the original file
		File backupFile = new File(originalFile.getAbsolutePath() + ".backup");
		FileUtils.copyFile(originalFile, backupFile);

		// copy replacement file to upload dir as the original file's name
		File replacementFile = new File(replacementFilePath);

		FileUtils.deleteQuietly(originalFile);
		File newOriginalFile = new File(originalFile.getParent(), originalFile.getName());
		FileUtils.copyFile(replacementFile, newOriginalFile);
		
		return newOriginalFile;
	}
}
	