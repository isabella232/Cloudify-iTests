package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.hp.storage;

import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.AbstractStorageTest;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Author: nirb
 * Date: 21/02/13
 */
public class HpSimpleStorageTest extends AbstractStorageTest{

    @BeforeMethod(alwaysRun = true)
    public void bootstrapAndInit() throws Exception{
        super.bootstrapAndInit();
    }

    @AfterMethod(alwaysRun = true)
    public void cleanup() throws Exception{
        super.cleanup();
    }

    @Test(timeOut = DEFAULT_TEST_TIMEOUT * 4, enabled = true)
    public void testInstallWithStorage() throws Exception{
        super.testInstallWithStorage();
    }

    @Override
    protected String getCloudName() {
        return "hp";
    }

    @Override
    protected boolean isReusableCloud() {
        return false;
    }
}
