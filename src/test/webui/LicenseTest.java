package test.webui;

import java.net.UnknownHostException;

import org.testng.annotations.Test;

import test.cli.cloudify.AbstractLocalCloudTest;

import com.gigaspaces.webuitf.LoginPage;
import com.gigaspaces.webuitf.dashboard.DashboardTab;

import framework.utils.AssertUtils;

public class LicenseTest extends AbstractLocalCloudTest{
	
	private WebuiTestUtils webuiHelper;
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT)
	public void testCloudifyLicense() throws InterruptedException, UnknownHostException {
		
		webuiHelper = new WebuiTestUtils();
		
		LoginPage loginPage = webuiHelper.getLoginPage();
		
		DashboardTab dashboard = loginPage.login().switchToDashboard();
		
		AssertUtils.assertTrue(!dashboard.isXap());
		
		webuiHelper.close();
	}

}
