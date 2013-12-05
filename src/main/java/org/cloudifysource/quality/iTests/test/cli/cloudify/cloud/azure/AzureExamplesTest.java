package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.azure;

import iTests.framework.tools.SGTestHelper;
import iTests.framework.utils.AssertUtils;
import iTests.framework.utils.LogUtils;
import iTests.framework.utils.ScriptUtils;
import org.apache.commons.io.FileUtils;
import org.cloudifysource.quality.iTests.framework.utils.ApplicationInstaller;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.NewAbstractCloudTest;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.services.azure.MicrosoftAzureCloudService;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;

public class AzureExamplesTest extends NewAbstractCloudTest {
	
	@BeforeClass(alwaysRun = true)
	protected void bootstrap() throws Exception {
		super.bootstrap();
	}
	
	@BeforeMethod
	public void copyAzureApplicationsToBuildDir() throws IOException {
		copyApplicationToBuildDir("travel-azure");
		copyApplicationToBuildDir("petclinic-simple-azure");
		copyApplicationToBuildDir("helloworld-azure");
		
	}

	private void copyApplicationToBuildDir(final String applicationName) throws IOException {
		File appsDir = new File(SGTestHelper.getBuildDir() + "/recipes/apps");
		File originalAzureApplication = new File(SGTestHelper.getBuildDir() + "/recipes/apps/" + applicationName);
		FileUtils.deleteDirectory(originalAzureApplication);
		File newAzureApplication = new File(SGTestHelper.getSGTestRootDir() + "/src/main/resources/apps/cloudify/recipes/" + applicationName);
		FileUtils.copyDirectoryToDirectory(newAzureApplication, appsDir);
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 5, enabled = true)
	public void testTravel() throws Exception {
		LogUtils.log("installing application travel on azure");
		String applicationPath = ScriptUtils.getBuildPath() + "/recipes/apps/travel-azure";
		ApplicationInstaller applicationInstaller = new ApplicationInstaller(getRestUrl(), "travel");
		applicationInstaller.recipePath(applicationPath);
		applicationInstaller.waitForFinish(true);
		applicationInstaller.timeoutInMinutes(45);
		applicationInstaller.install();		
		applicationInstaller.uninstall();
		super.scanForLeakedAgentNodes();		
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 5, enabled = true)
	public void testPetclinicSimple() throws Exception {
		LogUtils.log("installing application petclinic-simple on azure");
		String applicationPath = ScriptUtils.getBuildPath() + "/recipes/apps/petclinic-simple-azure";
		ApplicationInstaller applicationInstaller = new ApplicationInstaller(getRestUrl(), "petclinic");
		applicationInstaller.recipePath(applicationPath);
		applicationInstaller.waitForFinish(true);
		applicationInstaller.timeoutInMinutes(45);
		applicationInstaller.install();		
		applicationInstaller.uninstall();
		super.scanForLeakedAgentNodes();		
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 5, enabled = true)
	public void testHelloWorld() throws Exception {
		LogUtils.log("installing application helloworld on azure");
		String applicationPath = ScriptUtils.getBuildPath() + "/recipes/apps/helloworld-azure";
		ApplicationInstaller applicationInstaller = new ApplicationInstaller(getRestUrl(), "helloworld");
		applicationInstaller.recipePath(applicationPath);
		applicationInstaller.waitForFinish(true);
		applicationInstaller.timeoutInMinutes(45);
		applicationInstaller.install();		
		applicationInstaller.uninstall();
		super.scanForLeakedAgentNodes();		
	}

	
	@AfterMethod(alwaysRun = true)
	public void cleanUp() throws IOException, InterruptedException {
		if (getService().getBootstrapper().isBootstraped()) {
			super.uninstallApplicationIfFound("travel");
			super.uninstallApplicationIfFound("petclinic");
			super.uninstallApplicationIfFound("helloworld");			
		}
		super.scanForLeakedAgentNodes();
	}

    @AfterClass(alwaysRun = true)
    protected void teardown() throws Exception {
        super.teardown();
        AssertUtils.assertFalse("Found leaking cloud services after teardown ended",
                ((MicrosoftAzureCloudService) getService()).scanForLeakingCloudServices());
    }

    @Override
	protected String getCloudName() {
		return "azure";
	}

	@Override
	protected boolean isReusableCloud() {
		return false;
	}
}
