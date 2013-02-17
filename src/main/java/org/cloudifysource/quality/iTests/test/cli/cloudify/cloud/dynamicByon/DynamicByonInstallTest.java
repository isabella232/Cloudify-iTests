package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.dynamicByon;

import java.io.IOException;

import junit.framework.Assert;

import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.byon.AbstractByonCloudTest;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.cloudifysource.quality.iTests.test.cli.cloudify.CommandTestUtils;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.byon.AbstractByonCloudTest;

public class DynamicByonInstallTest extends AbstractByonCloudTest {

	protected final String APPLICATION_FOLDER_PATH = CommandTestUtils.getPath("src/main/resources/apps/USM/usm/applications/verySimple");
	protected final String APPLICATION_NAME = "verySimple";


	@Override
	protected String getCloudName() {
		return "dynamic-byon";
	}

	@BeforeClass(alwaysRun = true)
	@Override
	protected void bootstrap() throws Exception {
		super.bootstrap();
	}


	@AfterClass(alwaysRun = true)
	@Override
	protected void teardown() throws Exception {
		super.teardown();
	}

	@Override
	protected void customizeCloud() throws Exception {
		super.customizeCloud();
		getService().getProperties().put("password", "tgrid");
		getService().getProperties().put("password", "tgrid");
		String[] machines = getService().getIpList().split(",");
		getService().getProperties().put("startMachineIP", machines[0]);
		getService().getProperties().put("managementMachines", machines[1]);
	}

	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 2, enabled = false)
	public void installSimpleApplicationTest() throws IOException, InterruptedException {
		installApplicationAndWait(APPLICATION_FOLDER_PATH, APPLICATION_NAME);
		String output = CommandTestUtils.runCommandAndWait("connect " + getRestUrl() + ";list-applications");
		Assert.assertTrue(output.contains(APPLICATION_NAME));

	}
	
	@AfterMethod
	public void cleanup() throws IOException, InterruptedException {
		uninstallApplicationIfFound(APPLICATION_NAME);
	}
}
