package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.hp.storage;

import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.AbstractStorageTest;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.services.hp.HpCloudService;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Author: nirb
 * Date: 21/02/13
 */
public class HpStorageDeleteOnExitTest extends AbstractStorageTest {

    @BeforeClass(alwaysRun = true)
    public void init() throws Exception{
        super.bootstrapAndInit();
    }

    @AfterClass(alwaysRun = true)
    public void cleanup() throws Exception{
        super.cleanup(true);
    }

    @Test(timeOut = DEFAULT_TEST_TIMEOUT * 3, enabled = true)
    public void testDeleteOnExitFalse() throws Exception{
        super.testDeleteOnExitFalse();
    }

    @Override
    protected String getCloudName() {
        return "hp";
    }

    @Override
    protected boolean isReusableCloud() {
        return false;
    }

    protected void customizeCloud() throws Exception {
        ((HpCloudService)getService()).getAdditionalPropsToReplace().put("deleteOnExit true", "deleteOnExit false");
    }

}
