package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.ec2.persistence;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.io.FileUtils;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.utils.ServiceUtils;
import org.cloudifysource.quality.iTests.framework.utils.SSHUtils;
import org.cloudifysource.quality.iTests.test.cli.cloudify.CommandTestUtils;
import org.cloudifysource.quality.iTests.test.cli.cloudify.CommandTestUtils.ProcessResult;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.NewAbstractCloudTest;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class Ec2PersistentManagerTest extends NewAbstractCloudTest {

	@Override
	protected String getCloudName() {
		return "ec2";
	}

	@Override
	protected boolean isReusableCloud() {
		return false;
	}

	@Override
	@BeforeClass(alwaysRun = true)
	protected void bootstrap() throws Exception {
		super.bootstrap();
	}

	@Override
	@AfterClass(alwaysRun = true)
	protected void teardown() throws Exception {
		super.teardown();
	}

	@Test
	public void test() throws IOException, InterruptedException {

		final String restPath = createRemoteRestPath();
		final String ipAddress = createServerIpAddress();
		final String command  = "ls -al " + restPath;
		final File pemFile = getPemFile();

		final String restDetailsBeforeRecovery = SSHUtils.runCommand(ipAddress, 10000, command, "ec2-user", pemFile);

		final String connect = "connect " + getRestUrl() + ";";

		String listAttributesOutput = getGlobalAttributes(connect);
		Assert.assertTrue(!listAttributesOutput.contains("PERSISTENT_ATTRIBUTE"),
				"Persistent attribute exists before test started");

		writeGlobalAttribute(connect);

		CommandTestUtils.runCommandAndWait(connect + "install-application travel");

		try {


			CommandTestUtils.runCommandAndWait(connect + "shutdown-managers");

			Assert.assertFalse(ServiceUtils.isHttpURLAvailable(getRestUrl()),
					"Rest gateway still available after shutdown");

			CommandTestUtils.runCommandAndWait("bootstrap-cloud --verbose -use-existing "
					+ this.getService().getPathToCloudFolder());

			final String restDetailsAfterRecovery = SSHUtils.runCommand(ipAddress, 10000, command, "ec2-user", pemFile);
			Assert.assertNotEquals(restDetailsAfterRecovery, restDetailsBeforeRecovery, "Expected rest timestamps to change");

			String listAttributesOutputAfterRestart = getGlobalAttributes(connect);
			Assert.assertTrue(listAttributesOutputAfterRestart.contains("PERSISTENT_ATTRIBUTE"),
					"Persistent attribute exists before test started");

			final String applications = CommandTestUtils.runCommandAndWait(connect + "list-applications");
			Assert.assertTrue(applications.contains("travel"), "Travel application not found after restart");

			final String services =
					CommandTestUtils.runCommandAndWait(connect + "use-application travel;list-services");
			Assert.assertTrue(services.contains("tomcat"), "tomcat service not found after restart");
			Assert.assertTrue(services.contains("cassandra"), "cassandra service not found after restart");

			final String tomcatInstances =
					CommandTestUtils.runCommandAndWait(connect + "use-application travel;list-instances tomcat");
			Assert.assertTrue(tomcatInstances.contains("instance #1"), "No tomcat instances found");
			final String cassandraInstances =
					CommandTestUtils.runCommandAndWait(connect + "use-application travel;list-instances cassandra");
			Assert.assertTrue(cassandraInstances.contains("instance #1"), "No cassandra instances found");
		} finally {
			uninstallApplicationAndWait("travel");
		}

	}

    @Override
    protected void customizeCloud() throws Exception {
        super.customizeCloud();
        getService().getProperties().put("persistencePath", "/home/ec2-user/persistence");
    }


    private String createServerIpAddress() {
		String restUrl  = this.cloudService.getRestUrls()[0];
		URL url;
		try {
			url = new URL(restUrl);
		} catch (MalformedURLException e) {
			throw new IllegalStateException("Failed to parse server url: " + e, e);
		}
		String ipAddress = url.getHost();


		return ipAddress;
	}

	private String createRemoteRestPath() {
		final String persistencePath = this.cloudService.getCloud().getConfiguration().getPersistentStoragePath();
		final String deployPath = persistencePath + "/" + CloudifyConstants.PERSISTENCE_DIRECTORY_DEPLOY_RELATIVE_PATH;
		final String restPath = deployPath + "/" + CloudifyConstants.MANAGEMENT_REST_SERVICE_NAME;
		return restPath;
	}

	private void writeGlobalAttribute(final String connect) throws IOException, InterruptedException {
		// need this cause trying to pass all the single and double quotes on the command line is a pain on multiple OS.
		final File tempFile = File.createTempFile("testcommands", "txt");
		tempFile.deleteOnExit();
		FileUtils.writeStringToFile(tempFile, connect + "set-attributes '{\"PERSISTENT_ATTRIBUTE\":\"YES\"}';");
		CommandTestUtils.runCommandAndWait("-f=" + tempFile.getAbsolutePath());
	}

	private String getGlobalAttributes(final String connect) throws IOException, InterruptedException {
		ProcessResult listAttributesResult = CommandTestUtils.runCloudifyCommandAndWait(connect + "list-attributes");
		Assert.assertEquals(listAttributesResult.getExitcode(), 0, "Failed to read global attributes");
		String listAttributesOutput = listAttributesResult.getOutput();
		return listAttributesOutput;
	}

}
