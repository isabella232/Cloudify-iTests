package test.cli.cloudify;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.testng.annotations.Test;

import test.cli.cloudify.security.SecurityConstants;
import framework.tools.SGTestHelper;
import framework.utils.ApplicationInstaller;

public class LocalCloudSecurityTest extends AbstractSecuredLocalCloudTest{

	private static final String APP_NAME = "simple";
	private static final String APP_PATH = "\\src\\main\\resources\\apps\\USM\\usm\\applications\\" + APP_NAME;
	
	private static final String APP_NAME_2 = "travelExtended";
	private static final String APP_PATH_2 = "\\src\\main\\resources\\apps\\USM\\usm\\applications\\" + APP_NAME_2;

	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = false)
	public void installAndUninstallWithCloudAdminTest() throws IOException, InterruptedException {

		ApplicationInstaller appInstaller = new ApplicationInstaller(restUrl, APP_NAME);
		
		String output = appInstaller.setCloudifyUsername(SecurityConstants.CLOUD_ADMIN_USER_PWD).setCloudifyPassword(SecurityConstants.CLOUD_ADMIN_USER_PWD).setRecipePath(SGTestHelper.getSGTestRootDir() + APP_PATH).install();
		appInstaller.assertInstall(output);
		
		output = appInstaller.uninstall();
		appInstaller.assertUninstall(output);
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = false)
	public void installAndUninstallWithDifferentUsersTest() throws IOException, InterruptedException {
		
		ApplicationInstaller appInstaller = new ApplicationInstaller(restUrl, APP_NAME);
		
		String output = appInstaller.setCloudifyUsername(SecurityConstants.APP_MANAGER_AND_VIEWER_USER_PWD).setCloudifyPassword(SecurityConstants.APP_MANAGER_AND_VIEWER_USER_PWD).setRecipePath(SGTestHelper.getSGTestRootDir() + APP_PATH).install();
		appInstaller.assertInstall(output);
		
		output = appInstaller.setCloudifyUsername(SecurityConstants.VIEWER_USER_PWD).setCloudifyPassword(SecurityConstants.VIEWER_USER_PWD).setExpectToFail(true).uninstall();
		appInstaller.assertUninstall(output);
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = false)
	public void installWithViewerTest() throws IOException, InterruptedException{

		ApplicationInstaller appInstaller = new ApplicationInstaller(restUrl, APP_NAME);
		String output = appInstaller.setCloudifyUsername(SecurityConstants.VIEWER_USER_PWD).setCloudifyPassword(SecurityConstants.VIEWER_USER_PWD).setRecipePath(SGTestHelper.getSGTestRootDir() + APP_PATH).setExpectToFail(true).install();

		assertTrue("install access granted to " + SecurityConstants.VIEWER_USER_PWD, output.contains("no_permission_access_is_denied"));
		appInstaller.assertInstall(output);
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = false)
	public void installWithoutCredentialsTest() throws IOException, InterruptedException{

		ApplicationInstaller appInstaller = new ApplicationInstaller(restUrl, APP_NAME);
		String output = appInstaller.setRecipePath(SGTestHelper.getSGTestRootDir() + APP_PATH).setExpectToFail(true).install();

		assertTrue("install access granted to an Anonymous user" , output.contains("bad_credentials"));
		appInstaller.assertInstall(output);
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void installingAndViewingTest() throws IOException, InterruptedException{

		String output = "no output";
		ApplicationInstaller appInstaller = new ApplicationInstaller(restUrl, APP_NAME);

		output = appInstaller.setCloudifyUsername(SecurityConstants.APP_MANAGER_USER_PWD).setCloudifyPassword(SecurityConstants.APP_MANAGER_USER_PWD).setRecipePath(SGTestHelper.getSGTestRootDir() + APP_PATH).install();		
		appInstaller.assertInstall(output);
		
		output = appInstaller.setApplicationName(APP_NAME_2).setCloudifyUsername(SecurityConstants.APP_MANAGER_AND_VIEWER_USER_PWD).setCloudifyPassword(SecurityConstants.APP_MANAGER_AND_VIEWER_USER_PWD).setRecipePath(SGTestHelper.getSGTestRootDir() + APP_PATH_2).install();		
		appInstaller.assertInstall(output);

		output = listApplications(SecurityConstants.CLOUD_ADMIN_USER_PWD, SecurityConstants.CLOUD_ADMIN_USER_PWD);
		assertTrue(SecurityConstants.CLOUD_ADMIN_USER_PWD + " sees the application of " + SecurityConstants.APP_MANAGER_USER_PWD, !output.contains(APP_NAME));
		assertTrue(SecurityConstants.CLOUD_ADMIN_USER_PWD + " sees the application of " + SecurityConstants.APP_MANAGER_AND_VIEWER_USER_PWD, !output.contains(APP_NAME_2));

		output = listApplications(SecurityConstants.CLOUD_ADMIN_AND_APP_MANAGER_USER_PWD, SecurityConstants.CLOUD_ADMIN_AND_APP_MANAGER_USER_PWD);
		assertTrue(SecurityConstants.CLOUD_ADMIN_AND_APP_MANAGER_USER_PWD + " doesn't see the application of " + SecurityConstants.APP_MANAGER_USER_PWD, output.contains(APP_NAME));
		assertTrue(SecurityConstants.CLOUD_ADMIN_AND_APP_MANAGER_USER_PWD + " doesn't see the application of " + SecurityConstants.APP_MANAGER_AND_VIEWER_USER_PWD, output.contains(APP_NAME_2));

		output = listApplications(SecurityConstants.APP_MANAGER_USER_PWD, SecurityConstants.APP_MANAGER_USER_PWD);
		assertTrue(SecurityConstants.APP_MANAGER_USER_PWD + " doesn't see the application of " + SecurityConstants.APP_MANAGER_USER_PWD, output.contains(APP_NAME));
		assertTrue(SecurityConstants.APP_MANAGER_USER_PWD + " doesn't see the application of " + SecurityConstants.APP_MANAGER_AND_VIEWER_USER_PWD, output.contains(APP_NAME_2));

		output = listApplications(SecurityConstants.APP_MANAGER_AND_VIEWER_USER_PWD, SecurityConstants.APP_MANAGER_AND_VIEWER_USER_PWD);
		assertTrue(SecurityConstants.APP_MANAGER_AND_VIEWER_USER_PWD + " doesn't see the application of " + SecurityConstants.APP_MANAGER_USER_PWD, output.contains(APP_NAME));
		assertTrue(SecurityConstants.APP_MANAGER_USER_PWD + " doesn't see the application of " + SecurityConstants.APP_MANAGER_AND_VIEWER_USER_PWD, output.contains(APP_NAME_2));

		output = listApplications(SecurityConstants.VIEWER_USER_PWD, SecurityConstants.VIEWER_USER_PWD);
		assertTrue(SecurityConstants.VIEWER_USER_PWD + " sees the application of " + SecurityConstants.APP_MANAGER_USER_PWD, !output.contains(APP_NAME));
		assertTrue(SecurityConstants.APP_MANAGER_USER_PWD + " doesn't see the application of " + SecurityConstants.APP_MANAGER_AND_VIEWER_USER_PWD, output.contains(APP_NAME_2));

		output = listApplications(SecurityConstants.NO_ROLE_USER_PWD, SecurityConstants.NO_ROLE_USER_PWD);
		assertTrue(SecurityConstants.NO_ROLE_USER_PWD + " sees the application of " + SecurityConstants.APP_MANAGER_USER_PWD, !output.contains(APP_NAME));
		assertTrue(SecurityConstants.NO_ROLE_USER_PWD + " sees the application of " + SecurityConstants.APP_MANAGER_AND_VIEWER_USER_PWD, !output.contains(APP_NAME_2));

	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = false)
	public void loginTest() throws IOException, InterruptedException{

		String output = login(SecurityConstants.VIEWER_USER_PWD, SecurityConstants.VIEWER_USER_PWD);		
		assertTrue("login failed for user: " + SecurityConstants.VIEWER_USER_PWD, output.contains("Logged in successfully"));			

	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = false)
	public void connectWithNonexistentUserTest() throws IOException, InterruptedException{

		String output = connect(SecurityConstants.CLOUD_ADMIN_USER_PWD + "bad", SecurityConstants.CLOUD_ADMIN_USER_PWD);		
		assertTrue("connect succeeded for user: " + SecurityConstants.CLOUD_ADMIN_USER_PWD + "bad", output.contains("bad credentials"));			

	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = false)
	public void loginWithNonexistentUserTest() throws IOException, InterruptedException{

		String output = login(SecurityConstants.CLOUD_ADMIN_USER_PWD + "bad", SecurityConstants.CLOUD_ADMIN_USER_PWD);		
		assertTrue("login succeeded for user: " + SecurityConstants.CLOUD_ADMIN_USER_PWD + "bad", output.contains("Bad credentials"));			

	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = false)
	public void connectWithWrongPassword() {

		String output = connect(SecurityConstants.CLOUD_ADMIN_USER_PWD, SecurityConstants.CLOUD_ADMIN_USER_PWD + "bad");		
		assertTrue("connect succeeded for password: " + SecurityConstants.CLOUD_ADMIN_USER_PWD + "bad", output.contains("Access denied"));			

	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = false)
	public void loginWithWrongPassword() throws IOException, InterruptedException {

		String output = login(SecurityConstants.CLOUD_ADMIN_USER_PWD, SecurityConstants.CLOUD_ADMIN_USER_PWD + "bad");
		assertTrue("login succeeded for password: " + SecurityConstants.APP_MANAGER_USER_PWD + "bad", output.contains("Bad credentials"));			

	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = false)
	public void TamperWithSecurityFileTest() throws IOException, InterruptedException {

		String fakeCloudAdminUserAndPassword = "John";

		String originalFilePath = SGTestHelper.getBuildDir() + "\\config\\security\\spring-security.xml";
		String backupFilePath = SGTestHelper.getBuildDir() + "\\config\\security\\spring-security.xml.backup";
		String fakeFilePath = SGTestHelper.getSGTestRootDir() + "\\src\\main\\config\\security\\spring-security.xml";
		File originalFile = new File(originalFilePath);
		File backupFile = new File(backupFilePath);
		File fakeFile = new File(fakeFilePath);

		try{
			FileUtils.moveFile(originalFile, backupFile);
			FileUtils.copyFile(fakeFile, originalFile);

			ApplicationInstaller appInstaller = new ApplicationInstaller(restUrl, APP_NAME);
			String output = appInstaller.setCloudifyUsername(fakeCloudAdminUserAndPassword).setCloudifyPassword(fakeCloudAdminUserAndPassword).setRecipePath(SGTestHelper.getSGTestRootDir() + APP_PATH).install();

			assertTrue("install access granted to " + fakeCloudAdminUserAndPassword, output.contains("no_permission_access_is_denied"));
			appInstaller.assertInstall(output);
		}
		catch(AssertionError ae){
			FileUtils.deleteQuietly(originalFile);
			FileUtils.moveFile(backupFile, originalFile);			
		}
		catch(Exception e){
			FileUtils.deleteQuietly(originalFile);
			FileUtils.moveFile(backupFile, originalFile);			
		}
			
	}

}
