package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.byon;

import org.cloudifysource.quality.iTests.framework.utils.CloudBootstrapper;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.services.CloudService;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.services.CloudServiceManager;
import org.cloudifysource.quality.iTests.test.cli.cloudify.security.SecurityConstants;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * test port and memory allocation with security.
 * 
 * @author adaml
 *
 */
public class PortAndMemoryAllocationWithSecurityTest extends PortAndMemoryAllocationTest {

	
	@BeforeClass(alwaysRun = true)
	protected void bootstrap() throws Exception {
		CloudService service = CloudServiceManager.getInstance().getCloudService(getCloudName());
		CloudBootstrapper securedBootstrapper = new CloudBootstrapper();
		securedBootstrapper.secured(true).securityFilePath(SecurityConstants.BUILD_SECURITY_FILE_PATH)
		.user(SecurityConstants.USER_PWD_ALL_ROLES).password(SecurityConstants.USER_PWD_ALL_ROLES);
		securedBootstrapper.keystoreFilePath(SecurityConstants.DEFAULT_KEYSTORE_FILE_PATH).keystorePassword(SecurityConstants.DEFAULT_KEYSTORE_PASSWORD);
		service.setBootstrapper(securedBootstrapper);
		
		super.bootstrap(service);
		installServiceAndWait(SIMPLE_RECIPE_FOLDER, SERVICE_NAME, 5, "Superuser", "Superuser", false,  "");
		
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, enabled = true)
	public void testPortAndMemoryAllocation() {
		super.testPortAndMemoryAllocation();
	}
	
	@AfterClass(alwaysRun = true)
	protected void teardown() throws Exception {
		super.teardown();
	}
	
}
