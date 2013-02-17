package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.ec2;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.AbstractExamplesTest;
import org.eclipse.jgit.api.Git;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.AbstractExamplesTest;
import org.cloudifysource.quality.iTests.framework.utils.LogUtils;
import org.cloudifysource.quality.iTests.framework.utils.ScriptUtils;

public class Ec2GitExamplesTest extends AbstractExamplesTest {

	private String localGitAppsPath;
	private String localGitRepoPath;
	
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
	
	//should work
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 4, enabled = true)
	public void testComputers() throws Exception {
		super.testComputers(localGitAppsPath);
	}
	
	//should work
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 4, enabled = true)
	public void testBabies() throws Exception {
		super.testBabies(localGitAppsPath);
	}
	
	//fails
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 4, enabled = false)
	public void testBiginsights() throws Exception {
		super.testBiginsights(localGitAppsPath);
	}
	
	//should work
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 4, enabled = true)
	public void testPetclinicJboss() throws Exception {
		super.testPetclinicJboss(localGitAppsPath);
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 4, enabled = true)
	public void testLamp() throws Exception {
		super.testLamp(localGitAppsPath);
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 4, enabled = true)
	public void testMasterSlave() throws Exception {
		super.testMasterSlave(localGitAppsPath);
	}
	
	//needs configuration to work.
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 4, enabled = false)
	public void testPetclinicWas() throws Exception {
		super.testPetclinicWas(localGitAppsPath);
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 4, enabled = true)
	public void testStorm() throws Exception {
		super.testStorm(localGitAppsPath);
	}
	
	//should work
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 4, enabled = true)
	public void testTravelLb () throws Exception {
		super.testTravelLb(localGitAppsPath);
	}
}
