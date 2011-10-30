package test.webui.recipes.services;

import org.openspaces.admin.pu.DeploymentStatus;
import org.testng.annotations.BeforeMethod;

import test.webui.objects.LoginPage;
import test.webui.objects.MainNavigation;
import test.webui.objects.dashboard.DashboardTab;
import test.webui.objects.dashboard.ServicesGrid;
import test.webui.objects.dashboard.ServicesGrid.ApplicationServicesGrid;
import test.webui.objects.dashboard.ServicesGrid.ApplicationsMenuPanel;
import test.webui.objects.services.PuTreeGrid;
import test.webui.objects.services.ServicesTab;
import test.webui.objects.topology.ApplicationMap;
import test.webui.objects.topology.ApplicationMap.ApplicationNode;
import test.webui.objects.topology.TopologyTab;
import framework.utils.AssertUtils;
import framework.utils.AssertUtils.RepetitiveConditionProvider;

public class NginxServiceTest extends AbstractSeleniumServiceRecipeTest {
	
	@Override
	@BeforeMethod(alwaysRun = true)
	public void beforeTest() {
		setCurrentRecipe("nginx");
		super.beforeTest();
	}
	
	//@Test(timeOut = DEFAULT_TEST_TIMEOUT)
	public void mongosTest() throws InterruptedException {
		
		// get new login page
		LoginPage loginPage = getLoginPage();
		
		MainNavigation mainNav = loginPage.login();
		
		DashboardTab dashboardTab = mainNav.switchToDashboard();
		
		ServicesGrid servicesGrid = dashboardTab.getServicesGrid();
		
		ApplicationsMenuPanel appMenu = servicesGrid.getApplicationsMenuPanel();
		
		appMenu.selectApplication(MANAGEMENT);
		
		final ApplicationServicesGrid applicationServicesGrid = servicesGrid.getApplicationServicesGrid();
		
		RepetitiveConditionProvider condition = new RepetitiveConditionProvider() {		
			@Override
			public boolean getCondition() {
				return applicationServicesGrid.getWebModule().getCount() == 2;
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition, waitingTime);
		
		appMenu.selectApplication("default");
		
		condition = new RepetitiveConditionProvider() {		
			@Override
			public boolean getCondition() {
				return applicationServicesGrid.getWebServerModule().getCount() == 1;
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition, waitingTime);
		
		TopologyTab topologyTab = mainNav.switchToTopology();
		
		final ApplicationMap appMap = topologyTab.getApplicationMap();
		
		appMap.selectApplication(MANAGEMENT);
		
		ApplicationNode restful = appMap.getApplicationNode("rest");
		
		assertTrue(restful != null);
		assertTrue(restful.getStatus().equals(DeploymentStatus.INTACT));
		
		ApplicationNode webui = appMap.getApplicationNode("webui");
		
		assertTrue(webui != null);
		assertTrue(webui.getStatus().equals(DeploymentStatus.INTACT));
		
		appMap.selectApplication("default");
		
		ApplicationNode mongocfgNode = appMap.getApplicationNode("nginx");
		
		assertTrue(mongocfgNode != null);
		assertTrue(mongocfgNode.getStatus().equals(DeploymentStatus.INTACT));
		
		ServicesTab servicesTab = mainNav.switchToServices();

		PuTreeGrid puTreeGrid = servicesTab.getPuTreeGrid();

		assertTrue(puTreeGrid.getProcessingUnit("webui") != null);
		assertTrue(puTreeGrid.getProcessingUnit("rest") != null);
		assertTrue(puTreeGrid.getProcessingUnit("nginx") != null);
		
	}

}
