package org.cloudifysource.quality.iTests.test.cli.cloudify;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import junit.framework.Assert;

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
	private static final Map<String, Object> EXPECTED_SERVICE_FIELDS = new HashMap<String, Object>();

	private static final String SERVICE_ICON = "simpleServiceIcon.png";
	private static final String SERVICE_URL = "simpleOverridenUrl";

	static {
		EXPECTED_SERVICE_FIELDS.put("icon", SERVICE_ICON);
		EXPECTED_SERVICE_FIELDS.put("url", SERVICE_URL);
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
	public void serviceOverridesViaRestApiTest() throws IOException, InterruptedException {

		final File serviceDir = new File(SERVICE_DIR_PATH);
		final File overridesFile = new File(OVERRIDES_FILE_PATH);

		try {
//			restUrl = "http://localhost:8888/rest";
			RestTestUtils.installServiceUsingRestApi(restUrl, SERVICE_NAME, serviceDir, null, null, overridesFile);

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
	 * Tests overrides properties of service that has overrides file in addition to properties file.
	 * Using 'install-service -overrides &ltoverrides file path&gt &ltservice directory path&gt' CLI command. 
	 * @throws InterruptedException .
	 * @throws IOException .
	 */
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
	public void serviceWithExternalOverridesFileTest()
			throws InterruptedException, IOException {
		serviceOverridesTest(SERVICE_NAME, SERVICE_DIR_PATH, OVERRIDES_FILE_PATH);
	}
  
	/**
	 * Tests overrides properties of service that has overrides file in addition to properties file.
	 * The overrides file located in the service directory.
	 * Using 'install-service &ltservice directory path&gt' CLI command.
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

	@Override
	protected Map<String, Object> getExpectedServiceFields() {
		return EXPECTED_SERVICE_FIELDS;
	}

}
