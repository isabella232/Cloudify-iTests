package test.webui.services.hosts;

import static framework.utils.AdminUtils.loadGSCs;
import static framework.utils.AdminUtils.loadGSM;
import static framework.utils.LogUtils.log;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.machine.Machine;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.webui.AbstractSeleniumTest;
import test.webui.objects.LoginPage;
import test.webui.objects.services.HostsAndServicesGrid;
import test.webui.objects.services.ServicesTab;

public class StartupTest extends AbstractSeleniumTest {
	
	private Machine machineA;
	
	@BeforeMethod(alwaysRun = true)
	public void startSetUp() {
		
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
	}
	
	 @Test(timeOut = DEFAULT_TEST_TIMEOUT)
	public void testGsAgentStartup() throws Exception {
		
		String hostname = machineA.getHostName();
		
		// get new login page
		LoginPage loginPage = getLoginPage();
		
		// get new topology tab
		ServicesTab topologyTab = loginPage.login().switchToServices();
		
		// get the needed grid
		HostsAndServicesGrid hostAndServices = topologyTab.getHostAndServicesGrid();
		
		// make components visible
		hostAndServices.clickOnHost(hostname);
		
		// assert all components are present
		assertTrue(hostAndServices.countNumberOf("gsc") == 2);
		assertTrue(hostAndServices.countNumberOf("gsm") == 1);
		assertTrue(hostAndServices.countNumberOf("gsa") == 1);
		assertTrue(hostAndServices.countNumberOf("lus") == 1);
	}	

}
