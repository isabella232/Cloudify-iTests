package test.webui.dashboard.grid;

import static framework.utils.AdminUtils.loadGSCs;
import static framework.utils.AdminUtils.loadGSM;
import static framework.utils.LogUtils.log;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.machine.Machine;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.webui.AbstractSeleniumTest;
import test.webui.objects.LoginPage;
import test.webui.objects.dashboard.DashboardTab;
import test.webui.objects.dashboard.StatusGrid.CPUCores;
import test.webui.objects.dashboard.StatusGrid.Memory;

public class GridStatusTest extends AbstractSeleniumTest {
	
	Machine machineA;
	GridServiceAgent gsaA;
	
	@BeforeMethod
	public void startSetup() {
		log("waiting for 1 machine");
		admin.getMachines().waitFor(1);

		log("waiting for 1 GSA");
		admin.getGridServiceAgents().waitFor(1);

		GridServiceAgent[] agents = admin.getGridServiceAgents().getAgents();
		gsaA = agents[0];

		machineA = gsaA.getMachine();

		log("starting: 1 GSM and 2 GSC's on 1 machine");
		loadGSM(machineA); 
		loadGSCs(machineA, 2);
	}
	
	 @Test(timeOut = DEFAULT_TEST_TIMEOUT)
	public void gridStatusTest() throws InterruptedException {
		
		// get new login page
		LoginPage loginPage = getLoginPage();
		
		// get new topology tab
		DashboardTab dashboardTab = loginPage.login().switchToDashboard();
		
		// wait for browser to stabilize
		Thread.sleep(10000);
		
		Memory mem = dashboardTab.getStatusGrid().getMemory();
		CPUCores cpuCores = dashboardTab.getStatusGrid().getCpuCores();
		
		double cpuPercentage = cpuCores.getCount();
		double memoryPercentage = mem.getCount();
		
		assertTrue( (cpuPercentage <= 100) && (cpuPercentage >= 0));
		assertTrue( (memoryPercentage <= 100) && (memoryPercentage >= 0));
		assertTrue(dashboardTab.getStatusGrid().getGridHealth().equals("Good"));
		
	}

}
