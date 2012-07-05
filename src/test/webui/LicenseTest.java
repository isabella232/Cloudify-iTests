package test.webui;

import org.openspaces.admin.pu.DeploymentStatus;
import org.openspaces.admin.pu.ProcessingUnit;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.gigaspaces.webuitf.LoginPage;
import com.gigaspaces.webuitf.dashboard.DashboardTab;

import framework.utils.ProcessingUnitUtils;

public class LicenseTest extends AbstractWebUITest {
	
	@BeforeMethod
	public void startBrowser() throws InterruptedException {	
		ProcessingUnit webui = admin.getProcessingUnits().waitFor("webui");
		ProcessingUnitUtils.waitForDeploymentStatus(webui, DeploymentStatus.INTACT);
		assertTrue(webui != null);
		assertTrue(webui.getInstances().length != 0);	
		String url = ProcessingUnitUtils.getWebProcessingUnitURL(webui).toString();	
		startWebBrowser(url); 
		
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT)
	public void testCloudifyLicense() throws InterruptedException {
		
		LoginPage loginPage = getLoginPage();
		
		DashboardTab dashboard = loginPage.login().switchToDashboard();
		
		assertTrue(!dashboard.isXap());
	}

}
