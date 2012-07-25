package test.webui;

import org.testng.annotations.Test;

import com.gigaspaces.webuitf.LoginPage;
import com.gigaspaces.webuitf.dashboard.DashboardTab;

public class LicenseTest extends AbstractWebUILocalCloudTest {
	

	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT)
	public void testCloudifyLicense() throws InterruptedException {
		
		LoginPage loginPage = getLoginPage();
		
		DashboardTab dashboard = loginPage.login().switchToDashboard();
		
		assertTrue(!dashboard.isXap());
	}

}
