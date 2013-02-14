package org.cloudifysource.quality.iTests.test.webui;

import java.net.UnknownHostException;

import org.cloudifysource.quality.iTests.framework.utils.AssertUtils;
import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
import org.cloudifysource.quality.iTests.test.cli.cloudify.AbstractLocalCloudTest;
import org.testng.annotations.Test;

import com.gigaspaces.webuitf.LoginPage;
import com.gigaspaces.webuitf.dashboard.DashboardTab;

public class LicenseTest extends AbstractLocalCloudTest {
	
	private WebuiTestUtils webuiHelper;
	
	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT)
	public void testCloudifyLicense() throws InterruptedException, UnknownHostException {
		
		webuiHelper = new WebuiTestUtils();
		
		LoginPage loginPage = webuiHelper.getLoginPage();
		
		DashboardTab dashboard = loginPage.login().switchToDashboard();
		
		AssertUtils.assertTrue(!dashboard.isXap());
		
		webuiHelper.close();
	}

}
