package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.ec2.Persistency;

import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.AbstractCloudManagementPersistencyTest;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * User: nirb
 * Date: 14/03/13
 */
public class Ec2CorruptedPersistencyDirectoryTest extends AbstractCloudManagementPersistencyTest {

    @BeforeMethod(alwaysRun = true)
    public void bootstrapAndInit() throws Exception{
        super.bootstrapAndInit(false, false);
    }

    @AfterMethod(alwaysRun = true)
    public void afterTest() throws Exception{
        super.afterTest();
    }

    @Test(timeOut = DEFAULT_TEST_TIMEOUT * 4, enabled = false)
    public void testCorruptedPersistencyDirectory() throws Exception {
        super.testCorruptedPersistencyDirectory();
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
