package test.cli.cloudify.cloud.byon;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashMap;
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
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.openspaces.admin.pu.ProcessingUnits;
import org.openspaces.pu.service.ServiceDetails;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import test.cli.cloudify.CommandTestUtils;
import test.cli.cloudify.cloud.services.byon.ByonCloudService;
import framework.utils.IOUtils;
import framework.utils.LogUtils;

public class AddRemoveTemplatesTest extends AbstractByonCloudTest {

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

	private final String[] DEFAULT_TEMPLATES_EXIST =
		{
			"SMALL_LINUX"
		};
	private final String[] TEMPLATES1_SUCCESSFUL_LIST = 
		{
			"SIMPLE_TEMPLATE1",
			"SIMPLE_TEMPLATE2"		
		};
	private final String[] TEMPLATES2_SUCCESSFUL_LIST = 
		{
			"SIMPLE_TEMPLATE3"		
		};
	private final String[] TEMPLATES2_FAILURE_LIST = 
		{
			"SIMPLE_TEMPLATE1"		
		};
	private final String[] TEMPLATES3_SUCCESSFUL_LIST = 
		{
			"SIMPLE_TEMPLATE3"		
		};
	private final String[] TEMPLATES3_FAILURE_LIST = 
		{
			"SIMPLE_TEMPLATE1"		
		};
	private final String[] TEMPLATES5_SUCCESSFUL_LIST = 
		{
			"SIMPLE_TEMPLATE5"		
		};
	private final String[] NO_UPLOAD_FAILURE_LIST = 
		{
			"SIMPLE_TEMPLATE1",
			"SIMPLE_TEMPLATE2"
		};
	private final String[] EXPECTED_LIST_TEMPLATES = 
		{
			"SIMPLE_TEMPLATE1",
			"SIMPLE_TEMPLATE2",
			"SIMPLE_TEMPLATE3"		
		};	

	@Override
	@BeforeClass(alwaysRun = true)
	protected void bootstrap() throws Exception {
		
		ByonCloudService service = new ByonCloudService();
		String[] machines = service.getMachines();
		service.setIpList(machines[0]);
		
		super.bootstrap(service);

//		this.cloudService = CloudServiceManager.getInstance().getCloudService(this.getCloudName());
//		AdminFactory factory = new AdminFactory();
//		factory.addLocators("pc-lab111:" + CloudifyConstants.DEFAULT_LUS_PORT);
//		admin = factory.createAdmin();
	}

	private void updateTemplatesProperties(int templateNum, String propertiesFile) throws IOException {
		
		OutputStream out = null;
		try {
			File template5PropsFile = new File(propertiesFile);		
			Properties props5 = new Properties();
			props5.setProperty("node_ip", '"' + getService().getMachines()[templateNum] + '"');
			props5.setProperty("node_id", "\"byon-pc-lab{0}\"");
			out = new FileOutputStream(template5PropsFile);
			props5.store(out, null);
			IOUtils.writePropertiesToFile(props5, template5PropsFile);
			
		
		}finally {
			if (out != null) {
				out.close();
			}
		}
		
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, enabled = true)
	public void addTempalteAndInstallService() throws IOException, InterruptedException {
		updateTemplatesProperties(1, TEMPLATES1_PROPERTIES_PATH);
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

	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, enabled = false)
	public void addTempaltesUsingRestAPI() 
			throws IllegalStateException, IOException, InterruptedException {
		updateTemplatesProperties(5, TEMPLATES5_PROPERTIES_PATH);
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

	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, enabled = false)
	public void addExistAndNotExistTempaltes() {
		try {
			addTempaltes(TEMPLATES1_DIR_PATH, TEMPLATES1_SUCCESSFUL_LIST, null);
			addTempaltes(TEMPLATES2_DIR_PATH, TEMPLATES2_SUCCESSFUL_LIST, TEMPLATES2_FAILURE_LIST);
		} finally {
			removeTempaltes(EXPECTED_LIST_TEMPLATES, DEFAULT_TEMPLATES_EXIST);
		}
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, enabled = false)
	public void removeTemplateAndTryToInstallService() throws IOException, InterruptedException {
		addTempaltes(TEMPLATES1_DIR_PATH, TEMPLATES1_SUCCESSFUL_LIST, null);
		String[] expectedAfterRemoved = {"SIMPLE_TEMPLATE2"};
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

	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, enabled = false)
	public void removeNotExistTemplate() {
		removeTempalte("SIMPLE_TEMPLATE1", true, DEFAULT_TEMPLATES_EXIST);
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, enabled = false)
	public void illegalDuplicateTempaltes() {
		try {
			addTempaltes(TEMPLATES1_DIR_PATH, TEMPLATES1_SUCCESSFUL_LIST, null);
			addTempaltes(TEMPLATES3_DIR_PATH, TEMPLATES3_SUCCESSFUL_LIST, TEMPLATES3_FAILURE_LIST);
		} finally {
			removeTempaltes(EXPECTED_LIST_TEMPLATES, DEFAULT_TEMPLATES_EXIST);
		}
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, enabled = false)
	public void illegalTempalteWithoutLocalUploadDir() {
		addTempaltes(NO_UPLOAD_TEMPLATE_DIR_PATH, null, NO_UPLOAD_FAILURE_LIST);
		assertListTempaltes(DEFAULT_TEMPLATES_EXIST);
	}

	@Override
	@AfterClass(alwaysRun = true)
	protected void teardown() throws Exception {
		super.teardown();
	}

	private void installService(String servicePath, String serviceName, boolean expectToFail) {
		try {
			installServiceAndWait(servicePath, serviceName, 5 , expectToFail);
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	private String addTempaltes(String templatesFolder, String[] expectedTemplates, 
			String[] failedTemplates) {
		String command = "connect " + getRestUrl() + ";add-templates " + templatesFolder;
		String output = null;
		try {
			if (failedTemplates != null && failedTemplates.length > 0) {
				output = CommandTestUtils.runCommandExpectedFail(command);
			} else {
				output = CommandTestUtils.runCommandAndWait(command);
			}
			assertListTempaltes(expectedTemplates);
		} catch (final Exception e) {
			LogUtils.log(e.getMessage(), e);
			Assert.fail(e.getMessage());
		}
		return output;
	}

	private void removeTempaltes(String[] tempaltes, String[] expectedAfterRemove) {
		for (String templateName : tempaltes) {
			removeTempalte(templateName, false, expectedAfterRemove);
		}
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
		if (expectedTempaltes == null) {
			return;
		}
		String command = "connect " + getRestUrl() + ";list-templates";
		try {
			String output = CommandTestUtils.runCommandAndWait(command);
			for (String templateName : expectedTempaltes) {
				Assert.assertTrue(output.contains(templateName));
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

//	@Override
//	protected String getRestUrl() {
//		return "http://pc-lab111:8100";
//	}

}
