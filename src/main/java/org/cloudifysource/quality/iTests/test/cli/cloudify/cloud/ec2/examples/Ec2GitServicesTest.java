package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.ec2.examples;

import iTests.framework.testng.annotations.TestConfiguration;
import iTests.framework.tools.SGTestHelper;
import iTests.framework.utils.ScriptUtils;

import org.cloudifysource.quality.iTests.framework.utils.JGitUtils;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.AbstractServicesTest;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;


public class Ec2GitServicesTest extends AbstractServicesTest {

	private static String localGitRepoPath;
    private static String BRANCH_NAME = SGTestHelper.getBranchName();

    @Override
    protected String getCloudName() {
        return "ec2";
    }

    @BeforeClass(alwaysRun = true)
    protected void bootstrap() throws Exception {
    	super.bootstrap();
        localGitRepoPath = ScriptUtils.getBuildPath() + "/git-recipes-" + this.getClass().getSimpleName() ;
        String remotePath = "https://github.com/CloudifySource/cloudify-recipes.git";
        JGitUtils.clone(localGitRepoPath, remotePath, BRANCH_NAME);
    }

    @Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, enabled = true)
    public void testApache() throws Exception {
        testService("apache");
    }

    @Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, enabled = true)
    public void testApacheLB() throws Exception {
        testService("apacheLB");
    }

    //tested as part of an app
    @Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, enabled = false)
    public void testBigInsights() throws Exception{
        testService("biginsights");
    }
    
    //cant run on localcloud??
    @Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, enabled = true)
    public void testCouchbase() throws Exception{
    	testService("couchbase");
    }

    //not our recipe. fails.
    @Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, enabled = false)
    public void testCouchDBe() throws Exception{
        testService("couchdb");
    }

    
    //works
    @TestConfiguration(os = {TestConfiguration.VM.MAC, TestConfiguration.VM.UNIX} )
    @Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, enabled = true)
    public void testElasticSearch() throws Exception{
        testService("elasticsearch");
    }

    //should not be tested.
    @Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, enabled = false)
    public void testGroovyUtils() throws Exception{
        testService("groovy-utils");
    }
    //should work
    @Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, enabled = true)
    public void testJboss() throws Exception{
        testService("jboss", 20);
    }

    //works
    @Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, enabled = true)
    public void testMysql() throws Exception{
        testService("mysql");
    }

    //not our recipe. fails.
    @Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, enabled = false)
    public void testNginx() throws Exception{
        testService("nginx");
    }

    //needs configuration to work.
    @Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, enabled = false)
    public void testPlay() throws Exception{
        testService("play");
    }

    //not our recipe. fails.
    @Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, enabled = false)
    public void testPostgresql() throws Exception{
        testService("postgresql");
    }

    //ubuntu only. works
    @TestConfiguration(os = TestConfiguration.VM.UNIX)
    @Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, enabled = true)
    public void testPuppet() throws Exception{
        testService("puppet");
    }

    //dont work. should work as part of an app.
    @Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, enabled = false)
    public void testRails() throws Exception{
        testService("rails");
    }

    //Recipe is not finish
    @Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, enabled = false)
    public void testRedis() throws Exception{
        testService("redis");
    }

    //works
    @Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, enabled = true)
    public void testSolr() throws Exception{
        testService("solr");
    }

    //depends on zookeeper??
    @Test(timeOut = DEFAULT_TEST_TIMEOUT * 3, enabled = false)
    public void testStormNimbus() throws Exception{
        testService("storm/storm-nimbus");
    }
    
    //should work. takes time to install.
    @Test(timeOut = DEFAULT_TEST_TIMEOUT * 3, enabled = false)
    public void testStormSupervisor() throws Exception{
        testService("storm/storm-supervisor");
    }
    
    //linux only. works
    @TestConfiguration(os = {TestConfiguration.VM.MAC, TestConfiguration.VM.UNIX} )
    @Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, enabled = true)
    public void testVertx() throws Exception{
        testService("vertx");
    }

    //linux only. doesn't work. needs configuration to work.
    @TestConfiguration(os = {TestConfiguration.VM.MAC, TestConfiguration.VM.UNIX})
    @Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, enabled = false)
    public void testWebshere() throws Exception{
        testService("websphere");
    }

    //linux only. works
    @TestConfiguration(os = {TestConfiguration.VM.MAC, TestConfiguration.VM.UNIX} )
    @Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, enabled = true)
    public void testZookeeper() throws Exception{
        testService("zookeeper");
    }
    
    @AfterClass(alwaysRun = true)
    protected void teardown() throws Exception {
        super.teardown();
    }
    
    private void testService(String serviceName) throws Exception {
    	testService(localGitRepoPath + "/services/" + serviceName, null);
    }

    private void testService(final String serviceName, final int timeoutInMinutes) throws Exception {
        testService(localGitRepoPath + "/services/" + serviceName, null, timeoutInMinutes);
    }
}

