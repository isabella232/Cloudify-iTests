package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.ec2.staticstorage;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.AbstractStorageTest;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
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
public class Ec2WriteToStorageTest extends AbstractEc2OneServiceStaticStorageTest {

    private static final String FOLDER_NAME = "simple-storage-with-custom-commands";

	private static String EXPECTED_LIST_MOUNT_OUTPUT = "/dev/xvdc on /home/ec2-user/storage type ext4 (rw)";

	@BeforeClass(alwaysRun = true)
	protected void bootstrap() throws Exception {
		super.bootstrap();
	}

    @Override
    public void doTest() throws Exception {
        super.testWriteToStorage(FOLDER_NAME);
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
	protected boolean isReusableCloud() {
		return false;
	}

    @Override
    public String getServiceFolder() {
        return FOLDER_NAME;
    }
}
