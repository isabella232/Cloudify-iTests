package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.ec2;

import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.AbstractStorageTest;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.services.ec2.Ec2CloudService;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Author: nirb
 * Date: 25/02/13
 */
public class Ec2StorageFailedInstallTest extends AbstractStorageTest {

    @BeforeMethod(alwaysRun = true)
    public void bootstrapAndInit() throws Exception{
        super.bootstrapAndInit();
    }

    @AfterMethod(alwaysRun = true)
    public void cleanup() throws Exception{
        super.cleanup();
    }

    @Test(timeOut = DEFAULT_TEST_TIMEOUT * 4, enabled = true)
    public void testFailedInstall() throws Exception{
         super.testFailedInstall();
    }

    protected void customizeCloud() throws Exception {
        ((Ec2CloudService)getService()).getAdditionalPropsToReplace().put("deviceName \"/dev/sdc\"", "deviceName \"/dev/falsename\"");
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
