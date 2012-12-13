package test.cli.cloudify;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.openspaces.admin.AdminFactory;

import test.AbstractTest;
import test.cli.cloudify.CommandTestUtils.ProcessResult;
import test.cli.cloudify.security.SecurityConstants;
import framework.utils.ApplicationInstaller;
import framework.utils.LocalCloudBootstrapper;
import framework.utils.LogUtils;
import framework.utils.ServiceInstaller;

public class AbstractSecuredLocalCloudTest extends AbstractTest {

	protected static final String SIMPLE_APP_NAME = "simple";
	protected static final String SIMPLE_APP_PATH = CommandTestUtils.getPath("/src/main/resources/apps/USM/usm/applications/" + SIMPLE_APP_NAME);
	protected static final String SIMPLE_SERVICE_NAME = "simple";
	protected static final String SIMPLE_SERVICE_PATH = CommandTestUtils.getPath("/src/main/resources/apps/USM/usm/" + SIMPLE_SERVICE_NAME);

	protected static final String GROOVY_APP_NAME = "groovyApp";
	protected static final String GROOVY_APP_PATH = CommandTestUtils.getPath("/src/main/resources/apps/USM/usm/applications/" + GROOVY_APP_NAME);
	protected static final String GROOVY_SERVICE_NAME = "groovy";
	protected static final String GROOVY2_SERVICE_NAME = "groovy2";	
	protected static final int TIMEOUT_IN_MINUTES = 30;
	protected static final String ACCESS_DENIED_MESSAGE = "no_permission_access_is_denied";
	protected static final String INSTANCE_VERIFICATION_STRING = "instance #1";
	protected static final String BAD_CREDENTIALS_MESSAGE = "Bad credentials";

	protected void installAndUninstallTest() throws IOException,InterruptedException {
		installAndUninstall(SecurityConstants.APP_MANAGER_AND_VIEWER_USER_PWD, SecurityConstants.APP_MANAGER_AND_VIEWER_USER_PWD, false);
		installAndUninstall(SecurityConstants.APP_MANAGER_USER_PWD, SecurityConstants.APP_MANAGER_USER_PWD, false);
		installAndUninstall(SecurityConstants.CLOUD_ADMIN_AND_APP_MANAGER_USER_PWD, SecurityConstants.CLOUD_ADMIN_AND_APP_MANAGER_USER_PWD, false);
		installAndUninstall(SecurityConstants.CLOUD_ADMIN_USER_PWD, SecurityConstants.CLOUD_ADMIN_USER_PWD, false);
		installAndUninstall(SecurityConstants.VIEWER_USER_PWD, SecurityConstants.VIEWER_USER_PWD, true);
		installAndUninstall(SecurityConstants.NO_ROLE_USER_PWD, SecurityConstants.NO_ROLE_USER_PWD, true);
	}


	protected String getRestUrl() {
		return "https://127.0.0.1:8100";
	}

	protected void bootstrap() throws Exception {
		LocalCloudBootstrapper bootstrapper = new LocalCloudBootstrapper();
		bootstrapper.secured(true).securityFilePath(SecurityConstants.BUILD_SECURITY_FILE_PATH);
		bootstrapper.keystoreFilePath(SecurityConstants.DEFAULT_KEYSTORE_FILE_PATH).keystorePassword(SecurityConstants.DEFAULT_KEYSTORE_PASSWORD);
		bootstrap(bootstrapper);
	}

	protected ProcessResult bootstrap(LocalCloudBootstrapper bootstrapper) throws Exception {
		ProcessResult bootstrapResult = bootstrapper.bootstrap();
		if (bootstrapper.isBootstraped()) {
			// only create admin if bootstrap was successful
			admin = super.createAdminAndWaitForManagement();
		}
		return bootstrapResult;
	}

	protected void testLogin() throws IOException, InterruptedException {
		String output = "no output";

		output = login(SecurityConstants.VIEWER_USER_PWD, SecurityConstants.VIEWER_USER_PWD, false);		
		assertTrue("login failed for: " + SecurityConstants.VIEWER_DESCRIPTIN, output.contains("Logged in successfully"));
	}
	protected void testConnectWithNonExistingUser() throws IOException,
	InterruptedException {
		String output = connect(SecurityConstants.CLOUD_ADMIN_USER_PWD + "bad", SecurityConstants.CLOUD_ADMIN_USER_PWD, true);		
		assertTrue("connect succeeded for user: " + SecurityConstants.CLOUD_ADMIN_USER_PWD + "bad", output.contains(BAD_CREDENTIALS_MESSAGE));
	}

	protected void teardown(LocalCloudBootstrapper bootstrapper) throws IOException, InterruptedException {
		if (admin != null) {
			admin.close();
		}
		bootstrapper.setRestUrl(getRestUrl());
		bootstrapper.teardown();
	}

	protected void teardown() throws IOException, InterruptedException {
		LocalCloudBootstrapper bootstrapper = new LocalCloudBootstrapper();
		bootstrapper.user(SecurityConstants.ALL_ROLES_USER_PWD).password(SecurityConstants.ALL_ROLES_USER_PWD);
		teardown(bootstrapper);
	}

	@Override
	protected AdminFactory createAdminFactory() {
		AdminFactory factory = new AdminFactory();
		factory.addLocator("127.0.0.1:" + CloudifyConstants.DEFAULT_LOCALCLOUD_LUS_PORT);
		return factory;
	}

	protected void installWithoutCredentialsTest() throws IOException,
	InterruptedException {
		String output = installApplicationAndWait(SIMPLE_APP_PATH, SIMPLE_APP_NAME, TIMEOUT_IN_MINUTES, null, null, true, null);
		assertTrue("install access granted to an Anonymous user" , output.contains(BAD_CREDENTIALS_MESSAGE));
	}

	protected void verifyVisibleLists(String installer, String viewerName, String viewerPassword, String viewerDescription, String appName, boolean isVisible) throws IOException, InterruptedException {

		String output = "no output";


		if(isVisible){
			output = listApplications(viewerName, viewerPassword, false);
			assertTrue(viewerDescription + " doesn't see the application of " + installer, output.contains(appName));
		}
		else{
			output = listApplications(viewerName, viewerPassword, true);
			assertTrue(viewerDescription + " sees the application of " + installer, !output.contains(appName));
		}


		if(isVisible){
			if(appName.equalsIgnoreCase(SIMPLE_APP_NAME)){	

				output = listServices(viewerName, viewerPassword, SIMPLE_APP_NAME, false);
				assertTrue(viewerDescription + " doesn't see the services of " + installer, output.contains(SIMPLE_APP_NAME + "." + SIMPLE_SERVICE_NAME));
			}
			else{

				output = listServices(viewerName, viewerPassword, GROOVY_APP_NAME, true);
				assertTrue(viewerDescription + " doesn't see the services of " + installer, output.contains(GROOVY_APP_NAME + "." + GROOVY_SERVICE_NAME) && output.contains(GROOVY_APP_NAME + "." + GROOVY2_SERVICE_NAME));

			}

		}
		else{	
			if(appName.equalsIgnoreCase(SIMPLE_APP_NAME)){

				output = listServices(viewerName, viewerPassword, SIMPLE_APP_NAME, true);
				assertTrue(viewerDescription + " sees the services of " + installer, !output.contains(SIMPLE_APP_NAME + "." + SIMPLE_SERVICE_NAME));			
			}
			else{

				output = listServices(viewerName, viewerPassword, GROOVY_APP_NAME, true);
				assertTrue(viewerDescription + " sees the services of " + installer, !(output.contains(GROOVY_APP_NAME + "." + GROOVY_SERVICE_NAME) || output.contains(GROOVY_APP_NAME + "." + GROOVY2_SERVICE_NAME)));							
			}
		}


		if(appName.equalsIgnoreCase(SIMPLE_APP_NAME)){


			if(isVisible){	
				output = listInstances(viewerName, viewerPassword, SIMPLE_APP_NAME, SIMPLE_SERVICE_NAME, false);
				assertTrue(viewerDescription + " doesn't see the instances of " + installer, output.contains(INSTANCE_VERIFICATION_STRING));
			}
			else{
				output = listInstances(viewerName, viewerPassword, SIMPLE_APP_NAME, SIMPLE_SERVICE_NAME, true);
				assertTrue(viewerDescription + " sees the instances of " + installer, !output.contains(INSTANCE_VERIFICATION_STRING));

			}

		}
		else{	


			if(isVisible){	
				output = listInstances(viewerName, viewerPassword, GROOVY_APP_NAME, GROOVY_SERVICE_NAME, false);
				assertTrue(viewerDescription + " doesn't see the instances of " + installer, output.contains(INSTANCE_VERIFICATION_STRING));			
			}
			else{
				output = listInstances(viewerName, viewerPassword, GROOVY_APP_NAME, GROOVY_SERVICE_NAME, true);
				assertTrue(viewerDescription + " sees the instances of " + installer, !output.contains(INSTANCE_VERIFICATION_STRING));							
			}
		}
	}

	public void installAndUninstall(String user, String password, boolean isInstallExpectedToFail) throws IOException, InterruptedException{

		String output = installApplicationAndWait(SIMPLE_APP_PATH, SIMPLE_APP_NAME, TIMEOUT_IN_MINUTES, user, password, isInstallExpectedToFail, null);

		if(isInstallExpectedToFail){
			assertTrue("application installation access granted to " + user, output.contains(ACCESS_DENIED_MESSAGE));
		}

		if(output.contains("Application " + SIMPLE_APP_NAME + " installed successfully")){			
			uninstallApplicationAndWait(SIMPLE_APP_PATH, SIMPLE_APP_NAME, TIMEOUT_IN_MINUTES, user, password, isInstallExpectedToFail, null);
		}

		output = installServiceAndWait(SIMPLE_SERVICE_PATH, SIMPLE_SERVICE_NAME, TIMEOUT_IN_MINUTES, user, password, isInstallExpectedToFail, null);

		if(isInstallExpectedToFail){
			assertTrue("service installation access granted to " + user, output.contains(ACCESS_DENIED_MESSAGE));
		}

		if(output.contains("Service \"" + SIMPLE_SERVICE_NAME + "\" successfully installed")){			
			uninstallServiceAndWait(SIMPLE_APP_PATH, SIMPLE_APP_NAME, TIMEOUT_IN_MINUTES, user, password, isInstallExpectedToFail, null);
		}

	}



	protected String installApplicationAndWait(String applicationPath, String applicationName, int timeout, final String cloudifyUsername,
			final String cloudifyPassword, boolean isExpectedToFail, final String authGroups) throws IOException, InterruptedException {

		ApplicationInstaller applicationInstaller = new ApplicationInstaller(getRestUrl(), applicationName);
		applicationInstaller.recipePath(applicationPath);
		applicationInstaller.waitForFinish(true);
		applicationInstaller.timeoutInMinutes(timeout);
		applicationInstaller.cloudifyUsername(cloudifyUsername);
		applicationInstaller.cloudifyPassword(cloudifyPassword);
		applicationInstaller.expectToFail(isExpectedToFail);
		if (StringUtils.isNotBlank(authGroups)) {
			applicationInstaller.authGroups(authGroups);
		}

		return applicationInstaller.install();
	}

	protected String uninstallApplicationAndWait(String applicationPath, String applicationName, int timeout, final String cloudifyUsername,
			final String cloudifyPassword, boolean isExpectedToFail, final String authGroups) throws IOException, InterruptedException {

		ApplicationInstaller applicationInstaller = new ApplicationInstaller(getRestUrl(), applicationName);
		applicationInstaller.recipePath(applicationPath);
		applicationInstaller.waitForFinish(true);
		applicationInstaller.timeoutInMinutes(timeout);
		applicationInstaller.cloudifyUsername(cloudifyUsername);
		applicationInstaller.cloudifyPassword(cloudifyPassword);
		applicationInstaller.expectToFail(isExpectedToFail);
		if (StringUtils.isNotBlank(authGroups)) {
			applicationInstaller.authGroups(authGroups);
		}

		return applicationInstaller.uninstall();
	}

	protected void uninstallApplicationIfFound(String applicationName, final String cloudifyUsername, final String cloudifyPassword) throws IOException, InterruptedException {
		ApplicationInstaller applicationInstaller = new ApplicationInstaller(getRestUrl(), applicationName);
		applicationInstaller.waitForFinish(true);
		applicationInstaller.cloudifyUsername(cloudifyUsername);
		applicationInstaller.cloudifyPassword(cloudifyPassword);
		applicationInstaller.uninstallIfFound();
	}

	protected String installServiceAndWait(String servicePath, String serviceName, int timeout, final String cloudifyUsername,
			final String cloudifyPassword, boolean isExpectedToFail, final String authGroups) throws IOException, InterruptedException {

		ServiceInstaller serviceInstaller = new ServiceInstaller(getRestUrl(), serviceName);
		serviceInstaller.recipePath(servicePath);
		serviceInstaller.waitForFinish(true);
		serviceInstaller.cloudifyUsername(cloudifyUsername);
		serviceInstaller.cloudifyPassword(cloudifyPassword);
		serviceInstaller.expectToFail(isExpectedToFail);
		if (StringUtils.isNotBlank(authGroups)) {
			serviceInstaller.authGroups(authGroups);
		}

		return serviceInstaller.install();
	}

	protected String uninstallServiceAndWait(String servicePath, String serviceName, int timeout, final String cloudifyUsername,
			final String cloudifyPassword, boolean isExpectedToFail, final String authGroups) throws IOException, InterruptedException {

		ServiceInstaller serviceInstaller = new ServiceInstaller(getRestUrl(), serviceName);
		serviceInstaller.recipePath(servicePath);
		serviceInstaller.waitForFinish(true);
		serviceInstaller.cloudifyUsername(cloudifyUsername);
		serviceInstaller.cloudifyPassword(cloudifyPassword);
		serviceInstaller.expectToFail(isExpectedToFail);
		if (StringUtils.isNotBlank(authGroups)) {
			serviceInstaller.authGroups(authGroups);
		}

		return serviceInstaller.uninstall();
	}

	protected String listApplications(String user, String password, boolean expectedFail) throws IOException, InterruptedException {
		LocalCloudBootstrapper bootstrapper = new LocalCloudBootstrapper();
		bootstrapper.setRestUrl(getRestUrl());
		bootstrapper.user(user).password(password);
		return bootstrapper.listApplications(expectedFail);
	}

	protected String listInstances(String user, String password, String applicationName, String serviceName, boolean expectedFail) throws IOException, InterruptedException{
		LocalCloudBootstrapper bootstrapper = new LocalCloudBootstrapper();
		bootstrapper.setRestUrl(getRestUrl());
		bootstrapper.user(user).password(password);
		return bootstrapper.listInstances(applicationName, serviceName, expectedFail);
	}

	protected String listServices(String user, String password, String applicationName, boolean expectedFail) throws IOException, InterruptedException {
		LocalCloudBootstrapper bootstrapper = new LocalCloudBootstrapper();
		bootstrapper.setRestUrl(getRestUrl());
		bootstrapper.user(user).password(password);
		return bootstrapper.listServices(applicationName, expectedFail);
	}

	protected String connect(String user, String password, boolean failCommand) throws IOException, InterruptedException{
		LocalCloudBootstrapper bootstrapper = new LocalCloudBootstrapper();
		bootstrapper.setRestUrl(getRestUrl());
		bootstrapper.user(user).password(password);
		return bootstrapper.connect(failCommand);
	}

	protected String login(String user, String password, boolean failCommand) throws IOException, InterruptedException{
		LocalCloudBootstrapper bootstrapper = new LocalCloudBootstrapper();
		bootstrapper.setRestUrl(getRestUrl());
		bootstrapper.user(user).password(password);
		return bootstrapper.login(failCommand);		
	}

	protected void testConnectWithNoPassword() throws IOException,
	InterruptedException {
		String output = connect(SecurityConstants.CLOUD_ADMIN_USER_PWD, null, true);		
		assertTrue("connect succeeded for: " + SecurityConstants.CLOUD_ADMIN_DESCRIPTIN + " without providing a password", output.contains(BAD_CREDENTIALS_MESSAGE));
	}

	protected void testLoginWithNonexistingUser() throws IOException,
	InterruptedException {
		String output = "no output";

		output = login(SecurityConstants.CLOUD_ADMIN_USER_PWD + "bad", SecurityConstants.CLOUD_ADMIN_USER_PWD, true);					

		assertTrue("login succeeded for user: " + SecurityConstants.CLOUD_ADMIN_USER_PWD + "bad", output.contains(BAD_CREDENTIALS_MESSAGE));
	}

	protected void testConnectWithWrongPassword() throws IOException,
	InterruptedException {
		String output = connect(SecurityConstants.CLOUD_ADMIN_USER_PWD, SecurityConstants.CLOUD_ADMIN_USER_PWD + "bad", true);		
		assertTrue("connect succeeded for password: " + SecurityConstants.CLOUD_ADMIN_USER_PWD + "bad", output.contains(BAD_CREDENTIALS_MESSAGE));
	}

	protected void testLoginWithWrongPassword() throws IOException,
	InterruptedException {
		String output = "no output";

		output = login(SecurityConstants.CLOUD_ADMIN_USER_PWD, SecurityConstants.CLOUD_ADMIN_USER_PWD + "bad", true);

		assertTrue("login succeeded for password: " + SecurityConstants.CLOUD_ADMIN_USER_PWD + "bad", output.contains(BAD_CREDENTIALS_MESSAGE));
	}

	protected void testSecuredUseApplication() throws IOException,
	InterruptedException {
		installApplicationAndWait(SIMPLE_APP_PATH, SIMPLE_APP_NAME, TIMEOUT_IN_MINUTES, SecurityConstants.ALL_ROLES_USER_PWD, SecurityConstants.ALL_ROLES_USER_PWD, false, null);
		//Check use-application command.
		String useApplicationOutput;
		//appManager role has permissions to use the application
		useApplicationOutput = useApplication(SecurityConstants.APP_MANAGER_USER_PWD, SecurityConstants.APP_MANAGER_USER_PWD, SIMPLE_APP_NAME, false);
		assertTrue("Failed to change application context. Use-application command failed. output was: " + useApplicationOutput, 
				useApplicationOutput.contains("Using application simple"));

		//appManager role has permissions to create a new application context
		useApplicationOutput = useApplication(SecurityConstants.APP_MANAGER_USER_PWD, SecurityConstants.APP_MANAGER_USER_PWD, "SomeApp", false);
		assertTrue("Failed to change application context. Use-application command failed. output was: " + useApplicationOutput, 
				useApplicationOutput.contains("Using application SomeApp"));

		//user has viewing permission to view the existing installed app
		useApplicationOutput = useApplication(SecurityConstants.VIEWER_USER_PWD, SecurityConstants.VIEWER_USER_PWD, SIMPLE_APP_NAME, false);
		assertTrue("Failed to change application context. Use-application command failed. output was: " + useApplicationOutput, 
				useApplicationOutput.contains("Using application simple"));

		//user has permission to view the app but does not have permission to create a new one.
		useApplicationOutput = useApplication(SecurityConstants.VIEWER_USER_PWD, SecurityConstants.VIEWER_USER_PWD, "SomeApp", true);
		assertTrue("Failed to change application context. Use-application command failed. output was: " + useApplicationOutput, 
				useApplicationOutput.contains("Permission not granted, access is denied."));

		//user does not have permission to view the app.
		useApplicationOutput = useApplication(SecurityConstants.NO_ROLE_USER_PWD, SecurityConstants.NO_ROLE_USER_PWD, SIMPLE_APP_NAME, true);
		assertTrue("Failed to change application context. Use-application command failed. output was: " + useApplicationOutput, 
				useApplicationOutput.contains("Permission not granted, access is denied."));
	}

	protected String useApplication(String user, String password,
			String applicationName, boolean expectedFail) throws IOException, InterruptedException {
		String useApplicationOutput;
		String command = "connect -user " + user + " -password " + password + " " + getRestUrl() + "; use-application " + applicationName;
		if (expectedFail) {
			useApplicationOutput = CommandTestUtils.runCommandExpectedFail(command);
		} else {
			useApplicationOutput = CommandTestUtils.runCommandAndWait(command);
		}
		return useApplicationOutput;
	}

	protected void testTamperWithSecurityFile() throws IOException,
	InterruptedException {
		String fakeCloudAdminUserAndPassword = "John";

		String originalFilePath = SecurityConstants.BUILD_SECURITY_FILE_PATH;
		String backupFilePath = originalFilePath + ".tempBackup";
		String fakeFilePath = CommandTestUtils.getPath("src/main/config/security/custom-spring-security.xml");
		File originalFile = new File(originalFilePath);
		File backupFile = new File(backupFilePath);
		File fakeFile = new File(fakeFilePath);
		String output = "no output";

		LogUtils.log("moving " + originalFilePath + " to " + backupFilePath);
		FileUtils.moveFile(originalFile, backupFile);

		try {
			LogUtils.log("copying " + fakeFilePath + " to " + originalFilePath);
			FileUtils.copyFile(fakeFile, originalFile);

			output = installApplicationAndWait(SIMPLE_APP_PATH, SIMPLE_APP_NAME, TIMEOUT_IN_MINUTES, fakeCloudAdminUserAndPassword, fakeCloudAdminUserAndPassword, true, null);

		} 
		finally {			
			LogUtils.log("deleting " + originalFilePath);
			try{
				FileUtils.deleteQuietly(originalFile);
			}
			catch(Exception e) {
				LogUtils.log("deletion of " + originalFilePath + " failed", e);
			}

			LogUtils.log("moving " + backupFilePath + " to " + originalFilePath);
			try{
				FileUtils.moveFile(backupFile, originalFile);
			}
			catch(Exception e) {
				LogUtils.log("moving of " + backupFilePath + " failed", e);
			}
		}

		assertTrue("install access granted to viewer " + fakeCloudAdminUserAndPassword, output.contains(ACCESS_DENIED_MESSAGE));
	}


}
