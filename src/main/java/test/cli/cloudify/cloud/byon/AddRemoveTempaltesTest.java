package test.cli.cloudify.cloud.byon;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import junit.framework.Assert;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.packaging.Packager;
import org.openspaces.admin.AdminFactory;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.openspaces.admin.pu.ProcessingUnits;
import org.openspaces.pu.service.ServiceDetails;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import test.cli.cloudify.CommandTestUtils;
import test.cli.cloudify.cloud.services.CloudServiceManager;
import test.cli.cloudify.cloud.services.byon.ByonCloudService;
import framework.utils.IOUtils;
import framework.utils.LogUtils;

/**
 * 
 * @author yael
 *
 */
public class AddRemoveTempaltesTest extends AbstractByonCloudTest {

	/**
	 * Change to false to use the DESAULT machines (pc-lab).
	 * If it is true, the test will use machines from the {@link ByonCloudService}.DEFAULT_MACHINES
	 */
	private final boolean USE_NODES_FROM_SGTEST_BYON_POOL = true;
	/** 
	 * Change to false to avoid bootstrap
	 */
	private final boolean BOOTSTRAP = true;
	/** 
	 * Change to false to avoid teardown
	 */
	private final boolean TEARDOWN = true;

	private final String DESAULT_MNG_NODE_IP = "pc-lab111";
	private final String DESAULT_SERVICE1_NODE_IP = "pc-lab113";
	private final String DESAULT_SERVICE5_NODE_IP = "pc-lab114";

	private String mngMachineIP;
	private String service1MachineIP;
	private String service5MachineIP;

	private final String SERVICE1_PATH = 
			CommandTestUtils.getPath("src/main/resources/apps/USM/usm/services/simpleWithTemplates/simple1");
	private final String SERVICE1_NAME = "simple1";
	private final String SERVICE1_UPLOAD_DIR_NAME = "upload1";

	private final String SERVICE5_PATH = 
			CommandTestUtils.getPath("src/main/resources/apps/USM/usm/services/simpleWithTemplates/simple5");
	private final String SERVICE5_NAME = "simple5";
	private final String SERVICE5_UPLOAD_DIR_NAME = "upload5";

	private final String TEMPLATES1_DIR_PATH = 
			CommandTestUtils.getPath("src/main/resources/apps/USM/usm/services/simpleWithTemplates/templateFiles1");
	private final String TEMPLATES1_PROPERTIES_PATH = TEMPLATES1_DIR_PATH + "/simple1-template.properties";

	private final String TEMPLATES5_DIR_PATH = 
			CommandTestUtils.getPath("src/main/resources/apps/USM/usm/services/simpleWithTemplates/templateFiles5");
	private final String TEMPLATES5_PROPERTIES_PATH = TEMPLATES5_DIR_PATH + "/simple5-template.properties";

	private final String TEMPLATES2_DIR_PATH = 
			CommandTestUtils.getPath("src/main/resources/apps/USM/usm/services/simpleWithTemplates/templateFiles2");

	private final String TEMPLATES3_DIR_PATH = 
			CommandTestUtils.getPath("src/main/resources/apps/USM/usm/services/simpleWithTemplates/templateFiles3");

	private final String NO_UPLOAD_TEMPLATE_DIR_PATH = 
			CommandTestUtils.getPath("src/main/resources/apps/USM/usm/services/simpleWithTemplates/noUploadDir");

	private final String DEFAULT_TEMPLATE = "SMALL_LINUX";
	private final String[] DEFAULT_TEMPLATES_EXIST = {DEFAULT_TEMPLATE};
	private final String[] TEMPLATES1_SUCCESSFUL_LIST = 
		{
			DEFAULT_TEMPLATE,
			"SIMPLE_TEMPLATE1",
			"SIMPLE_TEMPLATE2"		
		};
	private final String[] DUPLICATES_SUCCESSFUL_LIST = 
		{
			DEFAULT_TEMPLATE,
			"SIMPLE_TEMPLATE1",
			"SIMPLE_TEMPLATE2",
			"SIMPLE_TEMPLATE3"		
		};
	private final String[] TEMPLATES5_SUCCESSFUL_LIST = 
		{
			DEFAULT_TEMPLATE,
			"SIMPLE_TEMPLATE5"		
		};
	private final String[] DUPLICATES_FAILURE_LIST = 
		{
			"SIMPLE_TEMPLATE1"		
		};
	private final String[] TEMPLATES3_FAILURE_LIST = 
		{
			"SIMPLE_TEMPLATE1"		
		};
	private final String[] NO_UPLOAD_FAILURE_LIST = 
		{
			"SIMPLE_TEMPLATE1",
			"SIMPLE_TEMPLATE2"
		};

	@Override
	@BeforeClass(alwaysRun = true)
	protected void bootstrap() throws Exception {

		ByonCloudService service = new ByonCloudService();
		String[] machines = service.getMachines();
		if (USE_NODES_FROM_SGTEST_BYON_POOL) {
			mngMachineIP = machines[0];
			service1MachineIP = machines[1];
			service5MachineIP = machines[5];
		} else {
			mngMachineIP = DESAULT_MNG_NODE_IP;
			service1MachineIP = DESAULT_SERVICE1_NODE_IP;
			service5MachineIP = DESAULT_SERVICE5_NODE_IP;
		}
		LogUtils.log("Updating MNG machine IP: " + mngMachineIP);
		LogUtils.log("Updating service1 machine IP: " + service1MachineIP);
		LogUtils.log("Updating service5 machine IP: " + service5MachineIP);

		service.setIpList(mngMachineIP);

		if (BOOTSTRAP) {
			super.bootstrap(service);
		} else {
			this.cloudService = CloudServiceManager.getInstance().getCloudService(this.getCloudName());
			AdminFactory factory = new AdminFactory();
			factory.addLocators(mngMachineIP + ":" + CloudifyConstants.DEFAULT_LUS_PORT);
			admin = factory.createAdmin();
		}
	}

	private void updateTemplatesProperties(String machineIP, String propertiesFile) throws IOException {
			File templatePropsFile = new File(propertiesFile);		
			Properties props = new Properties();
			props.setProperty("node_ip", '"' + machineIP + '"');
			props.setProperty("node_id", "\"byon-pc-lab{0}\"");
			IOUtils.writePropertiesToFile(props, templatePropsFile);
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, enabled = true)
	public void addTempalteAndInstallService() throws IOException, InterruptedException {
		updateTemplatesProperties(service1MachineIP, TEMPLATES1_PROPERTIES_PATH);
		addTempaltes(TEMPLATES1_DIR_PATH, TEMPLATES1_SUCCESSFUL_LIST, null);
		try {
			installService(SERVICE1_PATH, SERVICE1_NAME, false);
			assertRightUploadDir(SERVICE1_NAME, SERVICE1_UPLOAD_DIR_NAME);
		} finally {		
			try {
				uninstallServiceIfFound(SERVICE1_NAME);
			} finally {
				removeTempaltes(TEMPLATES1_SUCCESSFUL_LIST, DEFAULT_TEMPLATES_EXIST);
			}
		}
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, enabled = true)
	public void addTempaltesUsingRestAPI() 
			throws IllegalStateException, IOException, InterruptedException {
		updateTemplatesProperties(service5MachineIP, TEMPLATES5_PROPERTIES_PATH);
		File templatesFolder = new File(TEMPLATES5_DIR_PATH);
		File zipFile = Packager.createZipFile("templates", templatesFolder);
		final FileBody body = new FileBody(zipFile);
		final MultipartEntity reqEntity = new MultipartEntity();
		reqEntity.addPart(CloudifyConstants.TEMPLATES_DIR_PARAM_NAME, body);
		// create HttpPost
		String postCommand = getRestUrl() + "/service/templates/";
		final HttpPost httppost = new HttpPost(postCommand);
		httppost.setEntity(reqEntity);
		// execute
		final DefaultHttpClient httpClient = new DefaultHttpClient();

		HttpResponse response = null;
		try {
			response = httpClient.execute(httppost);
		} catch (Exception e) {
			e.printStackTrace();
			AssertFail(e.getMessage());
		}
		try {
			Assert.assertEquals(200, response.getStatusLine().getStatusCode());
			assertListTempaltes(TEMPLATES5_SUCCESSFUL_LIST);
			installService(SERVICE5_PATH, SERVICE5_NAME, false);
			assertRightUploadDir(SERVICE5_NAME, SERVICE5_UPLOAD_DIR_NAME);
		} finally {
			uninstallServiceIfFound(SERVICE5_NAME);
			removeTempaltes(TEMPLATES5_SUCCESSFUL_LIST, DEFAULT_TEMPLATES_EXIST);
			assertListTempaltes(DEFAULT_TEMPLATES_EXIST);
		}
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, enabled = true)
	public void addExistAndNotExistTempaltes() {
		try {
			addTempaltes(TEMPLATES1_DIR_PATH, TEMPLATES1_SUCCESSFUL_LIST, null);
			addTempaltes(TEMPLATES2_DIR_PATH, DUPLICATES_SUCCESSFUL_LIST, DUPLICATES_FAILURE_LIST);
		} finally {
			removeTempaltes(DUPLICATES_SUCCESSFUL_LIST, DEFAULT_TEMPLATES_EXIST);
		}
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, enabled = true)
	public void removeTemplateAndTryToInstallService() throws IOException, InterruptedException {
		addTempaltes(TEMPLATES1_DIR_PATH, TEMPLATES1_SUCCESSFUL_LIST, null);
		String[] expectedAfterRemoved = {"SIMPLE_TEMPLATE2", DEFAULT_TEMPLATE};
		removeTempalte("SIMPLE_TEMPLATE1", false, expectedAfterRemoved);
		try {
			installService(SERVICE1_PATH, SERVICE1_NAME, true);
		} catch (Exception e) {
			System.out.println("failed to isntall service as expected! error was " + e);
			uninstallServiceIfFound(SERVICE1_NAME);
		} finally {	
			removeTempalte("SIMPLE_TEMPLATE2", false, DEFAULT_TEMPLATES_EXIST);
		}
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, enabled = true)
	public void removeNotExistTemplate() {
		removeTempalte("SIMPLE_TEMPLATE1", true, DEFAULT_TEMPLATES_EXIST);
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, enabled = true)
	public void illegalDuplicateTempaltes() {
		try {
			addTempaltes(TEMPLATES1_DIR_PATH, TEMPLATES1_SUCCESSFUL_LIST, null);
			addTempaltes(TEMPLATES3_DIR_PATH, DUPLICATES_SUCCESSFUL_LIST, TEMPLATES3_FAILURE_LIST);
		} finally {
			removeTempaltes(DUPLICATES_SUCCESSFUL_LIST, DEFAULT_TEMPLATES_EXIST);
		}
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, enabled = true)
	public void illegalTempalteWithoutLocalUploadDir() {
		addTempaltes(NO_UPLOAD_TEMPLATE_DIR_PATH, DEFAULT_TEMPLATES_EXIST, NO_UPLOAD_FAILURE_LIST);
		assertListTempaltes(DEFAULT_TEMPLATES_EXIST);
	}

	@Override
	@AfterClass(alwaysRun = true)
	protected void teardown() throws Exception {
		if (TEARDOWN) {
			super.teardown();
		}
	}

	private void installService(String servicePath, String serviceName, boolean expectToFail) {
		try {
			installServiceAndWait(servicePath, serviceName, 5 , expectToFail);
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	private String addTempaltes(String templatesFolder, String[] expectedTemplatesAfterAdding, 
			String[] expectedFailedToAddTemplates) {
		String command = "connect " + getRestUrl() + ";add-templates " + templatesFolder;
		String output = null;
		try {
			if (expectedFailedToAddTemplates != null && expectedFailedToAddTemplates.length > 0) {
				output = CommandTestUtils.runCommandExpectedFail(command);
			} else {
				output = CommandTestUtils.runCommandAndWait(command);
			}
			assertListTempaltes(expectedTemplatesAfterAdding);
		} catch (final Exception e) {
			LogUtils.log(e.getMessage(), e);
			Assert.fail(e.getMessage());
		}
		return output;
	}

	private void removeTempaltes(String[] tempaltes, String[] expectedAfterRemove) {
		for (String templateName : tempaltes) {
			if (DEFAULT_TEMPLATE.equals(templateName)) {
				continue;
			}
			removeTempalte(templateName, false, null);
		}
		assertListTempaltes(expectedAfterRemove);
	}

	private void removeTempalte(String templateName, boolean expectToFail, String[] expectedAfterRemove) {
		String command = "connect " + getRestUrl() + ";remove-template " + templateName;
		try {
			if (expectToFail) {
				CommandTestUtils.runCommandExpectedFail(command);
			} else {
				CommandTestUtils.runCommandAndWait(command);
			}
			if(expectedAfterRemove != null && expectedAfterRemove.length > 0) {
				assertListTempaltes(expectedAfterRemove);
			}
		} catch (final Exception e) {
			LogUtils.log(e.getMessage(), e);
			Assert.fail(e.getMessage());
		}
	}

	private void assertListTempaltes(String[] expectedTempaltes) {
		List<String> expectedTemplatesList = new LinkedList<String>();
		if (expectedTempaltes != null) {
			expectedTemplatesList = Arrays.asList(expectedTempaltes);
		}
		String command = "connect " + getRestUrl() + ";list-templates";
		try {
			String output = CommandTestUtils.runCommandAndWait(command);
			List<String> templateNames = getTemplateNames(output);
			assertEquals("Expected tempaltes: " + expectedTemplatesList + ", but was: " 
					+ templateNames, expectedTemplatesList.size(), templateNames.size());
			for (String templateName : expectedTemplatesList) {
				Assert.assertTrue(templateNames.contains(templateName));
			}
		} catch (final Exception e) {
			LogUtils.log(e.getMessage(), e);
			Assert.fail(e.getMessage());
		}
	}

	private void assertRightUploadDir(String serviceName, String expectedUploadDirName) {
		ProcessingUnits processingUnits = admin.getProcessingUnits();
		ProcessingUnit processingUnit = processingUnits.getProcessingUnit("default." + serviceName);
		ProcessingUnitInstance processingUnitInstance = processingUnit.getInstances()[0];
		Collection<ServiceDetails> detailes = processingUnitInstance.getServiceDetailsByServiceId().values();		
		final Map<String, Object> allDetails = new HashMap<String, Object>();
		for (final ServiceDetails serviceDetails : detailes) {
			allDetails.putAll(serviceDetails.getAttributes());
		}
		Object uploadDetail = allDetails.get("UPLOAD_NAME");
		Assert.assertNotNull(uploadDetail);
		Assert.assertEquals(expectedUploadDirName, uploadDetail.toString());
	}

	private List<String> getTemplateNames(String templatesList) {
		List<String> templateNames = new LinkedList<String>();
		if (templatesList.contains(DEFAULT_TEMPLATE)) {
			templateNames.add(DEFAULT_TEMPLATE);
		}
		String templates = templatesList;
		while(true) {
			int begin = templates.indexOf("SIMPLE_TEMPLATE");
			if (begin == -1) {
				break;
			}
			int end = templates.indexOf(":", begin);
			String nextTemplateName = templates.substring(begin, end).trim();
			templates = templates.substring(end);
			templateNames.add(nextTemplateName);
		}
		return templateNames;		
	}

		@Override
		protected String getRestUrl() {
			if (!BOOTSTRAP) {
				return "http://" + mngMachineIP + ":8100";
			}
			return super.getRestUrl();
		}

}
