package test.webui;

import static framework.utils.AdminUtils.loadGSCs;
import static framework.utils.AdminUtils.loadGSM;
import static framework.utils.LogUtils.log;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.ProcessingUnit;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import framework.utils.AssertUtils;
import framework.utils.AssertUtils.RepetitiveConditionProvider;

import test.webui.objects.LoginPage;
import test.webui.objects.dashboard.DashboardTab;

public class TwoListenersLogoutTest extends AbstractSeleniumTest {
	
	Machine machineA;
	ProcessingUnit pu;
	GridServiceManager gsmA;
	
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
		gsmA = loadGSM(machineA); 
		loadGSCs(machineA, 1);
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT)
	public void logoutTest() throws InterruptedException {
		
		LoginPage loginPage1 = getLoginPage();
		
		final DashboardTab dashboardTab1 = loginPage1.login().switchToDashboard();
		
		DashboardTab dashboardTab2 = openAndSwitchToNewBrowser(FIREFOX).login().switchToDashboard();
		
		dashboardTab2.logout();
		
		admin.getGridServiceContainers().getContainers()[0].kill();
		admin.getGridServiceContainers().waitFor(0);
		
		RepetitiveConditionProvider condition = new RepetitiveConditionProvider() {
			
			@Override
			public boolean getCondition() {
				return (dashboardTab1.getServicesGrid().getInfrastructureGrid().getGSCInst().getCount() == 0);
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition, waitingTime);
		
	}

}
