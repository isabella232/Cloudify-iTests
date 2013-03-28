package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.ec2.storage;

import java.io.IOException;

import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.AbstractStorageTest;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.services.CloudServiceManager;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.services.ec2.Ec2CloudService;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class Ec2StorageInExplicitAvailabilityZoneTest extends AbstractStorageTest{

	private static String EXPECTED_LIST_MOUNT_OUTPUT = "/dev/xvdc on /home/ec2-user/storage type ext4 (rw)";
	
	@Override
	protected String getCloudName() {
		return "ec2";
	}
	
	@BeforeClass(alwaysRun = true)
	protected void bootstrap() throws Exception {
		Ec2CloudService cloudService = (Ec2CloudService) CloudServiceManager.getInstance().getCloudService(getCloudName());
		cloudService.setAvailabilityZone("c");
		super.bootstrapAndInit(cloudService);
		installServiceAndWait(SERVICE_PATH, SERVICE_NAME);
	}
	
	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 4, enabled = true)
	protected void testStorageVolumeMounted() throws IOException, InterruptedException {
		super.testStorageVolumeMounted(EXPECTED_LIST_MOUNT_OUTPUT);
	}
	
	@AfterClass(alwaysRun = true)
	protected void teardown() throws Exception {
		super.cleanup();
	}


	@Override
	protected boolean isReusableCloud() {
		return false;
	}
}
