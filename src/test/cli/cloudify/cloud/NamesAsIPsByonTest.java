package test.cli.cloudify.cloud;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.jclouds.compute.RunNodesException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import test.cli.cloudify.cloud.services.byon.ByonCloudService;
import framework.tools.SGTestHelper;
import framework.utils.ScriptUtils;




/**
 * This test installs petclinic-simple on byon after changing the byon-cloud.groovy file to contain machine names instead of IPs.
 * <p>It checks whether the bootstrap and instal have succeeded.
 * <p>Note: this test uses 3 fixed machines - 192.168.9.115, 192.168.9.116, 192.168.9.120.
 */
public class NamesAsIPsByonTest extends AbstractCloudTest{

	private ByonCloudService byonService;
	private static final String CLOUD_NAME = "byon";
	private static final String TEST_UNIQUE_NAME = "NamesAsIPsByonTest";
	private String namesList = "pc-lab107,pc-lab108,pc-lab109";
	public final static long MY_OPERATION_TIMEOUT = 1 * 60 * 1000;
	
	@BeforeClass(enabled = true)
	public void before() throws RunNodesException, IOException, InterruptedException {
		
		// get the cached service
		setCloudService(CLOUD_NAME, TEST_UNIQUE_NAME, false);
		byonService = (ByonCloudService)getService();
		if ((byonService != null) && byonService.isBootstrapped()) {
			byonService.teardownCloud(); // tear down the existing byon cloud since we need a new bootstrap			
		}
		
		byonService.setMachinePrefix(this.getClass().getName());

		// replace the default bootstap-management.sh with a multicast version one
		File standardBootstrapManagement = new File(byonService.getPathToCloudFolder() + "/upload", "bootstrap-management.sh");
		File bootstrapManagementWithMulticast = new File(SGTestHelper.getSGTestRootDir() + "/apps/cloudify/cloud/byon/bootstrap-management-multicast-and-byon-java-home.sh");
		Map<File, File> filesToReplace = new HashMap<File, File>();
		filesToReplace.put(standardBootstrapManagement, bootstrapManagementWithMulticast);
		byonService.addFilesToReplace(filesToReplace);
		
		byonService.bootstrapCloud();
		
		/*LogUtils.log("creating admin");
		AdminFactory factory = new AdminFactory();
		factory.addGroup(LOOKUPGROUP);
		admin = factory.createAdmin();*/
		
		//AssertUtils.assertTrue("webui is not up - bootstrap failed", admin.getProcessingUnits().getProcessingUnit("webui") != null);
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, enabled = true)
	public void testPetclinic() throws IOException, InterruptedException{
		
		//setCloudService(cloudName, CLOUD_SERVICE_UNIQUE_NAME, false);
		//byonService = (ByonCloudService)getService();
		/*if ((byonService != null) && byonService.isBootstrapped()) {
			byonService.teardownCloud(); // tear down the existing byon cloud since we need a new bootstrap			
		}
		
		byonService.setMachinePrefix(this.getClass().getName());
		byonService.bootstrapCloud();*/
				
		installApplicationAndWait(ScriptUtils.getBuildPath() + "/recipes/apps/petclinic-simple", "petclinic");
		
		/*AssertUtils.assertTrue("petclinic.mongod is not up - install failed", admin.getProcessingUnits().getProcessingUnit("petclinic.mongod") != null);		
		AssertUtils.assertTrue("petclinic.tomcat is not up - install failed", admin.getProcessingUnits().getProcessingUnit("petclinic.tomcat") != null);*/
	}
	
	@AfterClass(alwaysRun = true)
	public void teardown() throws IOException, InterruptedException {
		
		//restore byon-cloud.groovy
		/*originialByonDslFile.delete();
		FileUtils.moveFile(backupByonDslFile, originialByonDslFile);
		
		//restore bootstrap management file
		originialBootstrapManagement.delete();
		FileUtils.moveFile(backupStartManagementFile, originialBootstrapManagement);*/
		
		try{
			uninstallApplicationAndWait("petclinic");
		} catch(Exception e) {
			//TODO : log
		} finally {
			byonService.teardownCloud();	
		}
			
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
	