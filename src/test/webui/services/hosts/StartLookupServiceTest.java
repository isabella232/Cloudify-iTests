package test.webui.services.hosts;

import static test.utils.AdminUtils.loadGSCs;
import static test.utils.AdminUtils.loadGSM;
import static test.utils.LogUtils.log;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.machine.Machine;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.utils.AssertUtils;
import test.utils.LogUtils;
import test.utils.AssertUtils.RepetitiveConditionProvider;
import test.webui.AbstractSeleniumTest;
import test.webui.objects.LoginPage;
import test.webui.objects.services.HostsAndServicesGrid;
import test.webui.objects.services.ServicesTab;

public class StartLookupServiceTest extends AbstractSeleniumTest {
	
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
	
	 @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = {"cloudify" , "xap"})
	public void startLookup() throws InterruptedException {
		
		// get new login page
		LoginPage loginPage = getLoginPage();
		
		// get new topology tab
		ServicesTab topologyTab = loginPage.login().switchToServices();
		
		// get the needed grid
		HostsAndServicesGrid hostAndServices = topologyTab.getHostAndServicesGrid();
		
		// tell the grid witch gsa to work with
		hostAndServices.setGsaPID(gsaA);
		
		LogUtils.log("starting lus...");
		hostAndServices.startGridServiceComponent(machineA.getHostName(), 
				HostsAndServicesGrid.LUS);
		
		RepetitiveConditionProvider condition = new RepetitiveConditionProvider() {
			
			
			public boolean getCondition() {
				return verifyAlertThrown();
			}
		};
		
		// verify an alert pops up, saying lus cannot be launched.
		AssertUtils.repetitiveAssertTrue(null, condition, 3000);
		
	}

}
