package org.cloudifysource.quality.iTests.test.cli.cloudify;

import iTests.framework.tools.SGTestHelper;

import java.io.IOException;

import iTests.framework.utils.LogUtils;
import org.testng.annotations.Test;

/**
 * GS-1230
 *
 * @author nirb
 *
 */
public class CustomCommandMissingTest extends AbstractLocalCloudTest {

	private static final String SGTEST_ROOT_DIR = SGTestHelper.getSGTestRootDir().replace('\\', '/');

	private static final String SERVICE_NAME = "groovy";
	private static final String SERVICE_PATH = SGTEST_ROOT_DIR + "/src/main/resources/apps/USM/usm/simpleGroovy";

	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void install() throws IOException, InterruptedException {

		installServiceAndWait(SERVICE_PATH, SERVICE_NAME, false);
		try {
			final String invokeResult =
					CommandTestUtils.runCommandExpectedFail("connect " + restUrl + ";invoke " + SERVICE_NAME
							+ " missingCommand");
			org.testng.Assert.assertTrue(invokeResult.contains("No such Custom Command"));
		} finally {
			try {
				this.uninstallService(SERVICE_NAME);
			} catch (final Exception e) {
				LogUtils.log("Failed to uninstall service: " + SERVICE_NAME, e);
			}

		}

	}
}
