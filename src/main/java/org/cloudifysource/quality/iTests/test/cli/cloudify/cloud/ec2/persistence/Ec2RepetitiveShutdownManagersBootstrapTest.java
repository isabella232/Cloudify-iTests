package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.ec2.persistence;

import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.AbstractCloudManagementPersistencyTest;
import org.testng.annotations.*;

/**
 * User: nirb
 * Date: 07/03/13
 */
public class Ec2RepetitiveShutdownManagersBootstrapTest extends AbstractCloudManagementPersistencyTest {

    @BeforeClass(alwaysRun = true)
    public void bootstrapAndInit() throws Exception{
        super.bootstrap();
        super.installTomcatService(2, null);
    }

    @Test(timeOut = DEFAULT_TEST_TIMEOUT * 4, enabled = true, groups = SUSPECTED)
    public void testRepetitiveShutdownManagersBootstrap() throws Exception {
        super.testRepetitiveShutdownManagersBootstrap();
    }

    @AfterClass(alwaysRun = true)
    protected void teardown() throws Exception {
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
