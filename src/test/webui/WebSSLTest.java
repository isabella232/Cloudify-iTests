package test.webui;

import static framework.utils.AdminUtils.loadGSCs;
import static framework.utils.AdminUtils.loadGSM;
import static framework.utils.LogUtils.log;

import org.openqa.selenium.WebDriver;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.DeploymentStatus;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitDeployment;
import org.testng.annotations.BeforeMethod;

import test.webui.objects.LoginPage;
import test.webui.objects.MainNavigation;
import test.webui.objects.services.PuTreeGrid.WebUIProcessingUnit;
import test.webui.objects.services.ServicesTab;

import com.thoughtworks.selenium.Selenium;

import framework.utils.DeploymentUtils;
import framework.utils.LogUtils;
import framework.utils.ProcessingUnitUtils;

public class WebSSLTest extends AbstractSeleniumTest {
	
	private Machine machineA;
	private final int port = 8080;
	private final int sslPort = 8443;
	
	
	@BeforeMethod(alwaysRun = true)
	public void startSetup() {
		log("waiting for 1 machine");
		admin.getMachines().waitFor(1);

		log("waiting for 1 GSA");
		admin.getGridServiceAgents().waitFor(1);

		GridServiceAgent[] agents = admin.getGridServiceAgents().getAgents();
		GridServiceAgent gsaA = agents[0];

		machineA = gsaA.getMachine();

		log("starting: 1 GSM and 2 GSC's on 1 machine");
		GridServiceManager gsmA = loadGSM(machineA); 
		loadGSCs(machineA, 2);
		
		LogUtils.log("deploying processing unit...");
		ProcessingUnit WebSSL = gsmA.deploy(new ProcessingUnitDeployment(DeploymentUtils.getArchive("webssl.war")));
		ProcessingUnitUtils.waitForDeploymentStatus(WebSSL, DeploymentStatus.INTACT);
		
	}
	
	//@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = {"cloudify" , "xap"})
	public void webSSLTest() throws InterruptedException {
		
		LoginPage loginPage = getLoginPage();
		
		MainNavigation mainNavigation = loginPage.login();
		
		ServicesTab servicesTab = mainNavigation.switchToServices();
		
		Selenium selenium = getSelenium();
		WebDriver driver = getDriver();
		
		String httpUrl = "http://" + machineA.getHostAddress() + ":" + port + "/web";
		String httpsUrl = "https://" + machineA.getHostAddress() + ":" + sslPort + "/web";
		
		driver.get(httpUrl);
		assertTrue(driver.getTitle().equals("index"));
		
		driver.get(httpsUrl);
		assertTrue(driver.getTitle().equals("index"));
		
		driver.get(baseUrl);
		
		mainNavigation.switchToServices();
		
		WebUIProcessingUnit webssl = servicesTab.getPuTreeGrid().getProcessingUnit("webssl");
		
		assertTrue(webssl != null);
		
		webssl.expand();
		
		assertTrue(selenium.isTextPresent(httpsUrl));
	}

}
