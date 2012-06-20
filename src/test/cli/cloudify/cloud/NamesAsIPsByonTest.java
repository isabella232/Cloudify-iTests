package test.cli.cloudify.cloud;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.jclouds.compute.RunNodesException;
import org.openspaces.admin.AdminFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import test.cli.cloudify.cloud.services.byon.ByonCloudService;
import framework.tools.SGTestHelper;
import framework.utils.AssertUtils;
import framework.utils.LogUtils;
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
	private String namesList = "pc-lab95,pc-lab96,pc-lab100";
	public final static long MY_OPERATION_TIMEOUT = 1 * 60 * 1000;
	public static final String IP_LIST_PROPERTY = "ipList";
	
	@BeforeClass(enabled = true)
	public void before() throws RunNodesException, IOException, InterruptedException {
		
		System.setProperty(IP_LIST_PROPERTY, namesList);
		
		// get the cached service
		setCloudService(CLOUD_NAME, TEST_UNIQUE_NAME, false);
		byonService = (ByonCloudService)getService();
		if ((byonService != null) && byonService.isBootstrapped()) {
			byonService.teardownCloud(); // tear down the existing byon cloud since we need a new bootstrap			
		}
		
		byonService.setMachinePrefix(this.getClass().getName());

		// replace the default bootstap-management.sh with a multicast version one
		File standardBootstrapManagement = new File(byonService.getPathToCloudFolder() + "/upload", "bootstrap-management.sh");
		File customBootstrapManagement = new File(SGTestHelper.getSGTestRootDir() + "/apps/cloudify/cloud/byon/bootstrap-management-" + byonService.getServiceFolder() + ".sh");
		Map<File, File> filesToReplace = new HashMap<File, File>();
		filesToReplace.put(standardBootstrapManagement, customBootstrapManagement);
		byonService.addFilesToReplace(filesToReplace);
		
		byonService.bootstrapCloud();
		
		LogUtils.log("creating admin");
		AdminFactory factory = new AdminFactory();
		factory.addGroup(TEST_UNIQUE_NAME);
		admin = factory.createAdmin();
		
		//AssertUtils.assertTrue("webui is not up - bootstrap failed", admin.getProcessingUnits().getProcessingUnit("webui") != null);
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, enabled = true)
	public void testPetclinic() throws IOException, InterruptedException{
		
		installApplicationAndWait(ScriptUtils.getBuildPath() + "/recipes/apps/petclinic-simple", "petclinic");
		
		//TODO : edit this, so if it fails it won't be on NPE!
		AssertUtils.assertTrue("petclinic.mongod is not up - install failed", admin.getProcessingUnits().getProcessingUnit("petclinic.mongod") != null);		
		AssertUtils.assertTrue("petclinic.tomcat is not up - install failed", admin.getProcessingUnits().getProcessingUnit("petclinic.tomcat") != null);
	}
	
	@AfterClass(alwaysRun = true)
	public void teardown() throws IOException, InterruptedException {
		
		if (admin != null) {
			admin.close();
			admin = null;
		}
		
		try{
			uninstallApplicationAndWait("petclinic");
		} catch(Exception e) {
			//TODO : log
		} finally {
			System.clearProperty(IP_LIST_PROPERTY);
			byonService.teardownCloud();
		}
			
	}

}
	