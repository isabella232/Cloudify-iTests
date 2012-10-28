package test.cli.cloudify.cloud.ec2.bigdata;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.testng.ITestContext;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.cli.cloudify.cloud.NewAbstractCloudTest;
import framework.tools.SGTestHelper;

public class StockDemoApplicationTest extends NewAbstractCloudTest {

	private static final int APPLICATION_INSTALL_TIMEOUT_IN_MINUTES = 60;
	
	@BeforeClass(alwaysRun = true)
	protected void bootstrap(final ITestContext testContext) {
		super.bootstrap(testContext);
	}
	
	@BeforeMethod
	public void prepareApplication() throws IOException {
		File stockDemoAppSG = new File(SGTestHelper.getSGTestRootDir() + "/apps/cloudify/recipes/stockdemo");
		File appsFolder = new File(SGTestHelper.getBuildDir() + "/recipes/apps/stockdemo");
		FileUtils.copyDirectory(stockDemoAppSG, appsFolder);
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 4, groups = "1", enabled = true)
	public void testStockDemo() throws IOException, InterruptedException {
		doSanityTest("stockdemo", "stockdemo", APPLICATION_INSTALL_TIMEOUT_IN_MINUTES);
	}
	
	@AfterClass(alwaysRun = true)
	protected void teardown() {
		super.teardown();
	}
	
	@Override
	public void beforeTeardown() {
		super.uninstallApplicationIfFound("stockdemo");
	}
	

	@Override
	protected String getCloudName() {
		return "ec2";
	}

	@Override
	protected boolean isReusableCloud() {
		return false;
	}

	@Override
	protected void customizeCloud() throws Exception {
		// TODO Auto-generated method stub
		
	}
}
