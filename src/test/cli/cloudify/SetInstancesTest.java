package test.cli.cloudify;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.cloudifysource.dsl.utils.ServiceUtils;
import org.openspaces.admin.pu.ProcessingUnit;
import org.testng.annotations.Test;

import test.usm.USMTestUtils;
import framework.utils.LogUtils;

public class SetInstancesTest extends AbstractLocalCloudTest {

	private static final String DEFAULT_APPLICATION_NAME = "default";

	public static final String USM_SERVICE_FOLDER_NAME = "simple";
	public static final String USM_SERVICE_NAME = "simple";

	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
	public void testSetInstancesOnLocalCloud() throws IOException, InterruptedException {

		
		final String serviceName = "groovy";
		final String servicePath = getUsmServicePath(serviceName);

		LogUtils.log("Installing service " + serviceName);
		CommandTestUtils.runCommandAndWait("connect " + restUrl + ";install-service --verbose " + servicePath);

		
		final String absolutePUName = ServiceUtils.getAbsolutePUName(DEFAULT_APPLICATION_NAME, serviceName);

		final ProcessingUnit processingUnit = admin.getProcessingUnits().waitFor(absolutePUName,
				Constants.PROCESSINGUNIT_TIMEOUT_SEC, TimeUnit.SECONDS);

		assertTrue("Processing unit :" + absolutePUName + " Was not found", processingUnit != null);
		assertTrue("Instance of '" + absolutePUName + "' service was not found", processingUnit != null
				&& processingUnit.waitFor(1, Constants.PROCESSINGUNIT_TIMEOUT_SEC, TimeUnit.SECONDS));

		// assert USM service is in a RUNNING state.
		if (serviceName.equals(USM_SERVICE_NAME)) {
			LogUtils.log("Verifing USM service state is set to RUNNING");
			assertTrue(USMTestUtils.waitForPuRunningState(absolutePUName, 60, TimeUnit.SECONDS, admin));
		}


		assertTrue("Two instances of '" + absolutePUName + "' service were not found", processingUnit != null
				&& processingUnit.waitFor(2, 10, TimeUnit.SECONDS));

		CommandTestUtils.runCommandAndWait("connect " + restUrl + ";set-instances groovy 1");

		assertTrue("Two instances of '" + absolutePUName + "' service were found, was expecting only one!",
				!processingUnit.waitFor(2, Constants.PROCESSINGUNIT_TIMEOUT_SEC, TimeUnit.SECONDS));

		LogUtils.log("Uninstalling service " + serviceName);
		CommandTestUtils.runCommandAndWait("connect " + restUrl + "; uninstall-service " + serviceName + "; exit;");

	}

	private String getUsmServicePath(final String dirOrFilename) {
		return CommandTestUtils.getPath("apps/USM/usm/" + dirOrFilename);
	}
}
