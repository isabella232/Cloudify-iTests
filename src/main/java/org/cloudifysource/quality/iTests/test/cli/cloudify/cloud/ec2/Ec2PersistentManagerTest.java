package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.ec2;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.cloudifysource.dsl.utils.ServiceUtils;
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
	protected void customizeCloud() throws Exception {
		super.customizeCloud();
		getService().getProperties().put("persistencePath", "/home/ec2-user/persistence");

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

	public static void main(String[] args) throws IOException, InterruptedException {
		final File tempFile = File.createTempFile("testcommands", "txt");
		FileUtils.writeStringToFile(tempFile, "connect localhost;set-attributes '{\"PERSISTENT_ATTRIBUTE\":\"YES2\"}';");
		CommandTestUtils.runCommandAndWait("-f=" + tempFile.getAbsolutePath());
		CommandTestUtils.runCommandAndWait("connect localhost;list-attributes");

	}
	@Test
	public void test() throws IOException, InterruptedException {
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

			CommandTestUtils.runCommandAndWait("bootstrap-cloud -use-existing "
					+ this.getService().getPathToCloudFolder());

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
