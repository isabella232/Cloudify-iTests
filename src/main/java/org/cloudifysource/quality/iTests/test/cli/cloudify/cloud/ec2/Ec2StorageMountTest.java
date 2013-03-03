package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.ec2;

import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.AbstractStorageTest;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Author: nirb
 * Date: 28/02/13
 */
public class Ec2StorageMountTest extends AbstractStorageTest {

    @BeforeMethod(alwaysRun = true)
    public void bootstrapAndInit() throws Exception{
        super.bootstrapAndInit();
    }

    @AfterMethod(alwaysRun = true)
    public void cleanup() throws Exception{
        super.cleanup();
    }

    @Test(timeOut = DEFAULT_TEST_TIMEOUT * 4, enabled = true)
    public void testMount() throws Exception {
        super.testMount();
    }

    @Override
    protected String getCloudName() {
        return "ec2";
    }

    @Override
    protected boolean isReusableCloud() {
        return false;
    }
}
