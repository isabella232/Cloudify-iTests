package test.cli.cloudify.recipes;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import test.cli.cloudify.AbstractLocalCloudTest;
import framework.utils.LogUtils;
import framework.utils.ScriptUtils;

public class GitExamplesTest extends AbstractLocalCloudTest{

	private String localGitAppsPath;
	private String localGitRepoPath;
	private static final String USER = "tgrid";
	private static final String PASSWORD = "tgrid";
	
	@BeforeClass(alwaysRun = true)
	protected void bootstrap() throws Exception {
		
	    localGitRepoPath = ScriptUtils.getBuildPath() + "/git-temp-repo";
	    String remotePath = "https://github.com/CloudifySource/cloudify-recipes.git";
	    
		LogUtils.log("exporting recipes from github");
        Git.cloneRepository().setURI(remotePath).setDirectory(new File(localGitRepoPath)).call();
        
        localGitAppsPath = localGitRepoPath + "/apps";
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 4, enabled = false)
	public void testComputers() throws Exception {
		super.doTest(localGitAppsPath, "computers", "computers");
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 4, enabled = false)
	public void testBabies() throws Exception {
		super.doTest(localGitAppsPath, "drupal-babies", "babies");
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 4, enabled = false)
	public void testBiginsights() throws Exception {
		super.doTest(localGitAppsPath, "hadoop-biginsights", "biginsights");
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 4, enabled = false)
	public void testLamp() throws Exception {
		super.doTest(localGitAppsPath, "lamp", "lamp");
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 4, enabled = false)
	public void testMasterSlave() throws Exception {
		super.doTest(localGitAppsPath, "masterslave", "masterslave");
	}

	@AfterClass(alwaysRun = true)
	protected void teardown() throws Exception {
		LogUtils.log("removing exported git folder");
		FileUtils.deleteDirectory(new File(localGitRepoPath));
	}
}
