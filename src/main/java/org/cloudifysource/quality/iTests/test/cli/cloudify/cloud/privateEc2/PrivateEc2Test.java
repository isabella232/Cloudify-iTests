package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.privateEc2;

import iTests.framework.utils.AssertUtils;

import java.io.File;

import org.cloudifysource.domain.Application;
import org.cloudifysource.domain.Service;
import org.cloudifysource.dsl.internal.ServiceReader;
import org.cloudifysource.quality.iTests.framework.utils.ApplicationInstaller;
import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
import org.cloudifysource.quality.iTests.test.cli.cloudify.CommandTestUtils;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.NewAbstractCloudTest;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.services.privateEc2.PrivateEc2Service;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class PrivateEc2Test extends NewAbstractCloudTest {

	@Override
	protected String getCloudName() {
		return "privateEc2";
	}

	@Override
	protected boolean isReusableCloud() {
		return false;
	}

	@Override
	@BeforeClass(alwaysRun = true)
	protected void bootstrap() throws Exception {
		super.bootstrap(new PrivateEc2Service(), null);
	}

	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 4, enabled = true)
	public void testSampleApplication() throws Exception {

		final String applicationPath = "src/main/resources/private-ec2/recipes/apps/sampleApplication";
		final File applicationDirectory = new File(applicationPath);
		Assert.assertTrue(applicationDirectory.exists() && applicationDirectory.isDirectory(),
				"Expected directory at: " + applicationDirectory.getAbsolutePath());

		final String restUrl = getRestUrl();
		final ApplicationInstaller installer = new ApplicationInstaller(restUrl, null);
		installer.setApplicationName("sampleApplication");
		installer.recipePath(applicationDirectory.getAbsolutePath());
		installer.waitForFinish(true);
		installer.install();

		final Application application =
				ServiceReader.getApplicationFromFile(new File(applicationPath)).getApplication();

		final String command = "connect " + restUrl + ";use-application sampleApplication;list-services";
		final String output = CommandTestUtils.runCommandAndWait(command);

		for (final Service singleService : application.getServices()) {
			AssertUtils.assertTrue("the service " + singleService.getName() + " is not running",
					output.contains(singleService.getName()));
		}
	}
	
	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 4, enabled = true)
	public void testSampleApplicationWithCustomCloudConfiguration() throws Exception {

		final String applicationPath = "src/main/resources/private-ec2/recipes/apps/sampleApplication";
		final File applicationDirectory = new File(applicationPath);
		Assert.assertTrue(applicationDirectory.exists() && applicationDirectory.isDirectory(),
				"Expected directory at: " + applicationDirectory.getAbsolutePath());

		final String restUrl = getRestUrl();
		final ApplicationInstaller installer = new ApplicationInstaller(restUrl, null);
		installer.setApplicationName("sampleApplication");
		installer.recipePath(applicationDirectory.getAbsolutePath());
		final File cfnTemplatesDirectory = new File("src/main/resources/private-ec2/cfn-templates");
		Assert.assertTrue(cfnTemplatesDirectory.exists() && cfnTemplatesDirectory.isDirectory(),
				"Expected directory at: " + cfnTemplatesDirectory.getAbsolutePath());
		installer.cloudConfiguration(cfnTemplatesDirectory.getAbsolutePath());
		installer.waitForFinish(true);
		installer.install();

		final Application application =
				ServiceReader.getApplicationFromFile(new File(applicationPath)).getApplication();

		final String command = "connect " + restUrl + ";use-application sampleApplication;list-services";
		final String output = CommandTestUtils.runCommandAndWait(command);

		for (final Service singleService : application.getServices()) {
			AssertUtils.assertTrue("the service " + singleService.getName() + " is not running",
					output.contains(singleService.getName()));
		}
	}

	@Override
	@AfterClass(alwaysRun = true)
	protected void teardown() throws Exception {
		super.teardown();
	}
}
