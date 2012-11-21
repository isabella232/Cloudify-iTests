package test.cli.cloudify;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.cloudifysource.dsl.Application;
import org.cloudifysource.dsl.internal.DSLException;
import org.cloudifysource.dsl.internal.ServiceReader;
import org.cloudifysource.dsl.internal.packaging.Packager;
import org.cloudifysource.dsl.internal.packaging.PackagingException;
import org.cloudifysource.restclient.StringUtils;
import org.openspaces.admin.pu.ProcessingUnit;
import org.testng.Assert;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.Test;

/**
 * 
 * @author yael
 *
 */
public class InstallApplicationWithOverridesFile extends OverridesTest {

	private static final String APPLICATION_OVERRIDEN_NAME = "simpleOverridesApplicationOverriden";
	private static final String SERVICE_OVERRIDEN_NAME = "simpleOverridesService";
	private static final String PU_NAME = APPLICATION_OVERRIDEN_NAME + "." + SERVICE_OVERRIDEN_NAME;

	private static final String SERVICE_ICON = "simpleOverridesApplicationIcon.png";
	private static final String SERVICE_URL = APPLICATION_OVERRIDEN_NAME;

	private static final String APPLICATION_DIR_PATH = 
			CommandTestUtils.getPath("src/main/resources/apps/USM/usm/applications/simpleOverridesApplication");
	private static final String OVERRIDES_FILE_PATH = 
			APPLICATION_DIR_PATH + "/overridesFile/simpleOverrides-application.overrides";
	private static final String APPLICATION_WITH_OVERRIDES_FILE_DIR_PATH = 
			CommandTestUtils.getPath("src/main/resources/apps/USM/usm/applications/simpleOverridesApplicationWithOverrideFile");

	private static final Map<String, Object> EXPECTED_SERVICE_FIELDS = new HashMap<String, Object>();
	static {
		EXPECTED_SERVICE_FIELDS.put("icon", SERVICE_ICON);
		EXPECTED_SERVICE_FIELDS.put("url", SERVICE_URL);
	}

	/**
	 * Tests overrides properties of application that has overrides file in addition to properties file.
	 * Using 'install-application -overrides &ltoverrides file path&gt &ltapplication directory path&gt' CLI command. 
	 * @throws InterruptedException .
	 * @throws IOException .
	 */
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
	public void applicationWithExternalOverridesFileTest()
			throws InterruptedException, IOException {
		applicationOverridesTest(APPLICATION_DIR_PATH, OVERRIDES_FILE_PATH);
	}

	/**
	 * Tests overrides properties of application that has overrides file in addition to properties file.
	 * The overrides file located in theapplication directory.
	 * Using 'install-application &ltapplication directory path&gt' CLI command.
	 * @throws InterruptedException .
	 * @throws IOException .
	 */
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
	public void applicationWithOverridesFileTest() throws InterruptedException,
	IOException {
		applicationOverridesTest(APPLICATION_WITH_OVERRIDES_FILE_DIR_PATH, null);
	}

	/**
	 * Tests overrides properties of application that has overrides file in addition to properties file.
	 * Using the REST API directly to invoke deployApplication, passing the overrides file in the HTTP request.
	 * @throws IOException .
	 * @throws DSLException .
	 * @throws PackagingException .
	 * @throws InterruptedException .
	 */
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
	public void applicationWithOverridesFilePostedDirectlyTest()
			throws IOException, DSLException, PackagingException, InterruptedException {

		// create application zip file
		final File applicationDir = new File(APPLICATION_DIR_PATH);
		final File overridesFile = new File(OVERRIDES_FILE_PATH);
		Application application = ServiceReader.getApplicationFromFile(applicationDir, overridesFile).getApplication();
		File packApplication = Packager.packApplication(application, applicationDir);
		final FileBody applicationFileBody = new FileBody(packApplication);
		final MultipartEntity reqEntity = new MultipartEntity();
		//add application zip file to reqEntity
		reqEntity.addPart("file", applicationFileBody);
		// add overrides file to reqEntity
		final FileBody overridesFileBody = new FileBody(overridesFile);
		reqEntity.addPart("recipeOverridesFile", overridesFileBody);
		// create HttpPost
		String postCommand = restUrl + "/service/applications/" + APPLICATION_OVERRIDEN_NAME + "/timeout/10";
		final HttpPost httppost = new HttpPost(postCommand);
		httppost.setEntity(reqEntity);
		// execute
		final DefaultHttpClient httpClient = new DefaultHttpClient();
		final HttpResponse response = httpClient.execute(httppost);
		StringUtils.getStringFromStream(response.getEntity().getContent());
		Assert.assertEquals(STATUS_OK, response.getStatusLine().getStatusCode());
			
		// asserts
		ProcessingUnit processingUnit = getProcessingUnit(PU_NAME);
		assertProcessingUnit(processingUnit);
		assertApplication();
		assertService();
		assertOverrides(processingUnit);
		
		// un-install
		uninstall(restUrl, "application", APPLICATION_OVERRIDEN_NAME);

	}

	private void applicationOverridesTest(final String applicationDirName,
			final String overridesFilePath) throws IOException, InterruptedException {

		// install
		install(restUrl, "application", applicationDirName, overridesFilePath);

		// get PU
		final ProcessingUnit processingUnit = getProcessingUnit(PU_NAME);

		// asserts
		assertProcessingUnit(processingUnit);
		assertApplication();
		assertService();
		assertOverrides(processingUnit);

		// un-install
		uninstall(restUrl, "application", APPLICATION_OVERRIDEN_NAME);

	}

	private static void assertService() 
			throws IOException, InterruptedException {
		// service exists in services list.
		String output = CommandTestUtils.runCommandAndWait("connect " + restUrl
				+ ";use-application " + APPLICATION_OVERRIDEN_NAME
				+ ";list-services");
		assertTrue("list-services command output doesn't conatin "
				+ SERVICE_OVERRIDEN_NAME + ", output: " + output,
				output.contains(SERVICE_OVERRIDEN_NAME));
	}

	private static void assertApplication() 
			throws IOException, InterruptedException {
		// application exists in applications list.
		String output = CommandTestUtils.runCommandAndWait("connect " + restUrl
				+ ";list-applications");
		assertTrue("list-application command output doesn't conatin "
				+ APPLICATION_OVERRIDEN_NAME + ", output: " + output,
				output.contains(APPLICATION_OVERRIDEN_NAME));
	}

	private void assertOverrides(final ProcessingUnit processingUnit) {
		// application's name was overridden.
		assertEquals(APPLICATION_OVERRIDEN_NAME, processingUnit.getApplication().getName());
		// service's fields were overridden by application's properties.
		assertServiceOverridenFields(processingUnit);
	}

	
	@Override
	protected Map<String, Object> getExpectedServiceFields() {
		return EXPECTED_SERVICE_FIELDS;
	}
	
	@Override
	@AfterSuite(alwaysRun = true)
	public void afterSuite() {
		// TODO Auto-generated method stub
	}
}
