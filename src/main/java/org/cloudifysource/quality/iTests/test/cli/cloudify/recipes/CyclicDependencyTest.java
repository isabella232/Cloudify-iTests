package org.cloudifysource.quality.iTests.test.cli.cloudify.recipes;

import iTests.framework.utils.AssertUtils;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.cloudifysource.dsl.utils.ServiceUtils;
import org.cloudifysource.quality.iTests.framework.utils.ApplicationInstaller;
import org.cloudifysource.quality.iTests.test.cli.cloudify.AbstractLocalCloudTest;
import org.cloudifysource.quality.iTests.test.cli.cloudify.CommandTestUtils;
import org.openspaces.admin.pu.ProcessingUnit;
import org.testng.Assert;
import org.testng.annotations.Test;

public class CyclicDependencyTest extends AbstractLocalCloudTest {

	private void dependencyTest(final String ApplicationName) throws IOException, InterruptedException {
		final String path = CommandTestUtils.getPath("src/main/resources/apps/cloudify/recipes/" + ApplicationName);
		final String output =
				CommandTestUtils
						.runCommandExpectedFail("connect " + restUrl + ";install-application " + path + ";exit");

		Assert.assertTrue(output.contains("contains one or more cycles"),
				"Output does not contain the expected cycle error. Output was: " + output);

	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
	public void complexTest() throws IOException, InterruptedException {
		dependencyTest("complexCycle");
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
	public void simpleTest() throws IOException, InterruptedException {
		dependencyTest("cycle");
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
	public void diamondTest() throws IOException, InterruptedException {

		final String path = CommandTestUtils.getPath("src/main/resources/apps/cloudify/recipes/diamond");

		final ApplicationInstaller installer = new ApplicationInstaller(restUrl, "diamond");
		installer.recipePath(path);
		installer.install();

		AssertUtils.assertTrue(
				"Failed to discover D service after installation",
				admin.getProcessingUnits().waitFor(ServiceUtils.getAbsolutePUName("diamond", "D"), OPERATION_TIMEOUT,
						TimeUnit.MILLISECONDS) != null);

		final ProcessingUnit processingUnit =
				admin.getProcessingUnits().getProcessingUnit(ServiceUtils.getAbsolutePUName("diamond", "D"));
		AssertUtils.assertTrue("Failed to discover 1 instance of service D",
				processingUnit.waitFor(1, OPERATION_TIMEOUT, TimeUnit.MILLISECONDS));
	}
}
