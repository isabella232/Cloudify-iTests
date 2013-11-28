package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.ec2.bigdata;

import iTests.framework.tools.SGTestHelper;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.NewAbstractCloudTest;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class StockDemoApplicationTest extends NewAbstractCloudTest {

	private static final int APPLICATION_INSTALL_TIMEOUT_IN_MINUTES = 120;
	
	@BeforeClass(alwaysRun = true)
	protected void bootstrap() throws Exception {
		super.bootstrap();
	}

	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 8, groups = "1", enabled = true)
	public void testStockDemo() throws IOException, InterruptedException {
		doSanityTest("stockdemo", "stockdemo", APPLICATION_INSTALL_TIMEOUT_IN_MINUTES);
	}
	
	@Override
	protected void beforeBootstrap() throws Exception {
		String suiteName = System.getProperty("iTests.suiteName");
		if(suiteName != null && "CLOUDIFY_XAP".equalsIgnoreCase(suiteName)){
			/* copy premium license to cloudify-overrides in order to run xap pu's */
			String overridesFolder = getService().getPathToCloudFolder() + "/upload/cloudify-overrides";
			File cloudifyPremiumLicenseFile = new File(SGTestHelper.getSGTestRootDir() + "/config/gslicense.xml");
			FileUtils.copyFileToDirectory(cloudifyPremiumLicenseFile, new File(overridesFolder));
		}
	}
	
	@AfterClass(alwaysRun = true)
	protected void teardown() throws Exception {
		super.teardown();
	}
	
	@Override
	public void beforeTeardown() throws IOException, InterruptedException {
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
}
