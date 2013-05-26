package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.ec2;

import iTests.framework.tools.SGTestHelper;
import iTests.framework.utils.IOUtils;

import java.io.File;
import java.io.IOException;

import org.cloudifysource.esc.installer.remoteExec.BootstrapScriptErrors;
import org.cloudifysource.quality.iTests.framework.utils.CloudBootstrapper;
import org.cloudifysource.quality.iTests.framework.utils.JCloudsUtils;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.NewAbstractCloudTest;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * This test makes a bootstrap on ec2 fail by changing the JAVA_HOME path to a bad one in the bootstrap-management.sh file.
 * <p>After the bootstrap fails, the test checks if the management machine was shutdown.
 * 
 * @author noak
 *
 */
public class BootstrapErrorCodesTest extends NewAbstractCloudTest {

	private static final String STANDARD_BOOTSTRAP_SCRIPT = "bootstrap-management.sh";

	private String badBootstrapScript = null;
	private CloudBootstrapper bootstrapper;

	@BeforeClass
	public void init() throws Exception {
		bootstrapper = new CloudBootstrapper();
		bootstrapper.scanForLeakedNodes(true);
	}
	

	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void wrongJavaDownloadUrlTest() throws IOException, InterruptedException {
		badBootstrapScript = "wrong-java-path-bootstrap-management.sh";
		BootstrapScriptErrors expectedError = BootstrapScriptErrors.JAVA_DOWNLOAD_FAILED;
		try {
			super.bootstrap(bootstrapper);
			AssertFail("Java download URL is wrong yet no error was thrown. Reported error: " 
					+ expectedError.getErrorMessage() + " (" + expectedError.getErrorCode() + ")");
		} catch (Throwable ae) {
			assertTrue("Java download URL is wrong but the wrong error was thrown. Reported error: " + ae.getMessage(),
					isMessageTextCorrect(ae.getMessage(), expectedError));
		}
	}

	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void wrongCloudifyOverridesUrlTest() throws IOException, InterruptedException {
		badBootstrapScript = "wrong-cloudify-overrides-bootstrap-management.sh";
		BootstrapScriptErrors expectedError = BootstrapScriptErrors.CLOUDIFY_OVERRIDES_DOWNLOAD_FAILED;
		try {
			super.bootstrap(bootstrapper);
			AssertFail("Cloudify overrides URL is wrong yet no error was thrown. Expected error: " 
					+ expectedError.getErrorMessage() + " (" + expectedError.getErrorCode() + ")");
		} catch (Throwable ae) {
			assertTrue("Cloudify overrides URL is wrong but the wrong error was thrown. Reported error: " + ae.getMessage(),
					isMessageTextCorrect(ae.getMessage(), expectedError));
		}
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void wrongChmodCommandTest() throws IOException, InterruptedException {
		badBootstrapScript = "wrong-chmod-bootstrap-management.sh";
		BootstrapScriptErrors expectedError = BootstrapScriptErrors.CLOUDIFY_CHMOD_FAILED;
		try {
			super.bootstrap(bootstrapper);
			AssertFail("The chmod command is wrong yet no error was thrown. Expected error: " 
					+ expectedError.getErrorMessage() + " (" + expectedError.getErrorCode() + ")");
		} catch (Throwable ae) {
			assertTrue("The chmod command is wrong but the wrong error was thrown. Reported error: " + ae.getMessage(),
					isMessageTextCorrect(ae.getMessage(), expectedError));
		}
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void failedCloudifyExecutionTest() throws IOException, InterruptedException {
		badBootstrapScript = "failed-cloudify-execution-bootstrap-management.sh";
		BootstrapScriptErrors expectedError = BootstrapScriptErrors.CUSTOM_ERROR;
		try {
			super.bootstrap(bootstrapper);
			AssertFail("Cloudify execution failed yet no error was thrown. Expected error: " 
					+ expectedError.getErrorMessage() + " (" + expectedError.getErrorCode() + ")");
		} catch (Throwable ae) {
			assertTrue("Cloudify execution failed but the wrong error was thrown. Reported error: " + ae.getMessage(),
					isMessageTextCorrect(ae.getMessage(), expectedError));
		}
	}
	
	
	@AfterTest
	public void teardown() throws Exception {
		super.teardown();
		assertTrue("Leaked node were found", getService().scanLeakedAgentAndManagementNodes());
		JCloudsUtils.closeContext();
	}

	@Override
	protected String getCloudName() {
		return "ec2";
	}

	@Override
	protected boolean isReusableCloud() {
		return false;
	}
	
	protected void customizeCloud() throws IOException {
		//replace the bootstrap-management with a bad version, to fail the bootstrap.
		File standardBootstrapFile = new File(getService().getPathToCloudFolder() + "/upload", STANDARD_BOOTSTRAP_SCRIPT);
		File badBootstrapFile = new File(SGTestHelper.getSGTestRootDir() + "/src/main/resources/apps/cloudify/cloud/ec2/" + badBootstrapScript);
		IOUtils.replaceFile(standardBootstrapFile, badBootstrapFile);
		File newFile = new File(getService().getPathToCloudFolder() + "/upload", badBootstrapScript);
		if (newFile.exists()) {
			newFile.renameTo(standardBootstrapFile);
		}
		
		System.out.println("replacing line ending (DOS2UNIX)");
		IOUtils.replaceTextInFile(standardBootstrapFile.getAbsolutePath(), "\r\n", "\n");// DOS2UNIX
	}
	
	private static boolean isMessageTextCorrect(final String messageText, final BootstrapScriptErrors expectedError) {
		return (messageText.contains(String.valueOf(expectedError.getErrorCode())) &&
				messageText.contains(expectedError.getErrorMessage()));
	}

}
