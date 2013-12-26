package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.openstack.examples;

import iTests.framework.tools.SGTestHelper;
import iTests.framework.utils.JGitUtils;
import iTests.framework.utils.ScriptUtils;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.AbstractExamplesTest;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Test
public class OpenstackGitApplicationTest extends AbstractExamplesTest {

    private static String localGitRepoPath;
    private static String BRANCH_NAME = SGTestHelper.getBranchName();

    @Override
    protected String getCloudName() {
        return "openstack";
    }

    @BeforeClass(alwaysRun = true)
    protected void bootstrap() throws Exception {
        super.bootstrap();
        localGitRepoPath = ScriptUtils.getBuildPath() + "/git-recipes-" + this.getClass().getSimpleName() ;
        String remotePath = "https://github.com/CloudifySource/cloudify-recipes.git";
        JGitUtils.clone(localGitRepoPath, remotePath, BRANCH_NAME);
    }

    //should work
    @Test(timeOut = DEFAULT_TEST_TIMEOUT * 4, enabled = true)
    public void testComputers() throws Exception {
        super.testComputers(localGitRepoPath + "/apps");
    }

    //should work
    @Test(timeOut = DEFAULT_TEST_TIMEOUT * 4, enabled = true)
    public void testDrupalBabies() throws Exception {
        super.testDrupalBabies(localGitRepoPath + "/apps");
    }

    @Test(timeOut = DEFAULT_TEST_TIMEOUT * 4, enabled = true)
    public void testDrupalChef() throws Exception {
        super.testDrupalChef(localGitRepoPath + "/apps");
    }

    @Test(timeOut = DEFAULT_TEST_TIMEOUT * 4, enabled = true)
    public void testBiginsights() throws Exception {
        super.testBiginsights(localGitRepoPath + "/apps");
    }

    @Test(timeOut = DEFAULT_TEST_TIMEOUT * 4, enabled = true)
    public void testMasterSlave() throws Exception {
        super.testMasterSlave(localGitRepoPath + "/apps");
    }

    @Test(timeOut = DEFAULT_TEST_TIMEOUT * 4, enabled = true)
    public void testLamp() throws Exception {
        super.testLamp(localGitRepoPath + "/apps");
    }

    @Test(timeOut = DEFAULT_TEST_TIMEOUT * 4, enabled = true)
    public void tesMysqlAdmin() throws Exception {
        super.testMySQLAdmin(localGitRepoPath + "/apps");
    }

    //should work
    @Test(timeOut = DEFAULT_TEST_TIMEOUT * 4, enabled = true)
    public void testPetclinicJboss() throws Exception {
        super.testPetclinicJboss(localGitRepoPath + "/apps");
    }

    @Test(timeOut = DEFAULT_TEST_TIMEOUT * 4, enabled = true)
    public void testRedmine() throws Exception {
        super.testRedmine(localGitRepoPath + "/apps");
    }

    @Test(timeOut = DEFAULT_TEST_TIMEOUT * 4, enabled = true)
    public void testRedminePuppet() throws Exception {
        super.testRedminePuppet(localGitRepoPath + "/apps");
    }

    @Test(timeOut = DEFAULT_TEST_TIMEOUT * 4, enabled = true)
    public void testHelloPuppet() throws Exception {
        super.testHelloPuppet(localGitRepoPath + "/apps");
    }

    // requires a non existing template
    @Test(timeOut = DEFAULT_TEST_TIMEOUT * 4, enabled = false)
    public void testStorm() throws Exception {
        super.testStorm(localGitRepoPath + "/apps");
    }

    @Test(timeOut = DEFAULT_TEST_TIMEOUT * 4, enabled = false)
    public void testXAPStream() throws Exception {
        super.testXAPStream(localGitRepoPath + "/apps");
    }

    //needs configuration to work.
    @Test(timeOut = DEFAULT_TEST_TIMEOUT * 4, enabled = false)
    public void testPetclinicWas() throws Exception {
        super.testPetclinicWas(localGitRepoPath + "/apps");
    }

    //should work
    @Test(timeOut = DEFAULT_TEST_TIMEOUT * 4, enabled = false)
    public void testTravelLb() throws Exception {
        super.testTravelLb(localGitRepoPath + "/apps");
    }


    @Test(timeOut = DEFAULT_TEST_TIMEOUT * 4, enabled = false)
    public void testMysqlJboss() throws Exception {
        super.testMysqlJboss(localGitRepoPath + "/apps");
    }

    @Test(timeOut = DEFAULT_TEST_TIMEOUT * 4, enabled = true)
    public void testHttpSession() throws Exception {
        super.testHttpSession(localGitRepoPath + "/apps");
    }

    @AfterClass(alwaysRun = true)
    protected void teardown() throws Exception {
        super.teardown();
    }
}
