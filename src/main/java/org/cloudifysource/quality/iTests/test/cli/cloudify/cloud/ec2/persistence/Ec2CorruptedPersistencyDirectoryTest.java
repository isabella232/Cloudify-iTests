package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.ec2.persistence;

import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.AbstractCloudManagementPersistencyTest;
import org.testng.annotations.*;

/**
 * User: nirb
 * Date: 14/03/13
 */
public class Ec2CorruptedPersistencyDirectoryTest extends AbstractCloudManagementPersistencyTest {

    @BeforeClass(alwaysRun = true)
    public void bootstrapAndInit() throws Exception{
        super.bootstrapAndInstallService(false, false);
    }

    @Test(timeOut = DEFAULT_TEST_TIMEOUT * 4, enabled = false)
    public void testCorruptedPersistencyDirectory() throws Exception {
        super.testCorruptedPersistencyDirectory();
    }

    @AfterClass(alwaysRun = true)
    public void teardown() throws Exception{
        super.teardown();
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
