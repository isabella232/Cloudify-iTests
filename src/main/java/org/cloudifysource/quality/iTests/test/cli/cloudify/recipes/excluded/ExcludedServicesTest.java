package org.cloudifysource.quality.iTests.test.cli.cloudify.recipes.excluded;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.cloudifysource.quality.iTests.framework.testng.annotations.TestConfiguration;
import org.cloudifysource.quality.iTests.framework.utils.ScriptUtils;
import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
import org.cloudifysource.quality.iTests.test.cli.cloudify.AbstractLocalCloudTest;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepository;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;


public class ExcludedServicesTest extends AbstractLocalCloudTest {
    public static volatile boolean portReleasedBeforTimeout;
    protected static volatile boolean portTakenBeforTimeout;
    private static String localPath, remotePath;
    private static Repository localRepo;
    private static Git git;
    private File excludedRecipesDir;


    public ExcludedServicesTest(){
        super();
    }

    @BeforeClass(alwaysRun = true)
    public void cloneRecipesRepository() throws Exception{
        super.bootstrapIfNeeded();
        localPath = ScriptUtils.getBuildPath() + "/excludedRecipes";
        remotePath = "https://github.com/CloudifySource/cloudify-recipes.git";
        localRepo = new FileRepository(localPath + "/.git");
        git = new Git(localRepo);
        excludedRecipesDir = new File(localPath + "/services");
        Git.cloneRepository()
                .setURI(remotePath)
                .setDirectory(new File(localPath))
                .call();

    }

    @BeforeMethod(alwaysRun = true)
    public void beforeTest() throws Exception {
        portReleasedBeforTimeout = false;
        portTakenBeforTimeout = false;
    }

    //should work
    @TestConfiguration(os = {TestConfiguration.VM.MAC, TestConfiguration.VM.UNIX} )
    @Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 2, enabled = false)
    public void installApache() throws Exception{
        doTest("apache");
    }


    //cant run on localcloud??
    @Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 2, enabled = false)
    public void installCouchbase() throws Exception{
        doTest("couchbase");
    }

    //Recipe is not finish
    @Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 2, enabled = false)
    public void installCouchDB() throws Exception{
        doTest("couchdb");
    }
    //no groovy file. not a service?
    @Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 2, enabled = false)
    public void installBigInsights() throws Exception{
        doTest("biginsights");
    }
    //linux only
    @TestConfiguration(os = {TestConfiguration.VM.MAC, TestConfiguration.VM.UNIX} )
    @Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 2, enabled = false)
    public void installElasticsearch() throws Exception{
        doTest("elasticsearch");
    }

    //dont work
    @Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 2, enabled = false)
    public void installGroovyUtils() throws Exception{
        doTest("groovy-utils");
    }
    //dont work
    @Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 2, enabled = false)
    public void installJboss() throws Exception{
        doTest("jboss");
    }

    //works
    @Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 2, enabled = true)
    public void installMysql() throws Exception{
        doTest("mysql");
    }

    //dont work
    @Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 2, enabled = false)
    public void installNginx() throws Exception{
        doTest("nginx");
    }

    //dont work
    @Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 2, enabled = false)
    public void installPlay() throws Exception{
        doTest("play");
    }

    //dont work
    @Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 2, enabled = false)
    public void installPostgresql() throws Exception{
        doTest("postgresql");
    }

    //ubuntu only
    @TestConfiguration(os = TestConfiguration.VM.UNIX)
    @Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 2, enabled = false)
    public void installPuppet() throws Exception{
        doTest("puppet");
    }

    //dont work
    @Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 2, enabled = false)
    public void installRails() throws Exception{
        doTest("rails");
    }

    //Recipe is not finish
    @Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 2, enabled = false)
    public void installRedis() throws Exception{
        doTest("redis");
    }

    //should work
    @Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 2, enabled = false)
    public void installSolr() throws Exception{
        doTest("solr");
    }

    //didnt run it
    @Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 2, enabled = false)
    public void installStorm() throws Exception{
        doTest("storm");
    }
    //linux only
    @TestConfiguration(os = {TestConfiguration.VM.MAC, TestConfiguration.VM.UNIX} )
    @Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 2, enabled = false)
    public void installVertx() throws Exception{
        doTest("vertx");
    }

    //linux only
    @TestConfiguration(os = {TestConfiguration.VM.MAC, TestConfiguration.VM.UNIX})
    @Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 2, enabled = false)
    public void installWebshere() throws Exception{
        doTest("websphere");
    }

    //linux only
    @TestConfiguration(os = {TestConfiguration.VM.MAC, TestConfiguration.VM.UNIX} )
    @Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 2, enabled = false)
    public void installZookeeper() throws Exception{
        doTest("zookeeper");
    }

    private void doTest(String recipeName) throws IOException, InterruptedException {
        String recipeDirPath = excludedRecipesDir + "/" + recipeName;
        runCommand("connect " + restUrl + ";install-service --verbose " + recipeDirPath);
        runCommand("connect " + restUrl + ";uninstall-service --verbose " + recipeName);
    }

    @AfterClass(alwaysRun = true)
    public void afterClass() throws Exception {
       FileUtils.deleteDirectory(new File(localPath));

    }


}

