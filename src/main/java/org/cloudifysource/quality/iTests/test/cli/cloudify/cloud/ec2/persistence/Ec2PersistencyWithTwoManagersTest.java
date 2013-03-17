package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.ec2.persistence;

import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.AbstractCloudManagementPersistencyTest;
import org.testng.annotations.*;

/**
 * User: nirb
 * Date: 06/03/13
 */
public class Ec2PersistencyWithTwoManagersTest extends AbstractCloudManagementPersistencyTest {

    @BeforeClass(alwaysRun = true)
    public void bootstrapAndInit() throws Exception{
        super.bootstrapAndInstallService();
    }

    @Override
    protected void customizeCloud() throws Exception {
        super.customizeCloud();
        getService().setNumberOfManagementMachines(getNumOfManagementMachines());
    }

    @Test(timeOut = DEFAULT_TEST_TIMEOUT * 4, enabled = true)
    public void testManagementPersistency() throws Exception {
        super.testManagementPersistency();
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
