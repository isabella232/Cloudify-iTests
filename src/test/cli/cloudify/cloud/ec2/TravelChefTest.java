package test.cli.cloudify.cloud.ec2;

import java.io.IOException;

import org.testng.ITestContext;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import test.cli.cloudify.cloud.NewAbstractCloudTest;

public class TravelChefTest extends NewAbstractCloudTest{

	private String appName = "travel-chef";
	
	@BeforeClass(alwaysRun = true)
	protected void bootstrap(final ITestContext testContext) {
		super.bootstrap(testContext);
	}
	
	@AfterClass(alwaysRun = true)
	protected void teardown() {
		super.teardown();
	}
	
	@AfterMethod(alwaysRun = true)
	public void cleanUp() throws IOException, InterruptedException {
		super.uninstallApplicationAndWait(appName);
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 4, enabled = true)
	public void testStockDemo() throws IOException, InterruptedException {
		doSanityTest(appName, appName);
	}
	
	@Override
	protected void customizeCloud() throws Exception {
		// TODO Auto-generated method stub		
	}

	@Override
	protected String getCloudName() {
		return "ec2";
	}

	@Override
	protected boolean isReusableCloud() {
		return false;
	}

}
