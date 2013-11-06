package org.cloudifysource.quality.iTests.test.cli.cloudify;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import junit.framework.Assert;

import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.openspaces.admin.pu.ProcessingUnit;
import org.testng.annotations.Test;

/**
 * 
 * @author yael
 *
 */
public class InstallServiceWithOverridesFileTest extends OverridesTest {
	private static final String SERVICE_NAME = "simple";
	private static final String SERVICE_WITH_OVERRIDES_NAME = "simpleWithOverrides";

	private static final String SERVICE_DIR_PATH = 
			CommandTestUtils.getPath("src/main/resources/apps/USM/usm/services/simple");
	private static final String OVERRIDES_FILE_PATH = 
			CommandTestUtils.getPath("src/main/resources/apps/USM/usm/services/simpleService-service.overrides");
	private static final String SERVICE_WITH_OVERRIDES_FILE_DIR_PATH = 
			CommandTestUtils.getPath("src/main/resources/apps/USM/usm/services/simpleWithOverrides");
	private static final String SERVICE_WITHOUT_PROPERTIES_FILE_DIR_PATH = 
			CommandTestUtils.getPath("src/main/resources/apps/USM/usm/services/simpleWithoutProperties");
	private static final String OVERRIDES_WITHOUT_PROPERTIES_FILE_PATH = 
			CommandTestUtils.getPath("src/main/resources/apps/USM/usm/services/simple-service.overrides");
	
	private static final Map<String, Object> EXPECTED_SERVICE_FIELDS = new HashMap<String, Object>();

	private static final String SERVICE_ICON = "simpleServiceIcon.png";
	private static final String SERVICE_URL = "simpleOverridenUrl";

	static {
		EXPECTED_SERVICE_FIELDS.put("icon", SERVICE_ICON);
		EXPECTED_SERVICE_FIELDS.put("url", SERVICE_URL);
	}

	/**
	 * Tests overrides properties of service.
	 * Uses the REST API to install the service.
	 * Uploads the overrides file before the REST API call.
	 * @throws IOException
	 * @throws InterruptedException
	 */
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
	public void serviceWithExternalOverridesFileViaRestApiTest() throws IOException, InterruptedException {

		final File serviceDir = new File(SERVICE_DIR_PATH);
		final File overridesFile = new File(OVERRIDES_FILE_PATH);

		try {
			NewRestTestUtils.installServiceUsingNewRestAPI(
					restUrl, 
					serviceDir, 
					CloudifyConstants.DEFAULT_APPLICATION_NAME, 
					SERVICE_NAME, 
					overridesFile, 
					5, 
					null);

			// get PU
			final ProcessingUnit processingUnit = getProcessingUnit("default." + SERVICE_NAME);
			// asserts
			assertProcessingUnit(processingUnit);
			assertService("default", SERVICE_NAME);
			assertServiceOverridenFields(processingUnit);

		} catch (Exception e) {
			Assert.fail(e.getMessage());
		} finally {
			// un-install
			uninstallService(SERVICE_NAME);
		}
	}
	
	/**
	 * Tests overrides properties of service.
	 * The overrides file located in the service folder.
	 * Uses the REST API to install the service.
	 * @throws IOException
	 * @throws InterruptedException
	 */
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
	public void serviceWithOverridesFileViaRestApiTest() throws IOException, InterruptedException {

		final File serviceDir = new File(SERVICE_WITH_OVERRIDES_FILE_DIR_PATH);

		try {
			NewRestTestUtils.installServiceUsingNewRestAPI(
					restUrl, 
					serviceDir, 
					CloudifyConstants.DEFAULT_APPLICATION_NAME, 
					SERVICE_WITH_OVERRIDES_NAME, 
					5);

			// get PU
			final ProcessingUnit processingUnit = getProcessingUnit("default." + SERVICE_WITH_OVERRIDES_NAME);
			// asserts
			assertProcessingUnit(processingUnit);
			assertService("default", SERVICE_WITH_OVERRIDES_NAME);
			assertServiceOverridenFields(processingUnit);

		} catch (Exception e) {
			Assert.fail(e.getMessage());
		} finally {
			// un-install
			uninstallService(SERVICE_WITH_OVERRIDES_NAME);
		}
	}	

	/**
	 * Tests overrides properties of service.
	 * Using CLI command with the -overrides option to install the service. 
	 * @throws InterruptedException .
	 * @throws IOException .
	 */
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
	public void serviceWithExternalOverridesFileTest()
			throws InterruptedException, IOException {
		serviceOverridesTest(SERVICE_NAME, SERVICE_DIR_PATH, OVERRIDES_FILE_PATH);
	}
  
	/**
	 * Tests overrides properties of service.
	 * The overrides file located in the service directory.
	 * Using CLI command (without the -overrides option) to install the service. 
	 * @throws InterruptedException .
	 * @throws IOException .
	 */
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
	public void serviceWithOverridesFileTest() throws InterruptedException,
	IOException {
		serviceOverridesTest(SERVICE_WITH_OVERRIDES_NAME, SERVICE_WITH_OVERRIDES_FILE_DIR_PATH, null);
	}

	private void serviceOverridesTest(final String serviceName, final String serviceDirPath,
			final String overridesFilePath) throws IOException, InterruptedException {

		try {
			// install
			install(restUrl, "service", serviceDirPath, overridesFilePath);

			// get PU
			final ProcessingUnit processingUnit = getProcessingUnit("default." + serviceName);

			// asserts
			assertProcessingUnit(processingUnit);
			assertService("default", serviceName);
			assertServiceOverridenFields(processingUnit);
		} finally {
			// un-install
			uninstallService(serviceName);
		}

	}
	
	/**
	 * Tests overrides properties of service.
	 * Uses the REST API to install the service.
	 * Uploads the overrides file before the REST API call.
	 * @throws InterruptedException 
	 * @throws IOException 
	 */
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
	public void installServiceWithoutPropertiesFileWithOverridesFileViaRestApiTest() throws IOException, InterruptedException {
		
		final File serviceDir = new File(SERVICE_WITHOUT_PROPERTIES_FILE_DIR_PATH);

		try {
			NewRestTestUtils.installServiceUsingNewRestAPI(
					restUrl, 
					serviceDir, 
					CloudifyConstants.DEFAULT_APPLICATION_NAME, 
					SERVICE_NAME, 
					new File(OVERRIDES_WITHOUT_PROPERTIES_FILE_PATH),
					5,
					null);

			// get PU
			final ProcessingUnit processingUnit = getProcessingUnit("default." + SERVICE_NAME);
			// asserts
			assertProcessingUnit(processingUnit);
			assertService("default", SERVICE_NAME);
			assertServiceOverridenFields(processingUnit);

		} catch (Exception e) {
			Assert.fail(e.getMessage());
		} finally {
			// un-install
			uninstallService(SERVICE_NAME);
		}		
	}
	
	/**
	 * Tests overrides properties of service.
	 * Using CLI command with the -overrides option to install the service. 
	 * @throws InterruptedException 
	 * @throws IOException 
	 */
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
	public void installServiceWithoutPropertiesFileWithOverridesFileTest() throws IOException, InterruptedException {
		serviceOverridesTest(SERVICE_NAME, SERVICE_WITHOUT_PROPERTIES_FILE_DIR_PATH, OVERRIDES_WITHOUT_PROPERTIES_FILE_PATH);
	}
	
	@Override
	protected Map<String, Object> getExpectedServiceFields() {
		return EXPECTED_SERVICE_FIELDS;
	}

}
