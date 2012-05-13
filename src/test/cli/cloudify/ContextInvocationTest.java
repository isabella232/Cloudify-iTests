package test.cli.cloudify;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.cloudifysource.dsl.utils.ServiceUtils;
import org.openspaces.admin.pu.ProcessingUnit;
import org.testng.annotations.Test;

import framework.utils.LogUtils;

public class ContextInvocationTest extends AbstractLocalCloudTest {

	private static final String DEFAULT_APPLICATION_NAME = "default";

	public static final String USM_SERVICE_FOLDER_NAME = "simple";
	public static final String USM_SERVICE_NAME = "simple";

	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
	public void testInvokeCommandFromServiceContext() throws IOException, InterruptedException {

		final String serviceName = "groovy";
		final String servicePath = getUsmServicePath(serviceName);

		LogUtils.log("Installing service " + serviceName);
		CommandTestUtils.runCommandAndWait("connect " + restUrl + ";install-service --verbose " + servicePath);

		final String absolutePUName = ServiceUtils.getAbsolutePUName(DEFAULT_APPLICATION_NAME, serviceName);

		final ProcessingUnit processingUnit = admin.getProcessingUnits().waitFor(absolutePUName,
				Constants.PROCESSINGUNIT_TIMEOUT_SEC, TimeUnit.SECONDS);

		assertTrue("Processing unit :" + absolutePUName + " Was not found", processingUnit != null);
		assertTrue("Instances of '" + absolutePUName + "' service were not found",
				processingUnit.waitFor(2, Constants.PROCESSINGUNIT_TIMEOUT_SEC, TimeUnit.SECONDS));

		final String result = CommandTestUtils.runCommandAndWait("connect " + restUrl
				+ ";invoke -instanceid 1 groovy contextInvoke hello");
		
		assertTrue("Did not find expected custom command response from all instances", result.contains("[hello from 1, hello from 1]"));
		LogUtils.log("Uninstalling service " + serviceName);
		CommandTestUtils.runCommandAndWait("connect " + restUrl + "; uninstall-service " + serviceName + "; exit;");

	}

	private String getUsmServicePath(final String dirOrFilename) {
		return CommandTestUtils.getPath("apps/USM/usm/" + dirOrFilename);
	}
}
