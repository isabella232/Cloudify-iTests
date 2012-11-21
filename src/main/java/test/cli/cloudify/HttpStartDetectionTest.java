package test.cli.cloudify;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.cloudifysource.dsl.utils.ServiceUtils;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.ProcessingUnit;
import org.testng.annotations.Test;

import test.usm.USMTestUtils;
import framework.utils.LogUtils;

/**
 * 
 * @author adaml
 * 
 */
public class HttpStartDetectionTest extends AbstractLocalCloudTest {

	private Machine machineA;
	public static final String USM_APPLICATION_NAME = "default";
	public static final String USM_SERVICE_NAME = "tomcat";

	/**
	 * deploy tomcat on port 8081(VIA Recipe) and try to assert deployment of tomcat on port 8080 using
	 * HttpStartDetection. Look at recipe tomcatHttpStartDetection for more details.
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 */
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, groups = "1")
	public void httpStartDetectionTest()
			throws IOException, InterruptedException {
		final String serviceDir = CommandTestUtils.getPath("/apps/USM/usm/tomcatHttpStartDetection");
		doTest(serviceDir);
	}

	/**
	 * deploy tomcat on port 8081(VIA Recipe) and try to assert deployment of tomcat on port 8080 using
	 * HttpLivenessDetector Plugin. Look at recipe tomcatHttpLivenessDetectorPlugin for more details.
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 */
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, groups = "1")
	public void urlLivenessDetectorTest()
			throws IOException, InterruptedException {
		final String serviceDir = CommandTestUtils.getPath("/apps/USM/usm/tomcatHttpLivenessDetectorPlugin");
		doTest(serviceDir);
	}

	private void doTest(final String serviceDir)
			throws IOException, InterruptedException {
		assertNotNull("Failed to find rest pu", admin.getProcessingUnits().waitFor("rest", 30, TimeUnit.SECONDS));
		machineA = admin.getProcessingUnits().getProcessingUnit("rest").getInstances()[0].getMachine();
		final String command = "connect " + restUrl + ";install-service --verbose " + serviceDir + ";exit";

		LogUtils.log("Installing tomcat on port 8081");
		CommandTestUtils.runCommandExpectedFail(command);
		final String tomcatAbsolutePuName = ServiceUtils.getAbsolutePUName(USM_APPLICATION_NAME, USM_SERVICE_NAME);
		final ProcessingUnit processingUnit =
				admin.getProcessingUnits().waitFor(tomcatAbsolutePuName, Constants.PROCESSINGUNIT_TIMEOUT_SEC,
						TimeUnit.SECONDS);
		assertTrue("tomcat service processing unit does not exist", processingUnit != null);

		// This is not a good test - there could be a temporary moment where the instance is considered 'OK'
		// assertTrue("tomcat service should not be installed.",
		// !admin.getProcessingUnits().getProcessingUnit(tomcatAbsolutePuName).getStatus().equals(DeploymentStatus.INTACT));

		assertTrue("Expected USM processing unit instance to be not running",
				!USMTestUtils.isUSMServiceRunning(tomcatAbsolutePuName, admin));

	}
}
