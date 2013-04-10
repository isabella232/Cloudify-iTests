package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.ec2.staticstorage;

import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.concurrent.TimeoutException;

public class Ec2SimpleStorageTest extends AbstractEc2OneServiceStaticStorageTest {

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
        super.testInstallWithStorage(FOLDER_NAME);
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
