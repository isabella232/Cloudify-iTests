package test.cli.cloudify.cloud;

import java.io.IOException;

import org.apache.commons.lang.StringUtils;
import org.testng.Assert;

import test.cli.cloudify.CommandTestUtils;
import test.cli.cloudify.cloud.services.CloudService;
import test.cli.cloudify.cloud.services.CloudServiceManager;
import test.cli.cloudify.security.SecurityConstants;
import framework.tools.SGTestHelper;
import framework.utils.ApplicationInstaller;
import framework.utils.CloudBootstrapper;
import framework.utils.ServiceInstaller;

public abstract class NewAbstractSecurityCloudTest extends NewAbstractCloudTest{

	private static final String BUILD_SECURITY_FILE_PATH = SGTestHelper.getBuildDir().replace('\\', '/') + "/config/security/spring-security.xml";
	private static final String DEFAULT_SECURITY_FILE_PATH = SGTestHelper.getSGTestRootDir().replace('\\', '/') + "/src/main/config/security/spring-security.xml";
	private static final String DEFAULT_KEYSTORE_FILE_PATH = SGTestHelper.getSGTestRootDir().replace('\\', '/') + "/src/main/config/security/keystore";
	private static final String DEFAULT_KEYSTORE_PASSWORD = "sgtest";
	
	@Override
	protected void bootstrap() throws Exception {
		
		CloudService service = CloudServiceManager.getInstance().getCloudService(getCloudName());
		CloudBootstrapper securedBootstrapper = new CloudBootstrapper();
		securedBootstrapper.secured(true).securityFilePath(DEFAULT_SECURITY_FILE_PATH)
			.user(SecurityConstants.ALL_ROLES_USER_PWD).password(SecurityConstants.ALL_ROLES_USER_PWD);
		securedBootstrapper.keystoreFilePath(DEFAULT_KEYSTORE_FILE_PATH).keystorePassword(DEFAULT_KEYSTORE_PASSWORD);
		service.setBootstrapper(securedBootstrapper);
		
		super.bootstrap(service);
	}
	
	protected void bootstrap(CloudService service, CloudBootstrapper securedBootstrapper) throws Exception {
		
		service.setBootstrapper(securedBootstrapper);		
		super.bootstrap(service);
	}
	
	@Override
	protected void teardown() throws Exception {
		
		super.teardown();
	}
	
	protected String installApplicationAndWait(String applicationPath, String applicationName, int timeout, final String cloudifyUsername,
			final String cloudifyPassword, boolean isExpectedToFail, final String authGroups) throws IOException, InterruptedException {

		ApplicationInstaller applicationInstaller = new ApplicationInstaller(getRestUrl(), applicationName);
		applicationInstaller.recipePath(applicationPath);
		applicationInstaller.waitForFinish(true);
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

	protected String login(String user, String password, boolean failCommand){

		String output = "no output";

		try {
			output = CommandTestUtils.runCommand(connectCommand(user, password) + ";" + loginCommand(user, password), true, failCommand);
		} catch (IOException e) {
			Assert.fail("Failed to connect and login");
		} catch (InterruptedException e) {
			Assert.fail("Failed to connect and login");
		}

		return output;
	}
	
	protected String connect(String user, String password, boolean isExpectedToFail){	

		String output = "no output";
		try {
			
			if (isExpectedToFail) {
				output = CommandTestUtils.runCommandExpectedFail(connectCommand(user, password));
			} else {
				output = CommandTestUtils.runCommandAndWait(connectCommand(user, password));
			}	
			
		} catch (IOException e) {
			Assert.fail("Failed to connect");
		} catch (InterruptedException e) {
			Assert.fail("Failed to connect");
		}

		return output;
	}

	protected String listApplications(String user, String password){
		try {
			return CommandTestUtils.runCommandAndWait(connectCommand(user, password) + ";list-applications");
		} catch (IOException e) {
			Assert.fail("Failed to list applications", e);
		} catch (InterruptedException e) {
			Assert.fail("Failed to list applications", e);
		}

		return null;
	}
	
	protected String listServices(String user, String password, String applicationName){
		try {
			return CommandTestUtils.runCommandAndWait(connectCommand(user, password) + ";use-application " + applicationName + ";list-services");
		} catch (IOException e) {
			Assert.fail("Failed to list applications", e);
		} catch (InterruptedException e) {
			Assert.fail("Failed to list applications", e);
		}
		
		return null;
	}
	
	protected String listInstances(String user, String password, String applicationName, String serviceName){
		try {
			return CommandTestUtils.runCommandAndWait(connectCommand(user, password) + ";use-application " + applicationName + ";list-instances " + serviceName);
		} catch (IOException e) {
			Assert.fail("Failed to list applications", e);
		} catch (InterruptedException e) {
			Assert.fail("Failed to list applications", e);
		}
		
		return null;
	}
	
	protected String loginCommand(String user, String password){		
		return ("login " + user + " " + password);
	}

	protected String connectCommand(String user, String password){
		
		StringBuilder builder = new StringBuilder();
		
		builder.append("connect ");
		
		if(user != null){
			builder.append("-user " + user + " ");
		}
		
		if(password != null){
			builder.append("-password " + password + " ");
		}
		
		builder.append(getRestUrl());
		
		return builder.toString();
	}
	
	public static String getDefaultSecurityFilePath() {
		return DEFAULT_SECURITY_FILE_PATH;
	}

	public static String getDefaultKeystoreFilePath() {
		return DEFAULT_KEYSTORE_FILE_PATH;
	}

	public static String getDefaultKeystorePassword() {
		return DEFAULT_KEYSTORE_PASSWORD;
	}
	
	public static String getBuildSecurityFilePath() {
		return BUILD_SECURITY_FILE_PATH;
	}
	
	
}

