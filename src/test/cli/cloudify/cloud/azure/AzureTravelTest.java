package test.cli.cloudify.cloud.azure;

import java.io.IOException;

import org.testng.ITestContext;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import test.cli.cloudify.cloud.NewAbstractCloudTest;
import framework.tools.SGTestHelper;

public class AzureTravelTest extends NewAbstractCloudTest {

	@BeforeClass(alwaysRun = true)
	protected void bootstrap(final ITestContext testContext) {
		
//		MicrosoftAzureCloudService azureCloudService = new MicrosoftAzureCloudService(this.getClass().getName());
//				
//		String cloudServiceFullPath = SGTestHelper.getBuildDir() + "/tools/cli/plugins/esc/" + azureCloudService.getServiceFolder();
//		
//		File originalCloudDriverConfigFile = new File(cloudServiceFullPath, "azure-cloud.groovy");
//		File customCloudDriverConfigFile = new File(SGTestHelper.getSGTestRootDir() + "/apps/cloudify/cloud/azure", "azure-cloud.groovy");
//		
//		Map<File, File> filesToReplace = new HashMap<File, File>();
//		filesToReplace.put(originalCloudDriverConfigFile, customCloudDriverConfigFile);
//		
//		azureCloudService.addFilesToReplace(filesToReplace);
//		
//		super.bootstrap(testContext, azureCloudService);
	}
	
	@AfterClass(alwaysRun = true)
	protected void teardown() {
		super.teardown();
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 4)
	public void testTravel() throws IOException, InterruptedException {
		String travelAzurePath = SGTestHelper.getBuildDir() + "/apps/cloudify/recipes/travel-azure";
		doSanityTest(travelAzurePath, "travel");		
	}
	
	@Override
	protected void customizeCloud() throws Exception {
		// TODO Auto-generated method stub
		
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
