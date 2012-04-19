package test.cli.cloudify.cloud;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import org.apache.commons.io.FileUtils;
import org.cloudifysource.dsl.cloud.Cloud;
import org.cloudifysource.dsl.internal.DSLException;
import org.cloudifysource.dsl.internal.ServiceReader;
import org.openspaces.admin.AdminFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.cli.cloudify.cloud.byon.ByonCloudService;
import framework.tools.SGTestHelper;
import framework.utils.IOUtils;
import framework.utils.LogUtils;
import framework.utils.ScriptUtils;

public class MultipleMachineTemplatesTest extends AbstractCloudTest{

	private URL petClinicUrl;
	private ByonCloudService service;
	private String cloudName = "byon";
	private static final String LOOKUPGROUP = "byon_group";
	private ExecutorService threadPool;
	private String restUrl;
	private File byonUploadDir = new File(ScriptUtils.getBuildPath() , "tools/cli/plugins/esc/byon/upload");
	private File backupStartManagementFile = new File(byonUploadDir, "bootstrap-management.backup");
	private File originialBootstrapManagement = new File(ScriptUtils.getBuildPath() + "/tools/cli/plugins/esc/byon/upload/bootstrap-management.sh");
	private File byonCloudConfDir = new File(ScriptUtils.getBuildPath() , "tools/cli/plugins/esc/byon");
	private File backupCloudConfFile = new File(byonCloudConfDir, "byon-cloud.backup");
	private File originialCloudConf = new File(byonCloudConfDir, "byon-cloud.groovy");
	private File petClinicDir = new File(ScriptUtils.getBuildPath() , "recipes/apps/petclinic");
	private Cloud readCloud;
	private static final String TEMPLATE_1 = "";
	private static final String TEMPLATE_2 = "";
	private static final String TEMPLATE_3 = "";
	private static final String DEFAULT_MACHINE_TEMPLATE = "SMALL_LINUX";

	@BeforeMethod(enabled = true)
	public void before() throws IOException, InterruptedException, DSLException {

		// get the default service
		service = (ByonCloudService) getDefaultService(cloudName);
		if (service.isBootstrapped()) {
			service.teardownCloud(); // tear down the existing byon cloud since we need a new bootstrap			
		}

		service = new ByonCloudService();
		service.setMachinePrefix(this.getClass().getName());

		// replace the default bootstrap-management.sh with a multicast version one
		// first make a backup of the original file
		FileUtils.copyFile(originialBootstrapManagement, backupStartManagementFile);

		// copy multicast bootstrap to upload dir as bootstrap-management.sh
		File bootstrapManagementWithMulticast = new File(SGTestHelper.getSGTestRootDir() + "recipes/apps/cloudify/cloud/byon/bootstrap-management-with-multicast.sh");

		FileUtils.deleteQuietly(originialBootstrapManagement);
		FileUtils.copyFile(bootstrapManagementWithMulticast, new File(byonUploadDir, "bootstrap-management.sh"));

		// replace the default byon-cloud.groovy with a multiple templates one
		// first make a backup of the original file
		readCloud = ServiceReader.readCloud(originialCloudConf);
		String cloudifyUrl = readCloud.getProvider().getCloudifyUrl();

		FileUtils.copyFile(originialCloudConf, backupCloudConfFile);
		// copy multi template bootstrap to upload dir as byon-cloud.groovy
		File byonCloudWithMultiMachineTemplates = new File(SGTestHelper.getSGTestRootDir() + "recipes/apps/cloudify/cloud/byon/byon-cloud.groovy");

		FileUtils.deleteQuietly(originialCloudConf);
		File newCloudConfFile = new File(byonCloudConfDir, "byon-cloud.groovy");
		FileUtils.copyFile(byonCloudWithMultiMachineTemplates, newCloudConfFile);
		Map<String,String> replaceMap = new HashMap<String,String>();
		replaceMap.put("cloudify.zip", cloudifyUrl);
		replaceMap.put("1.1.1.1", TEMPLATE_1);
		replaceMap.put("2.2.2.2", TEMPLATE_2);
		replaceMap.put("3.3.3.3", TEMPLATE_3);

		IOUtils.replaceTextInFile(newCloudConfFile.getAbsolutePath(), replaceMap);

		replaceMachineTemplateInService("mongos", "TEMPLATE_1");
		replaceMachineTemplateInService("mongod", "TEMPLATE_2");
		//replaceMachineTemplateInService("mongoConfig", "SMALL_LINUX");
		replaceMachineTemplateInService("tomcat", "TEMPLATE_3");
		
		replaceByonLookupGroup(LOOKUPGROUP);

		service.bootstrapCloud();

		if (service.getRestUrls() == null) {
			Assert.fail("Test failed becuase the cloud was not bootstrapped properly");
		}

		setService(service);

		LogUtils.log("creating admin");
		AdminFactory factory = new AdminFactory();
		factory.addGroup(LOOKUPGROUP);
		admin = factory.createAdmin();

		restUrl = service.getRestUrls()[0];
		String hostIp = restUrl.substring(0, restUrl.lastIndexOf(':'));
		petClinicUrl = new URL(hostIp + ":8080/petclinic-mongo/");

	}

	private void replaceMachineTemplateInService(String serviceName, String newTemplate) throws IOException {
		
		File originalService = new File(petClinicDir, serviceName + "/" + serviceName + "-service.groovy");
		FileUtils.copyFile(originalService, new File(originalService.getAbsolutePath() + ".backup"));
		IOUtils.replaceTextInFile(originalService.getAbsolutePath(), DEFAULT_MACHINE_TEMPLATE , newTemplate);
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, enabled = false)
	public void testPetclinic() throws Exception {
		LogUtils.log("installing application petclinic on " + cloudName);
		installApplicationAndWait(ScriptUtils.getBuildPath() + "/examples/petclinic", "petclinic");

		
		String hostAddressToCompare = admin.getProcessingUnits().getProcessingUnit("petclinic.mongod")
				.getInstances()[0].getMachine().getHostAddress();
		
		Assert.assertEquals(hostAddressToCompare, TEMPLATE_2);
		
		hostAddressToCompare = admin.getProcessingUnits().getProcessingUnit("petclinic.mongos")
				.getInstances()[0].getMachine().getHostAddress();
		Assert.assertEquals(hostAddressToCompare, TEMPLATE_1);
		
		hostAddressToCompare = admin.getProcessingUnits().getProcessingUnit("petclinic.tomcat")
				.getInstances()[0].getMachine().getHostAddress();
		Assert.assertEquals(hostAddressToCompare, TEMPLATE_3);
		
		
	}


	@AfterMethod(alwaysRun = true)
	public void teardown() throws IOException {
		threadPool.shutdownNow();
		try {
			service.teardownCloud();
		}
		catch (Throwable e) {
			LogUtils.log("caught an exception while tearing down " + service.getCloudName(), e);
			sendTeardownCloudFailedMail(cloudName, e);
		}
		putService(new ByonCloudService());
		restoreOriginalBootstrapManagementFile();
		restoreOriginalByonCloudFile();
		restoreOriginalServiceFile("mongos");
		restoreOriginalServiceFile("mongod");
		restoreOriginalServiceFile("tomcat");



	}

	private void replaceByonLookupGroup(String group) throws IOException {
		File originalStartManagementFile = new File(byonUploadDir, "bootstrap-management.sh");

		String toReplace = "setenv.sh\"\\s\\s\\s";
		String toAdd = "sed -i \"1i export LOOKUPGROUPS="+ group +"\" setenv.sh || error_exit \\$? \"Failed updating setenv.sh\"\n";
		IOUtils.replaceTextInFile(originalStartManagementFile.getAbsolutePath(), toReplace, "setenv.sh\"" + "\r\n" + toAdd + "\r\n");

	}



	private void restoreOriginalBootstrapManagementFile() throws IOException {
		originialBootstrapManagement.delete();
		FileUtils.moveFile(backupStartManagementFile, originialBootstrapManagement);

	}

	private void restoreOriginalByonCloudFile() throws IOException {
		originialCloudConf.delete();
		FileUtils.moveFile(backupCloudConfFile, originialCloudConf);

	}
	
	private void restoreOriginalServiceFile(String serviceName) throws IOException {
		
		File originalService = new File(petClinicDir, serviceName + "/" + serviceName + "-service.groovy");
		originalService.delete();
		FileUtils.moveFile(new File(originalService + ".backup"), originalService);
	}

}



