package test.cli.cloudify.cloud.ec2;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import test.cli.cloudify.cloud.AbstractExamplesTest;
import framework.utils.LogUtils;
import framework.utils.ScriptUtils;

public class Ec2GitExamplesTest extends AbstractExamplesTest{

	private String localGitAppsPath;
	private String localGitRepoPath;
	private static final String USER = "tgrid";
	private static final String PASSWORD = "tgrid";
	
	@Override
	protected String getCloudName() {
		return "ec2";
	}
	
	@BeforeClass(alwaysRun = true)
	protected void bootstrap() throws Exception {
		super.bootstrap();
		
	    localGitRepoPath = ScriptUtils.getBuildPath() + "/git-temp-repo";
	    String remotePath = "https://github.com/CloudifySource/cloudify-recipes.git";
	    
		LogUtils.log("exporting from github");
        Git.cloneRepository().setURI(remotePath).setDirectory(new File(localGitRepoPath)).call();
        
        localGitAppsPath = localGitRepoPath + "/apps";
	}
	
	@AfterClass(alwaysRun = true)
	protected void teardown() throws Exception {
		super.teardown();
		
		LogUtils.log("removing exported git folder");
		FileUtils.deleteDirectory(new File(localGitRepoPath));
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 4, enabled = false)
	public void testComputers() throws Exception {
		super.testComputers(localGitAppsPath);
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 4, enabled = false)
	public void testBabies() throws Exception {
		super.testBabies(localGitAppsPath);
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 4, enabled = false)
	public void testBiginsights() throws Exception {
		super.testBiginsights(localGitAppsPath);
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 4, enabled = false)
	public void testPetclinicJboss() throws Exception {
		super.testPetclinicJboss(localGitAppsPath);
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 4, enabled = false)
	public void testLamp() throws Exception {
		super.testLamp(localGitAppsPath);
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 4, enabled = false)
	public void testMasterSlave() throws Exception {
		super.testMasterSlave(localGitAppsPath);
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 4, enabled = false)
	public void testPetclinicWas() throws Exception {
		super.testPetclinicWas(localGitAppsPath);
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 4, enabled = false)
	public void testStorm() throws Exception {
		super.testStorm(localGitAppsPath);
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 4, enabled = false)
	public void testTravelLb () throws Exception {
		super.testTravelLb(localGitAppsPath);
	}
}
