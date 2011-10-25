package test.webui.recipes.applications;

import java.util.List;

import org.openspaces.admin.pu.DeploymentStatus;
import org.openspaces.admin.pu.ProcessingUnit;
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
import test.webui.objects.services.PuTreeGrid;
import test.webui.objects.services.ServicesTab;
import test.webui.objects.topology.ApplicationMap;
import test.webui.objects.topology.ApplicationMap.ApplicationNode;
import test.webui.objects.topology.ApplicationMap.ApplicationNode.Connector;
import test.webui.objects.topology.HealthPanel;
import test.webui.objects.topology.TopologyTab;
import test.webui.recipes.services.AbstractSeleniumServiceRecipeTest;

public class TravelTest extends AbstractSeleniumApplicationRecipeTest {
	
	private ProcessingUnit pu;

	@Override
	@BeforeMethod(alwaysRun = true)
	public void beforeTest() {
		setCurrentRecipe("travel");
		super.beforeTest();
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, groups = {"cloudify"})
	public void simpleRecipeTest() throws InterruptedException {

		// get new login page
		LoginPage loginPage = getLoginPage();

		MainNavigation mainNav = loginPage.login();

		DashboardTab dashboardTab = mainNav.switchToDashboard();

		ServicesGrid servicesGrid = dashboardTab.getServicesGrid();

		ApplicationsMenuPanel appMenu = servicesGrid.getApplicationsMenuPanel();

		appMenu.selectApplication(AbstractSeleniumServiceRecipeTest.MANAGEMENT);

		final ApplicationServicesGrid applicationServicesGrid = servicesGrid.getApplicationServicesGrid();

		RepetitiveConditionProvider condition = new RepetitiveConditionProvider() {		
			@Override
			public boolean getCondition() {
				return applicationServicesGrid.getWebModule().getCount() == 2;
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition, waitingTime);

		condition = new RepetitiveConditionProvider() {		
			@Override
			public boolean getCondition() {
				return applicationServicesGrid.getWebModule().getIcon().equals(Icon.OK);
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition, waitingTime);

		appMenu.selectApplication("travel");

		condition = new RepetitiveConditionProvider() {		
			@Override
			public boolean getCondition() {
				return applicationServicesGrid.getWebServerModule().getCount() == 1;
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition, waitingTime);

		condition = new RepetitiveConditionProvider() {		
			@Override
			public boolean getCondition() {
				return applicationServicesGrid.getWebServerModule().getIcon().equals(Icon.OK);
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition, waitingTime);

		condition = new RepetitiveConditionProvider() {		
			@Override
			public boolean getCondition() {
				return applicationServicesGrid.getNoSqlDbModule().getCount() == 1;
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition, waitingTime);

		condition = new RepetitiveConditionProvider() {		
			@Override
			public boolean getCondition() {
				return applicationServicesGrid.getNoSqlDbModule().getIcon().equals(Icon.OK);
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition, waitingTime);

		condition = new RepetitiveConditionProvider() {		
			@Override
			public boolean getCondition() {
				return applicationServicesGrid.getWebModule().getCount() == 1;
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition, waitingTime);

		condition = new RepetitiveConditionProvider() {		
			@Override
			public boolean getCondition() {
				return applicationServicesGrid.getWebModule().getIcon().equals(Icon.OK);
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition, waitingTime);

		TopologyTab topologyTab = mainNav.switchToTopology();

		final ApplicationMap appMap = topologyTab.getApplicationMap();

		appMap.selectApplication(AbstractSeleniumServiceRecipeTest.MANAGEMENT);

		ApplicationNode restful = appMap.getApplicationNode("rest");

		assertTrue(restful != null);
		assertTrue(restful.getStatus().equals(DeploymentStatus.INTACT));

		ApplicationNode webui = appMap.getApplicationNode("webui");

		assertTrue(webui != null);
		assertTrue(webui.getStatus().equals(DeploymentStatus.INTACT));

		appMap.selectApplication("travel");

		ApplicationNode cassandra = appMap.getApplicationNode("cassandra-service");

		assertTrue(cassandra != null);
		assertTrue(cassandra.getStatus().equals(DeploymentStatus.INTACT));	

		ApplicationNode tomcat = appMap.getApplicationNode("tomcat");

		assertTrue(tomcat != null);
		assertTrue(tomcat.getStatus().equals(DeploymentStatus.INTACT));		

		List<Connector> connectors = tomcat.getConnectors();
		assertTrue(connectors.size() == 2);
		List<Connector> targets = tomcat.getTargets();
		assertTrue(targets.size() == 1);
		assertTrue(targets.get(0).getTarget().equals(cassandra));

		cassandra.select();

		HealthPanel healthPanel = topologyTab.getTopologySubPanel().switchToHealthPanel();

		assertTrue(healthPanel.getMetric("Process Cpu Usage") != null);
		assertTrue(healthPanel.getMetric("Total Process Virtual Memory") != null);
		assertTrue(healthPanel.getMetric("Compaction Manager Completed Tasks") != null);
		assertTrue(healthPanel.getMetric("Compaction Manager Pending Tasks") != null);
		assertTrue(healthPanel.getMetric("Commit Log Active Tasks") != null);

		tomcat.select();

		assertTrue(healthPanel.getMetric("Process Cpu Usage") != null);
		assertTrue(healthPanel.getMetric("Total Process Virtual Memory") != null);
		assertTrue(healthPanel.getMetric("Num Of Active Threads") != null);
		assertTrue(healthPanel.getMetric("Current Http Threads Busy") != null);
		assertTrue(healthPanel.getMetric("Backlog") != null);
		assertTrue(healthPanel.getMetric("Active Sessions") != null);

		ServicesTab servicesTab = mainNav.switchToServices();

		PuTreeGrid puTreeGrid = servicesTab.getPuTreeGrid();

		assertTrue(puTreeGrid.getProcessingUnit("webui") != null);
		assertTrue(puTreeGrid.getProcessingUnit("rest") != null);
		assertTrue(puTreeGrid.getProcessingUnit("cassandra-service") != null);
		assertTrue(puTreeGrid.getProcessingUnit("tomcat") != null);

	}

}
