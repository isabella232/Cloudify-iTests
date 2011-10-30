package test.webui.services.hosts;

import static framework.utils.AdminUtils.loadGSCs;
import static framework.utils.AdminUtils.loadGSM;
import static framework.utils.LogUtils.log;

import org.junit.Assert;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.machine.Machine;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import framework.utils.LogUtils;

import test.webui.AbstractSeleniumTest;
import test.webui.objects.LoginPage;
import test.webui.objects.services.HostsAndServicesGrid;
import test.webui.objects.services.ServicesTab;

public class StartGridServiceContainerTest extends AbstractSeleniumTest {
	
	Machine machineA;
	GridServiceAgent gsaA;
	
	@BeforeMethod(alwaysRun = true)
	public void startSetup() {
		log("waiting for 1 machine");
		admin.getMachines().waitFor(1);

		log("waiting for 1 GSA");
		admin.getGridServiceAgents().waitFor(1);

		GridServiceAgent[] agents = admin.getGridServiceAgents().getAgents();
		gsaA = agents[0];

		machineA = gsaA.getMachine();

		log("starting: 1 GSM and 2 GSC's on 1 machine");
		GridServiceManager gsmA = loadGSM(machineA); 
		loadGSCs(machineA, 2);
	}
	
	 @Test(timeOut = DEFAULT_TEST_TIMEOUT)
	public void startContainer() throws InterruptedException {
		
		// get new login page
		LoginPage loginPage = getLoginPage();
		
		// get new topology tab
		ServicesTab topologyTab = loginPage.login().switchToServices();
		
		// get the needed grid
		HostsAndServicesGrid hostAndServices = topologyTab.getHostAndServicesGrid();
		
		// tell the grid witch gsa to work with
		hostAndServices.setGsaPID(gsaA);
		
		LogUtils.log("starting gsc...");
		hostAndServices.startGridServiceComponent(machineA.getHostName(), 
				HostsAndServicesGrid.GSC);
		
		// wait for the new gsc
		admin.getGridServiceContainers().waitFor(3);
		
		// assert 3 gsc is visible
		int seconds = 0;
		while(true) {
			if (seconds == 15) Assert.fail();
			if (hostAndServices.countNumberOf("gsc") == 3) break;
			Thread.sleep(1000);
			seconds++;
		}
		
	}

}
