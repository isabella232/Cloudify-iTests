package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.ec2;

import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.AbstractStorageTest;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.services.ec2.Ec2CloudService;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Author: nirb
 * Date: 20/02/13
 */
public class Ec2StorageDeleteOnExitTest extends AbstractStorageTest {

    @BeforeMethod(alwaysRun = true)
    public void init() throws Exception{
        super.bootstrapAndInit();
    }

    @AfterMethod(alwaysRun = true)
    public void cleanup() throws Exception{
        super.cleanup(true);
    }

    @Test(timeOut = DEFAULT_TEST_TIMEOUT * 3, enabled = true)
    public void testDeleteOnExitFalse() throws Exception{
        super.testDeleteOnExitFalse();
    }

    @Override
    protected String getCloudName() {
        return "ec2";
    }

    @Override
    protected boolean isReusableCloud() {
        return false;
    }

    protected void customizeCloud() throws Exception {
        ((Ec2CloudService)getService()).getAdditionalPropsToReplace().put("deleteOnExit true", "deleteOnExit false");
    }

}
