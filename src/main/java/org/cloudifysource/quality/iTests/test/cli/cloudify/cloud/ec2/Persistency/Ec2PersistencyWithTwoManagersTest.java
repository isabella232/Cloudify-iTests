package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.ec2.Persistency;

import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.AbstractCloudManagementPersistencyTest;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * User: nirb
 * Date: 06/03/13
 */
public class Ec2PersistencyWithTwoManagersTest extends AbstractCloudManagementPersistencyTest {

    @BeforeMethod(alwaysRun = true)
    public void bootstrapAndInit() throws Exception{
        super.bootstrapAndInit();
    }

    @AfterMethod(alwaysRun = true)
    public void afterTest() throws Exception{
        super.afterTest();
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

    @Override
    protected String getCloudName() {
        return "ec2";
    }

    @Override
    protected boolean isReusableCloud() {
        return false;
    }
}
