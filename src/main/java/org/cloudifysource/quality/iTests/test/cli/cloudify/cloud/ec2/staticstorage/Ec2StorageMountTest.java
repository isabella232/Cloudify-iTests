package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.ec2.staticstorage;

import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.AbstractStorageTest;
import org.testng.annotations.*;

/**
 * Author: nirb
 * Date: 28/02/13
 */
public class Ec2StorageMountTest extends AbstractEc2OneServiceStaticStorageTest {

    private static final String FOLDER_NAME = "simple-storage-with-custom-commands";

    @BeforeClass(alwaysRun = true)
    protected void bootstrap() throws Exception {
        super.bootstrap();
    }

    @Override
    public void doTest() throws Exception {
        super.testMount(FOLDER_NAME);
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
