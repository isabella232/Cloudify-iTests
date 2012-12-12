package test.cli.cloudify;

import java.io.IOException;

import org.apache.commons.lang.StringUtils;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.openspaces.admin.AdminFactory;

import test.AbstractTest;
import test.cli.cloudify.CommandTestUtils.ProcessResult;

import framework.tools.SGTestHelper;
import framework.utils.ApplicationInstaller;
import framework.utils.LocalCloudBootstrapper;
import framework.utils.ServiceInstaller;

public class AbstractSecuredLocalCloudTest extends AbstractTest {

	protected static final String SGTEST_ROOT_DIR = SGTestHelper.getSGTestRootDir().replace('\\', '/');

	protected static final String SIMPLE_APP_NAME = "simple";
	protected static final String SIMPLE_APP_PATH = SGTEST_ROOT_DIR + "/src/main/resources/apps/USM/usm/applications/" + SIMPLE_APP_NAME;
	protected static final String SIMPLE_SERVICE_NAME = "simple";
	protected static final String SIMPLE_SERVICE_PATH = SGTEST_ROOT_DIR + "/src/main/resources/apps/USM/usm/" + SIMPLE_SERVICE_NAME;
	
	protected static final String GROOVY_APP_NAME = "groovyApp";
	protected static final String GROOVY_APP_PATH = SGTEST_ROOT_DIR + "/src/main/resources/apps/USM/usm/applications/" + GROOVY_APP_NAME;
	protected static final String GROOVY_SERVICE_NAME = "groovy";
	protected static final String GROOVY2_SERVICE_NAME = "groovy2";	
	
	private static final String BUILD_SECURITY_FILE_PATH = SGTestHelper.getBuildDir().replace('\\', '/') + "/config/security/spring-security.xml";
	private static final String BUILD_SECURITY_BACKUP_FILE_PATH = SGTestHelper.getBuildDir().replace('\\', '/') + "/config/security/spring-security.xml.backup";
	private static final String DEFAULT_KEYSTORE_FILE_PATH = SGTestHelper.getSGTestRootDir().replace('\\', '/') + "/src/main/config/security/keystore";
	private static final String DEFAULT_KEYSTORE_PASSWORD = "sgtest";
	private static final String LDAP_SECURITY_FILE_PATH = SGTEST_ROOT_DIR + "/src/main/config/security/ldap-spring-security.xml";
	
	public static String getDefaultLdapSecurityFilePath() {
		return LDAP_SECURITY_FILE_PATH;
	}

	public static String getBuildSecurityFilePath() {
		return BUILD_SECURITY_FILE_PATH;
	}

	public static String getBuildSecurityBackupFilePath() {
		return BUILD_SECURITY_BACKUP_FILE_PATH;
	}

	public static String getDefaultKeystoreFilePath() {
		return DEFAULT_KEYSTORE_FILE_PATH;
	}

	public static String getDefaultKeystorePassword() {
		return DEFAULT_KEYSTORE_PASSWORD;
	}
	
	protected String getRestUrl() {
		return "https://127.0.0.1:8100";
	}
	
	protected void bootstrap() throws Exception {
		LocalCloudBootstrapper bootstrapper = new LocalCloudBootstrapper();
		bootstrapper.secured(true).securityFilePath(getBuildSecurityFilePath());
		bootstrapper.keystoreFilePath(getDefaultKeystoreFilePath()).keystorePassword(getDefaultKeystorePassword());
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
	
	protected void teardown(LocalCloudBootstrapper bootstrapper) throws IOException, InterruptedException {
		if (admin != null) {
			admin.close();
		}
		bootstrapper.setRestUrl(getRestUrl());
		bootstrapper.teardown();
	}
	
	protected void teardown() throws IOException, InterruptedException {
		LocalCloudBootstrapper bootstrapper = new LocalCloudBootstrapper();
		teardown(bootstrapper);
	}
	
	@Override
	protected AdminFactory createAdminFactory() {
		AdminFactory factory = new AdminFactory();
		factory.addLocator("127.0.0.1:" + CloudifyConstants.DEFAULT_LOCALCLOUD_LUS_PORT);
		return factory;
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
}
