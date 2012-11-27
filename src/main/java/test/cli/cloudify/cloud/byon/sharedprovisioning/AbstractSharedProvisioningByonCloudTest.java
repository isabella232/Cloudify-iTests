package test.cli.cloudify.cloud.byon.sharedprovisioning;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import test.cli.cloudify.CommandTestUtils;
import test.cli.cloudify.cloud.byon.AbstractByonCloudTest;
import framework.utils.ApplicationInstaller;
import framework.utils.IOUtils;

public class AbstractSharedProvisioningByonCloudTest extends AbstractByonCloudTest {

	private final static String APP_SHARED_APPLICATION_SGTEST_PATH = CommandTestUtils.getPath("/src/main/resources/apps/USM/usm/applications/groovy-app-shared-provisioning");
	private final static String APP_SHARED_SERVICE_SGTEST_PATH = CommandTestUtils.getPath("/src/main/resources/apps/USM/usm/groovy-app-shared-provisioning");
	private final static String TENANT_SHARED_APPLICATION_SGTEST_PATH = CommandTestUtils.getPath("/src/main/resources/apps/USM/usm/applications/groovy-tenant-shared-provisioning");
	private final static String TENANT_SHARED_SERVICE_SGTEST_PATH = CommandTestUtils.getPath("/src/main/resources/apps/USM/usm/groovy-tenant-shared-provisioning");

	protected void installManualAppSharedProvisioningApplicationAndWait(final String applicationName) throws IOException, InterruptedException {

		try {
			// first copy our service file to build folder
			FileUtils.copyDirectoryToDirectory(new File(APP_SHARED_SERVICE_SGTEST_PATH), new File(CommandTestUtils.getBuildServicesPath()));

			// now copy the application
			FileUtils.copyDirectoryToDirectory(new File(APP_SHARED_APPLICATION_SGTEST_PATH), new File(CommandTestUtils.getBuildApplicationsPath()));

			// change the application name
			IOUtils.replaceTextInFile(CommandTestUtils.getBuildApplicationsPath() + "/groovy-app-shared-provisioning/groovyApp-application.groovy", "groovyApp", applicationName);

			// install
			ApplicationInstaller installer = new ApplicationInstaller("http://192.168.9.130:8100", applicationName);
			installer.setWaitForFinish(true);
			installer.setRecipePath("groovy-app-shared-provisioning");
			installer.install();
		} finally {
			File service = new File(CommandTestUtils.getBuildServicesPath(), "groovy-app-shared-provisioning");
			if (service.exists()) {
				FileUtils.deleteDirectory(service);
			}
			File application = new File(CommandTestUtils.getBuildApplicationsPath(), "groovy-app-shared-provisioning");
			if (application.exists()) {
				FileUtils.deleteDirectory(application);
			}
		}

	}

	protected void installManualTenantSharedProvisioningApplicationAndWait(final String authGroup , final String applicationName) throws IOException, InterruptedException {	

		try {
			// first copy our service file to build folder
			FileUtils.copyDirectoryToDirectory(new File(TENANT_SHARED_SERVICE_SGTEST_PATH), new File(CommandTestUtils.getBuildServicesPath()));

			// now copy the application
			FileUtils.copyDirectoryToDirectory(new File(TENANT_SHARED_APPLICATION_SGTEST_PATH), new File(CommandTestUtils.getBuildApplicationsPath()));

			// change the application name
			IOUtils.replaceTextInFile(CommandTestUtils.getBuildApplicationsPath() + "/groovy-tenant-shared-provisioning/groovyApp-application.groovy", "groovyApp", applicationName);

			// install

			ApplicationInstaller installer = new ApplicationInstaller(getRestUrl(), applicationName);
			installer.setCloudifyUsername("Dana");
			installer.setCloudifyPassword("Dana");
			installer.setWaitForFinish(true);
			installer.setRecipePath("groovy-tenant-shared-provisioning");
			installer.setAuthGroups(authGroup);

			installer.install();
		} finally {
			File service = new File(CommandTestUtils.getBuildServicesPath(), "groovy-tenant-shared-provisioning");
			if (service.exists()) {
				FileUtils.deleteDirectory(service);
			}
			File application = new File(CommandTestUtils.getBuildApplicationsPath(), "groovy-tenant-shared-provisioning");
			if (application.exists()) {
				FileUtils.deleteDirectory(application);
			}
		}

	}
}
