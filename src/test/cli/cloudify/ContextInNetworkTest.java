package test.cli.cloudify;

import java.io.IOException;

import org.testng.annotations.Test;

import framework.utils.LogUtils;

public class ContextInNetworkTest extends AbstractLocalCloudTest {

	/**********
	 * Installs the network_with_context service, which checks that context.instanceId can be used in the network block.
	 * 
	 * @throws IOException .
	 * @throws InterruptedException .
	 */
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
	public void test()
			throws IOException, InterruptedException {

		final String serviceName = "groovy";
		final String servicePath = getUsmServicePath("network_with_context");

		LogUtils.log("Installing service " + serviceName);
		CommandTestUtils.runCommandAndWait("connect " + restUrl + ";install-service --verbose " + servicePath);

		CommandTestUtils.runCommandAndWait("connect " + restUrl + "; uninstall-service " + serviceName + "; exit;");

	}

	private String getUsmServicePath(final String dirOrFilename) {
		return CommandTestUtils.getPath("apps/USM/usm/" + dirOrFilename);
	}
}
