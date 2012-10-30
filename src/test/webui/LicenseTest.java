package test.webui;

import java.net.UnknownHostException;

import org.testng.annotations.Test;

import test.cli.cloudify.AbstractLocalCloudTest;

import com.gigaspaces.webuitf.LoginPage;
import com.gigaspaces.webuitf.dashboard.DashboardTab;

import framework.utils.AssertUtils;

public class LicenseTest extends AbstractLocalCloudTest{
	
	private WebuiEnabler webuiEnabler;
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT)
	public void testCloudifyLicense() throws InterruptedException, UnknownHostException {
		
		webuiEnabler = new WebuiEnabler();
		
		LoginPage loginPage = webuiEnabler.getLoginPage();
		
		DashboardTab dashboard = loginPage.login().switchToDashboard();
		
		AssertUtils.assertTrue(!dashboard.isXap());
		
		webuiEnabler.close();
	}

}
