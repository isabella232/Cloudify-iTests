package test.webui.recipes.services;

import java.io.IOException;

import org.openspaces.admin.pu.DeploymentStatus;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

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
import test.webui.objects.topology.HealthPanel;
import test.webui.objects.topology.TopologyTab;
import framework.utils.AssertUtils;
import framework.utils.AssertUtils.RepetitiveConditionProvider;

public class CassandraServiceTest extends AbstractSeleniumServiceRecipeTest  {
	
	@Override
	@BeforeMethod
	public void bootstrapAndInstall() throws IOException, InterruptedException {
		setCurrentRecipe("cassandra");
		super.bootstrapAndInstall();
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT)
	public void simpleRecipeTest() throws InterruptedException {

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
				return applicationServicesGrid.getNoSqlDbModule().getCount() == 1;
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

		ApplicationNode simple = appMap.getApplicationNode("cassandra");

		assertTrue(simple != null);
		assertTrue(simple.getStatus().equals(DeploymentStatus.INTACT));	

		HealthPanel healthPanel = topologyTab.getTopologySubPanel().switchToHealthPanel();

		assertTrue(healthPanel.getMetric("Process Cpu Usage") != null);
		assertTrue(healthPanel.getMetric("Total Process Virtual Memory") != null);
		assertTrue(healthPanel.getMetric("Compaction Manager Completed Tasks") != null);
		assertTrue(healthPanel.getMetric("Compaction Manager Pending Tasks") != null);
		assertTrue(healthPanel.getMetric("Commit Log Active Tasks") != null);

		ServicesTab servicesTab = mainNav.switchToServices();

		PuTreeGrid puTreeGrid = servicesTab.getPuTreeGrid();

		assertTrue(puTreeGrid.getProcessingUnit("webui") != null);
		assertTrue(puTreeGrid.getProcessingUnit("rest") != null);
		assertTrue(puTreeGrid.getProcessingUnit("cassandra") != null);

	}

}
