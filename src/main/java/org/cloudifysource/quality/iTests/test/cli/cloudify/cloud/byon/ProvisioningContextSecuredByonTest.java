package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.byon;

import iTests.framework.utils.LogUtils;

import java.io.File;
import java.io.IOException;

import org.cloudifysource.esc.driver.provisioning.byon.ByonProvisioningDriver;
import org.cloudifysource.quality.iTests.framework.utils.CloudBootstrapper;
import org.cloudifysource.quality.iTests.framework.utils.ServiceInstaller;
import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
import org.cloudifysource.quality.iTests.test.cli.cloudify.util.CloudTestUtils;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.services.CloudService;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.services.CloudServiceManager;
import org.cloudifysource.quality.iTests.test.cli.cloudify.security.SecurityConstants;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * 
 * @author barakme
 * 
 */
public class ProvisioningContextSecuredByonTest extends AbstractByonCloudTest {

	@Override
	@BeforeClass(alwaysRun = true)
	protected void bootstrap() throws Exception {
		CloudBootstrapper bootstrapper = new CloudBootstrapper();
		bootstrapper.user(SecurityConstants.USER_PWD_ALL_ROLES).password(SecurityConstants.USER_PWD_ALL_ROLES).secured(true)
			.securityFilePath(SecurityConstants.BUILD_SECURITY_FILE_PATH)
			.keystoreFilePath(SecurityConstants.DEFAULT_KEYSTORE_FILE_PATH)
			.keystorePassword(SecurityConstants.DEFAULT_KEYSTORE_PASSWORD);
		
		CloudService service = CloudServiceManager.getInstance().getCloudService(getCloudName());
		service.setBootstrapper(bootstrapper);
		super.bootstrap(service);
		
		

		
	}

	@Override
	public void beforeBootstrap() throws IOException {

		final String newCloudDriverClazz = "CustomCloudDriver";

		getService().setNumberOfManagementMachines(1);
		CloudTestUtils.replaceGroovyDriverImplementation(
				getService(),
				ByonProvisioningDriver.class.getName(), // old class
				newCloudDriverClazz, // new class
				new File("src/main/resources/custom-cloud-configs/byon/provisioning-context")); // version
	}
	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 2, enabled = true)
	public void testInstallTomcatWitSecuredByonAndProvisiningContext() throws Exception {

		final ServiceInstaller installer = new ServiceInstaller(getRestUrl(), "tomcat");
		installer
				.cloudifyUsername(SecurityConstants.USER_PWD_APP_MANAGER)
				.cloudifyPassword(SecurityConstants.USER_PWD_APP_MANAGER)
				.recipePath("tomcat")
				.timeoutInMinutes(AbstractTestSupport.OPERATION_TIMEOUT);

		try {
			installer.install();
			LogUtils.log("Successfully installed service");
		} finally {

			installer.uninstall();
		}

	}

	@Override
	@AfterClass(alwaysRun = true)
	protected void teardown() throws Exception {
		super.teardown();
	}

}
