package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.ec2.persistence;

import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.AbstractCloudManagementPersistencyTest;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * User: nirb
 * Date: 13/03/13
 */
public class Ec2PersistenceWithMultipleServicesTest extends AbstractCloudManagementPersistencyTest{

    @BeforeClass(alwaysRun = true)
    public void bootstrapAndInit() throws Exception{
        super.bootstrap();
        super.installTomcatService(1, "tomcat-1");
        super.installTomcatService(1, "tomcat-2");
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
