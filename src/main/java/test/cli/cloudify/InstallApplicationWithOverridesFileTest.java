package test.cli.cloudify;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.cloudifysource.dsl.internal.DSLException;
import org.cloudifysource.dsl.internal.packaging.PackagingException;
import org.openspaces.admin.pu.ProcessingUnit;
import org.testng.annotations.Test;

/**
 * 
 * @author yael
 *
 */
public class InstallApplicationWithOverridesFileTest extends OverridesTest {

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
	 * The overrides file located in the application directory.
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
	 * Using the REST API directly to invoke deployApplication, post the overrides file as part of the HTTP request.
	 * @throws IOException .
	 * @throws DSLException .
	 * @throws PackagingException .
	 * @throws InterruptedException .
	 */
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
	public void applicationWithOverridesFilePostedDirectlyTest()
			throws IOException, DSLException, PackagingException, InterruptedException {
		try {
			RestTestUtils.installApplicationUsingRestApi(restUrl, APPLICATION_OVERRIDEN_NAME, new File(APPLICATION_DIR_PATH), new File(OVERRIDES_FILE_PATH));
			// asserts
			ProcessingUnit processingUnit = getProcessingUnit(PU_NAME);
			assertProcessingUnit(processingUnit);
			assertApplication(APPLICATION_OVERRIDEN_NAME);
			assertService(APPLICATION_OVERRIDEN_NAME, SERVICE_OVERRIDEN_NAME);
			assertOverrides(processingUnit);
		} finally {
			// un-install
			uninstallApplicationIfFound(APPLICATION_OVERRIDEN_NAME);
		}
	}

	private void applicationOverridesTest(final String applicationDirName,
			final String overridesFilePath) throws IOException, InterruptedException {
		try {
			// install
			install(restUrl, "application", applicationDirName, overridesFilePath);
			// asserts
			final ProcessingUnit processingUnit = getProcessingUnit(PU_NAME);
			assertProcessingUnit(processingUnit);
			assertApplication(APPLICATION_OVERRIDEN_NAME);
			assertService(APPLICATION_OVERRIDEN_NAME, SERVICE_OVERRIDEN_NAME);
			assertOverrides(processingUnit);
		} finally {
			// un-install
			uninstallApplicationIfFound(APPLICATION_OVERRIDEN_NAME);
		}
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
}
