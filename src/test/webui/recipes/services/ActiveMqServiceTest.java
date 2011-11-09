package test.webui.recipes.services;

import java.io.IOException;

import org.openspaces.admin.pu.DeploymentStatus;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import framework.utils.AssertUtils;
import framework.utils.AssertUtils.RepetitiveConditionProvider;

import test.webui.objects.LoginPage;
import test.webui.objects.MainNavigation;
import test.webui.objects.dashboard.DashboardTab;
import test.webui.objects.dashboard.ServicesGrid;
import test.webui.objects.dashboard.ServicesGrid.ApplicationServicesGrid;
import test.webui.objects.dashboard.ServicesGrid.ApplicationsMenuPanel;
import test.webui.objects.dashboard.ServicesGrid.Icon;
import test.webui.objects.dashboard.ServicesGrid.InfrastructureServicesGrid;
import test.webui.objects.services.PuTreeGrid;
import test.webui.objects.services.ServicesTab;
import test.webui.objects.topology.ApplicationMap;
import test.webui.objects.topology.ApplicationMap.ApplicationNode;
import test.webui.objects.topology.HealthPanel;
import test.webui.objects.topology.TopologyTab;

public class ActiveMqServiceTest extends AbstractSeleniumServiceRecipeTest {
	
	@Override
	@BeforeMethod
	public void bootstrapAndInstall() throws IOException, InterruptedException {
		setCurrentRecipe("activemq");
		super.bootstrapAndInstall();
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT)
	public void simpleRecipeTest() throws InterruptedException {
		
		// get new login page
		LoginPage loginPage = getLoginPage();
		
		MainNavigation mainNav = loginPage.login();
		
		DashboardTab dashboardTab = mainNav.switchToDashboard();
		
		final InfrastructureServicesGrid infrastructureServicesGrid = dashboardTab.getServicesGrid().getInfrastructureGrid();
		
		RepetitiveConditionProvider condition = new RepetitiveConditionProvider() {
			
			@Override
			public boolean getCondition() {
				return ((infrastructureServicesGrid.getESMInst().getCount() == 1) 
						&& (infrastructureServicesGrid.getESMInst().getIcon().equals(Icon.OK)));
			}
		};
		AssertUtils.repetitiveAssertTrue("No esm in showing in the dashboard", condition, waitingTime);

		
		ServicesGrid servicesGrid = dashboardTab.getServicesGrid();
		
		ApplicationsMenuPanel appMenu = servicesGrid.getApplicationsMenuPanel();
		
		appMenu.selectApplication(MANAGEMENT);
		
		final ApplicationServicesGrid applicationServicesGrid = servicesGrid.getApplicationServicesGrid();
		
		condition = new RepetitiveConditionProvider() {		
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
				return applicationServicesGrid.getMessageBusModule().getCount() == 1;
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
		
		ApplicationNode simple = appMap.getApplicationNode("activemq");
		
		assertTrue(simple != null);
		assertTrue(simple.getStatus().equals(DeploymentStatus.INTACT));	
		
		HealthPanel healthPanel = topologyTab.getTopologySubPanel().switchToHealthPanel();
		
		assertTrue(healthPanel.getMetric("Store Percent Usage") != null);
		
		ServicesTab servicesTab = mainNav.switchToServices();
		
		PuTreeGrid puTreeGrid = servicesTab.getPuTreeGrid();
		
		assertTrue(puTreeGrid.getProcessingUnit("webui") != null);
		assertTrue(puTreeGrid.getProcessingUnit("rest") != null);
		assertTrue(puTreeGrid.getProcessingUnit("activemq") != null);
		
	}
}
