package test.cli.cloudify.cloud.azure;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import framework.tools.SGTestHelper;
import framework.utils.ApplicationInstaller;
import framework.utils.CloudBootstrapper;

import test.cli.cloudify.cloud.NewAbstractCloudTest;
import test.cli.cloudify.cloud.services.CloudService;
import test.cli.cloudify.cloud.services.CloudServiceManager;

/**
 * CLOUDIFY-1273
 * @author elip
 *
 */
public class TeardownWithoutUninstallAzureTest extends NewAbstractCloudTest {

	@Override
	protected String getCloudName() {
		return "azure";
	}

	@Override
	protected boolean isReusableCloud() {
		return false;
	}
	
	@BeforeClass(alwaysRun = true)
	protected void bootstrap() throws Exception {
		CloudBootstrapper bootstrapper = new CloudBootstrapper();
		bootstrapper.force(false);
		CloudService service = CloudServiceManager.getInstance().getCloudService(getCloudName());
		service.setBootstrapper(bootstrapper);
		super.bootstrap(service);
	}
	
	@BeforeMethod
	public void copyAzureApplicationsToBuildDir() throws IOException {
		copyApplicationToBuildDir("travel-azure");
	}
	
	private void copyApplicationToBuildDir(final String applicationName) throws IOException {
		File appsDir = new File(SGTestHelper.getBuildDir() + "/recipes/apps");
		File originalAzureApplication = new File(SGTestHelper.getBuildDir() + "/recipes/apps/" + applicationName);
		FileUtils.deleteDirectory(originalAzureApplication);
		File newAzureApplication = new File(SGTestHelper.getSGTestRootDir() + "/src/main/resources/apps/cloudify/recipes/" + applicationName);
		FileUtils.copyDirectoryToDirectory(newAzureApplication, appsDir);
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 5, enabled = false)
	public void testTeardownWithoutUninstall() throws Exception {
		
		ApplicationInstaller travelInstaller = new ApplicationInstaller(getRestUrl(), "travel");
		travelInstaller.recipePath("travel");
		
		travelInstaller.install();
		
		// this will fail if leaked nodes are found after the teardown.
		super.teardown();
	}

}
