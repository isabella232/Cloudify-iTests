package org.cloudifysource.quality.iTests.test.cli.cloudify;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.cloudifysource.dsl.internal.DSLException;
import org.cloudifysource.dsl.internal.packaging.PackagingException;
import org.cloudifysource.quality.iTests.test.cli.cloudify.util.NewRestTestUtils;
import org.openspaces.admin.pu.ProcessingUnit;
import org.testng.annotations.Test;

/**
 * 
 * @author yael
 *
 */
public class InstallApplicationWithOverridesFileTest extends OverridesTest {

	private static final String APPLICATION_OVERRIDEN_NAME = "simpleOverridesApplicationNameFromOverridesFile_1";
	private static final String SERVICE_OVERRIDEN_NAME = "simpleOverridesService";
	private static final String PU_NAME = APPLICATION_OVERRIDEN_NAME + "." + SERVICE_OVERRIDEN_NAME;

	private static final String SERVICE_ICON = "simpleOverridesApplicationIcon.png";
	private static final String SERVICE_URL = "simpleOverridesApplicationNameFromOverridesFile";

	private static final String APPLICATION_NO_OVERRIDES_FILE_DIR_PATH = 
			CommandTestUtils.getPath("src/main/resources/apps/USM/usm/applications/simpleOverridesApplication");
	private static final String OVERRIDES_FILE_PATH = 
			APPLICATION_NO_OVERRIDES_FILE_DIR_PATH + "/overridesFile/simpleOverrides-application.overrides";
	private static final String APPLICATION_WITH_OVERRIDES_FILE_DIR_PATH = 
			CommandTestUtils.getPath("src/main/resources/apps/USM/usm/applications/simpleOverridesApplicationWithOverrideFile");

	private static final String APPLICATION_SERVICE_NO_PROPERTIES_FILE1_DIR_PATH = 
			CommandTestUtils.getPath("src/main/resources/apps/USM/usm/applications/simpleAppServiceWithoutProperties1");
	private static final String OVERRIDES_FILE_PATH_1 = 
			APPLICATION_SERVICE_NO_PROPERTIES_FILE1_DIR_PATH + "/overridesFile/simpleOverrides-application.overrides";
	private static final String APPLICATION_SERVICE_NO_PROPERTIES_FILE2_DIR_PATH = 
			CommandTestUtils.getPath("src/main/resources/apps/USM/usm/applications/simpleAppServiceWithoutProperties2");
	
	private static final Map<String, Object> EXPECTED_SERVICE_FIELDS = new HashMap<String, Object>();
	static {
		EXPECTED_SERVICE_FIELDS.put("icon", SERVICE_ICON);
		EXPECTED_SERVICE_FIELDS.put("url", SERVICE_URL);
	}

	/**
	 * Tests overrides properties of application.
	 * Using the CLI command with the -overrides option to install the application. 
	 * @throws InterruptedException .
	 * @throws IOException .
	 */
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
	public void applicationWithExternalOverridesFileTest()
			throws InterruptedException, IOException {
		try {
			// install
			install(restUrl, "application", APPLICATION_NO_OVERRIDES_FILE_DIR_PATH, OVERRIDES_FILE_PATH);
			// asserts
			performAsserts();
		} finally {
			// un-install
			uninstallApplicationIfFound(APPLICATION_OVERRIDEN_NAME);
		}
	}

	/**
	 * Tests overrides properties of application. 
	 * The overrides file is located in the application folder.
	 * Uses CLI command (without the -overrides option) to install the service. 
	 * @throws InterruptedException .
	 * @throws IOException .
	 */
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
	public void applicationWithOverridesFileTest() throws InterruptedException,
	IOException {
		try {
			// install
			install(restUrl, "application", APPLICATION_WITH_OVERRIDES_FILE_DIR_PATH, null);
			// asserts
			performAsserts();
		} finally {
			// un-install
			uninstallApplicationIfFound(APPLICATION_OVERRIDEN_NAME);
		}		
	}

	/**
	 * Tests overrides properties of application. 
	 * The overrides file is located in the application folder.
	 * Uses the REST API to install the application.
	 * @throws IOException .
	 * @throws DSLException .
	 * @throws PackagingException .
	 * @throws InterruptedException .
	 */
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
	public void applicationWithOverridesFileNewRestApiTest()
            throws Exception {
		try {
			NewRestTestUtils.installApplicationUsingNewRestApi(
                    restUrl,
                    APPLICATION_OVERRIDEN_NAME,
                    new File(APPLICATION_WITH_OVERRIDES_FILE_DIR_PATH));
			// asserts
			performAsserts();
		} finally {
			// un-install
			uninstallApplicationIfFound(APPLICATION_OVERRIDEN_NAME);
		}
	}
	
	/**
	 * Tests overrides properties of application.
	 * Uploads the overrides file before executing the REST API call.
	 * Uses the REST API to install the application.
	 * @throws IOException
	 * @throws DSLException
	 * @throws PackagingException
	 * @throws InterruptedException
	 */
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
	public void applicationWithExternalOverridesFileNewRestApiTest()
            throws Exception {
		try {
			NewRestTestUtils.installApplicationUsingNewRestApi(
					restUrl, 
					APPLICATION_OVERRIDEN_NAME, 
					new File(APPLICATION_NO_OVERRIDES_FILE_DIR_PATH), 
					new File(OVERRIDES_FILE_PATH), 
					null);
			// asserts
			performAsserts();
		} finally {
			// un-install
			uninstallApplicationIfFound(APPLICATION_OVERRIDEN_NAME);
		}
	}

	/**
	 * Tests overrides properties of application's service. 
	 * service does not have properties file.
	 * application does not have properties file.
	 * application has overrides file.
	 * Uploads the application's overrides file before executing the REST API call.
	 * Uses the REST API to install the application.
	 * @throws IOException
	 * @throws DSLException
	 * @throws PackagingException
	 * @throws InterruptedException
	 */
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
	public void installApplicationWithOverridesFileNoPropsFileAndServiceWithoutPropsFileTest() throws IOException, DSLException, PackagingException, InterruptedException {
		try {
			// install
			install(restUrl, "application", APPLICATION_SERVICE_NO_PROPERTIES_FILE1_DIR_PATH, OVERRIDES_FILE_PATH_1);
			// asserts
			performAsserts();
		} finally {
			// un-install
			uninstallApplicationIfFound(APPLICATION_OVERRIDEN_NAME);
		}	
	}
	
	/**
	 * Tests overrides properties of application's service. 
	 * service does not have properties file.
	 * application does not have overrides file but have properties file.
	 * The application's properties file is located in the application folder.
	 * Uses CLI command (without the -overrides option) to install the service. 
	 * @throws InterruptedException .
	 * @throws IOException .
	 */
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
	public void installApplicationWithPropsFileNoOverridesFileAndServiceWithoutPropsFileTest() throws InterruptedException,
	IOException {
		try {
			// install
			install(restUrl, "application", APPLICATION_SERVICE_NO_PROPERTIES_FILE2_DIR_PATH, null);
			// asserts
			performAsserts();
		} finally {
			// un-install
			uninstallApplicationIfFound(APPLICATION_OVERRIDEN_NAME);
		}		
	}
	
	/**
	 * Tests overrides properties of application's service. 
	 * service does not have properties file.
	 * application does not have properties file.
	 * application has overrides file.
	 * Uploads the application's overrides file before executing the REST API call.
	 * Uses the REST API to install the application.
	 * @throws IOException
	 * @throws DSLException
	 * @throws PackagingException
	 * @throws InterruptedException
	 */
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
	public void installApplicationWithOverridesFileNoPropsFileAndServiceWithoutPropsFileViaRestApiTest()
            throws Exception {
		try {
			NewRestTestUtils.installApplicationUsingNewRestApi(
					restUrl, 
					APPLICATION_OVERRIDEN_NAME, 
					new File(APPLICATION_SERVICE_NO_PROPERTIES_FILE1_DIR_PATH), 
					new File(OVERRIDES_FILE_PATH_1), 
					null);
			// asserts
			performAsserts();
		} finally {
			// un-install
			uninstallApplicationIfFound(APPLICATION_OVERRIDEN_NAME);
		}
	}
	
	
	/**
	 * Tests overrides properties of application's service.
	 * service does not have properties file.
	 * application does not have overrides file but has a properties file.
	 * The application's properties file is located in the application folder.
	 * Uses the REST API to install the application.
	 * @throws IOException
	 * @throws DSLException
	 * @throws PackagingException
	 * @throws InterruptedException
	 */
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
	public void installApplicationWithPropsFileNoOverridesFileAndServiceWithoutPropsFileViaRestApiTest()
            throws Exception {
		try {
			NewRestTestUtils.installApplicationUsingNewRestApi(
					restUrl, 
					APPLICATION_OVERRIDEN_NAME, 
					new File(APPLICATION_SERVICE_NO_PROPERTIES_FILE2_DIR_PATH), 
					null, 
					null);
			// asserts
			performAsserts();
		} finally {
			// un-install
			uninstallApplicationIfFound(APPLICATION_OVERRIDEN_NAME);
		}
	}
	
	private void performAsserts() {
		ProcessingUnit processingUnit = getProcessingUnit(PU_NAME);
		assertProcessingUnit(processingUnit);
		assertApplication(APPLICATION_OVERRIDEN_NAME);
		assertService(APPLICATION_OVERRIDEN_NAME, SERVICE_OVERRIDEN_NAME);
		assertOverrides(processingUnit);
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
