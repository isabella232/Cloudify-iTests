package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.ec2.staticstorage;

import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.AbstractStorageTest;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.services.ec2.Ec2CloudService;
import org.testng.annotations.*;

import java.util.concurrent.TimeoutException;

/**
 * Author: nirb
 * Date: 20/02/13
 */
public class Ec2StorageDeleteOnExitTest extends AbstractEc2OneServiceStaticStorageTest {

    private static final String FOLDER_NAME = "simple-storage";

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


    @Override
    public void doTest() throws Exception {
        super.testDeleteOnExitFalse(FOLDER_NAME);
    }

    @AfterMethod
    public void scanForLeakes() throws TimeoutException {
        super.scanForLeakedVolumesCreatedViaTemplate("SMALL_BLOCK");
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
