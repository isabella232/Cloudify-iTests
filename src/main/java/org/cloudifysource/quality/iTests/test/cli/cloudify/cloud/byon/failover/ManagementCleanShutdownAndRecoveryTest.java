package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.byon.failover;

import org.cloudifysource.quality.iTests.framework.utils.CloudBootstrapper;
import org.cloudifysource.quality.iTests.framework.utils.LogUtils;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * User: nirb
 * Date: 05/03/13
 */
public class ManagementCleanShutdownAndRecoveryTest extends AbstractByonManagementPersistencyTest{

    @BeforeMethod(alwaysRun = true)
    public void bootstrapAndInit() throws Exception{
        super.prepareTest();
    }

    @AfterMethod(alwaysRun = true)
    public void cleanup() throws Exception{
//        super.cleanup(true);
    }

    @Test(timeOut = DEFAULT_TEST_TIMEOUT * 4, enabled = true)
    public void testManagementPersistency() throws Exception {
        super.testManagementPersistency();
    }

    @Override
    public void shutdownManagement() throws Exception {

        CloudBootstrapper bootstrapper = new CloudBootstrapper();
        bootstrapper.setRestUrl(getRestUrl());

        LogUtils.log("shutting down managers");
        bootstrapper.shutdownManagers("default", BACKUP_FILE_PATH, false);

    }
}
