package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.ec2.dynamicstorage;

import java.util.concurrent.TimeoutException;

import org.cloudifysource.dsl.context.blockstorage.LocalStorageOperationException;
import org.cloudifysource.quality.iTests.framework.utils.AssertUtils;
import org.cloudifysource.quality.iTests.framework.utils.ServiceInstaller;
import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class UnsupportedFileSystemTest extends AbstractDynamicStorageTest {

	// define this so in case of  refactoring the test wont fail.
	private static final String EXPECTED_EXCEPTION = LocalStorageOperationException.class.getSimpleName();

	private static final String FOLDER_NAME = "unsupported-fs";
	private ServiceInstaller installer;

	@Override
	protected String getCloudName() {
		return "ec2";
	}

	@BeforeClass(alwaysRun = true)
	protected void bootstrap() throws Exception {
		super.bootstrap();
	}

	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 4, enabled = true)
	public void testLinux() throws Exception {
		super.testLinux();
	}


	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 4, enabled = true)
	public void testUbuntu() throws Exception  {
		super.testUbuntu();
	}

	@Override
	public void doTest() throws Exception {

		installer = new ServiceInstaller(getRestUrl(), SERVICE_NAME);
		installer.recipePath(FOLDER_NAME);
		installer.timeoutInMinutes(5);
		installer.setDisableSelfHealing(true);
		String installOutput = installer.install();

		// this installs a service that tries to mount a device onto a non supported file system(foo)
		// see src/main/resources/apps/USM/usm/dynamicstroage/unsupported-fs/groovy.service
		// so we except the LocalStorageOperationException to propagate to the CLI.

		AssertUtils.assertTrue("install output should have contained " + EXPECTED_EXCEPTION, installOutput.contains(EXPECTED_EXCEPTION));

		installer.uninstall();


	}

	@AfterMethod
	public void scanForLeakes() throws TimeoutException {
		super.scanForLeakedVolumesCreatedViaTemplate("SMALL_BLOCK");;
	}

	@AfterClass(alwaysRun = true)
	protected void teardown() throws Exception {
		super.teardown();
	}

	@Override
	protected boolean isReusableCloud() {
		return false;
	}

	@Override
	public String getServiceFolder() {
		return FOLDER_NAME;
	}

}
