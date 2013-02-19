package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.ec2;

import java.io.IOException;

import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
import org.cloudifysource.quality.iTests.test.cli.cloudify.CommandTestUtils;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.AbstractStorageTest;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * 
 * This test executes some custom commands on the service instance and 
 * verifies the volume was mounted and is writable.
 *  
 * @author adaml
 *
 */
public class Ec2StorageTest extends AbstractStorageTest{

	private static final String SERVICE_NAME = "simpleStorage";
	private static String SERVICE_PATH = CommandTestUtils.getPath("src/main/resources/apps/USM/usm/" + SERVICE_NAME);
	private static String EXPECTED_LIST_MOUNT_OUTPUT = "/dev/xvdc on /home/ec2-user/storage type ext4 (rw)";
	
	@Override
	protected String getCloudName() {
		return "ec2";
	}
	
	@BeforeClass(alwaysRun = true)
	protected void bootstrap() throws Exception {
		super.bootstrap();
		installServiceAndWait(SERVICE_PATH, SERVICE_NAME);
	}
	
	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 4, enabled = true)
	public void testWriteToStorage() throws IOException, InterruptedException {
		super.testWriteToStorage();
	}
	
	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 4, enabled = true)
	protected void testStorageVolumeMounted() throws IOException, InterruptedException {
		super.testStorageVolumeMounted(EXPECTED_LIST_MOUNT_OUTPUT);
	}
	
	@AfterClass(alwaysRun = true)
	protected void teardown() throws Exception {
		super.teardown();
	}

	@Override
	protected boolean isReusableCloud() {
		return false;
	}
}
