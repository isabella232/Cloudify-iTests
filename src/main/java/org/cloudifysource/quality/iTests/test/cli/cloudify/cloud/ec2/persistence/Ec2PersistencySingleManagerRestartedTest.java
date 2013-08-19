package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.ec2.persistence;

import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.AbstractCloudManagementPersistencyTest;
import org.testng.annotations.*;

/**
 * @author Itai Frenkel
 * Date: 19/08/13
 */
public class Ec2PersistencySingleManagerRestartedTest extends AbstractCloudManagementPersistencyTest {

    @BeforeClass(alwaysRun = true)
    public void bootstrapAndInit() throws Exception{
        super.setNumOfManagementMachines(1);
    	super.bootstrap();
        super.initManagementUrlsAndRestClient();
        super.installTomcatService(1, null);
    }

    /** 
     * Do not call super.teardown(), since it will detect leaked management machine (that this test caused).
     * Instead just terminate all management machines.
     */
    @AfterClass(alwaysRun = true)
    public void teardown() throws Exception{
    	super.teardown();
    }

    /**
     * This test is tied to CLOUDIFY-1988 does not actually assert anything significant, we are inspecting the logs looking for the reproduction. 
     */
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, enabled = true, invocationCount=10)
    public void testSingleManagerResterted() throws Exception {
        super.testSingleManagerResterted();
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
