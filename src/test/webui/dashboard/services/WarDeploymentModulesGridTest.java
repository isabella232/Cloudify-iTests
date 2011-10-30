package test.webui.dashboard.services;

import static framework.utils.AdminUtils.loadGSCs;
import static framework.utils.AdminUtils.loadGSM;
import static framework.utils.LogUtils.log;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.DeploymentStatus;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitDeployment;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import framework.utils.AssertUtils;
import framework.utils.DeploymentUtils;
import framework.utils.LogUtils;
import framework.utils.ProcessingUnitUtils;
import framework.utils.AssertUtils.RepetitiveConditionProvider;

import test.webui.AbstractSeleniumTest;
import test.webui.objects.LoginPage;
import test.webui.objects.dashboard.DashboardTab;
import test.webui.objects.dashboard.ServicesGrid;
import test.webui.objects.dashboard.ServicesGrid.ApplicationServicesGrid;
import test.webui.objects.dashboard.ServicesGrid.Icon;

public class WarDeploymentModulesGridTest extends AbstractSeleniumTest {
	
	Machine machineA;
	ProcessingUnit puSessionTest;
	
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
		puSessionTest = gsmA.deploy(new ProcessingUnitDeployment(DeploymentUtils.getArchive("session-test-embedded.war")));
		ProcessingUnitUtils.waitForDeploymentStatus(puSessionTest, DeploymentStatus.INTACT);
	}
	
	 @Test(timeOut = DEFAULT_TEST_TIMEOUT)
	public void checkModules() throws InterruptedException {
		
		// get new login page
		LoginPage loginPage = getLoginPage();
		
		// get new dashboard tab
		DashboardTab dashboardTab = loginPage.login().switchToDashboard();
		
		ServicesGrid appGrid = dashboardTab.getServicesGrid();
		
		final ApplicationServicesGrid modulesGrid = appGrid.getApplicationServicesGrid();
		
		RepetitiveConditionProvider condition = new RepetitiveConditionProvider() {
			
			
			public boolean getCondition() {
				return (modulesGrid.getWebModule().getCount() == 1);
			}
		};
		
		AssertUtils.repetitiveAssertTrue(null, condition,3000);
		assertTrue(modulesGrid.getWebModule().getIcon().equals(Icon.OK));

	}
	
	@AfterMethod(alwaysRun = true)
	public void undeployPu() {
		puSessionTest.undeploy();
		ProcessingUnitUtils.waitForDeploymentStatus(puSessionTest, DeploymentStatus.UNDEPLOYED);
	}

}
