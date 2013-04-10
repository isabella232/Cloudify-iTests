package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.ec2.dynamicstorage;

import org.cloudifysource.quality.iTests.framework.utils.ServiceInstaller;
import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class SmallCreateVolumeTimeoutTest extends AbstractEc2OneServiceDynamicStorageTest {

	private ServiceInstaller installer;

	private static final String FOLDER_NAME = "small-create-volume-timeout";
	
	@BeforeClass(alwaysRun = true)
	protected void bootstrap() throws Exception {
		super.bootstrap();
	}
	
	
	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 4, enabled = true)
	public void testLinux() throws Exception {
		super.testLinux();
	}
	
	
	@AfterClass(alwaysRun = true)
	protected void teardown() throws Exception {
		super.teardown();
	}
	
	@Override
	public void doTest() throws Exception {
		installer = new ServiceInstaller(getRestUrl(), getServiceName());
		installer.recipePath(FOLDER_NAME);
		installer.setDisableSelfHealing(true);
		installer.install();
		
		// this service gives a very small timeout to the createVolume call. so we expect the StorageDriver to timeout while waiting for the instance status.
		// which should clean the created volume.
		super.scanForLeakedVolumesCreatedViaTemplate("SMALL_BLOCK");
	}

	@Override
	public String getServiceFolder() {
		return FOLDER_NAME;
	}


	@Override
	protected boolean isReusableCloud() {
		return false;
	}
}
