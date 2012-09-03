package test.webui.recipes.applications;

import java.io.IOException;
import java.util.List;

import org.cloudifysource.dsl.utils.ServiceUtils;
import org.openspaces.admin.pu.DeploymentStatus;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;



import com.gigaspaces.webuitf.LoginPage;
import com.gigaspaces.webuitf.MainNavigation;
import com.gigaspaces.webuitf.dashboard.DashboardTab;
import com.gigaspaces.webuitf.dashboard.ServicesGrid;
import com.gigaspaces.webuitf.dashboard.ServicesGrid.ApplicationServicesGrid;
import com.gigaspaces.webuitf.dashboard.ServicesGrid.ApplicationsMenuPanel;
import com.gigaspaces.webuitf.dashboard.ServicesGrid.Icon;
import com.gigaspaces.webuitf.dashboard.ServicesGrid.InfrastructureServicesGrid;
import com.gigaspaces.webuitf.services.PuTreeGrid;
import com.gigaspaces.webuitf.services.ServicesTab;
import com.gigaspaces.webuitf.topology.TopologyTab;
import com.gigaspaces.webuitf.topology.applicationmap.ApplicationMap;
import com.gigaspaces.webuitf.topology.applicationmap.ApplicationNode;
import com.gigaspaces.webuitf.topology.applicationmap.Connector;
import com.gigaspaces.webuitf.topology.healthpanel.HealthPanel;

import framework.utils.AssertUtils;
import framework.utils.AssertUtils.RepetitiveConditionProvider;

public class TravelTest extends AbstractSeleniumApplicationRecipeTest {
	
	private static final String TRAVEL_APPLICATION_NAME = "travel";
	private static final String CASSANDRA_SERVICE_FULL_NAME = ServiceUtils.getAbsolutePUName(TRAVEL_APPLICATION_NAME, "cassandra");
	private static final String TOMCAT_SERVICE_FULL_NAME = ServiceUtils.getAbsolutePUName(TRAVEL_APPLICATION_NAME, "tomcat");


	@Override
	@BeforeMethod(enabled = true)
	public void install() throws IOException, InterruptedException {
		setCurrentApplication(TRAVEL_APPLICATION_NAME);
		super.install();
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, enabled = true)
	public void travelApplicationTest() throws InterruptedException, IOException {

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

		appMenu.selectApplication(MANAGEMENT_APPLICATION_NAME);

		final ApplicationServicesGrid applicationServicesGrid = servicesGrid.getApplicationServicesGrid();

		condition = new RepetitiveConditionProvider() {		
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

		appMenu.selectApplication(TRAVEL_APPLICATION_NAME);

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

		TopologyTab topologyTab = mainNav.switchToTopology();

		final ApplicationMap appMap = topologyTab.getApplicationMap();

		topologyTab.selectApplication(MANAGEMENT_APPLICATION_NAME);

		ApplicationNode restful = appMap.getApplicationNode("rest");

		assertTrue(restful != null);
		assertTrue(restful.getStatus().equals(DeploymentStatus.INTACT));

		ApplicationNode webui = appMap.getApplicationNode("webui");

		assertTrue(webui != null);
		assertTrue(webui.getStatus().equals(DeploymentStatus.INTACT));

		topologyTab.selectApplication(TRAVEL_APPLICATION_NAME);

		ApplicationNode cassandra = appMap.getApplicationNode(CASSANDRA_SERVICE_FULL_NAME);

		assertTrue(cassandra != null);
		assertTrue(cassandra.getStatus().equals(DeploymentStatus.INTACT));	

		ApplicationNode tomcat = appMap.getApplicationNode(TOMCAT_SERVICE_FULL_NAME);

		assertTrue(tomcat != null);
		assertTrue(tomcat.getStatus().equals(DeploymentStatus.INTACT));		

		List<Connector> connectors = tomcat.getConnectors();
		assertTrue(connectors.size() == 1);
		List<Connector> targets = tomcat.getTargets();
		assertTrue(targets.size() == 1);
		assertTrue(targets.get(0).getTarget().getName().equals(cassandra.getName()));

		cassandra.select();

		HealthPanel healthPanel = topologyTab.getTopologySubPanel().switchToHealthPanel();

		tomcat.select();

		assertTrue("metric 0 is not displayed for tomcat recipe", healthPanel.getMetric(0) != null);
		assertTrue("metric 1 is not displayed for tomcat recipe", healthPanel.getMetric(1) != null);
		assertTrue("metric 2 is not displayed for tomcat recipe", healthPanel.getMetric(2) != null);
		assertTrue("metric 3 is not displayed for tomcat recipe", healthPanel.getMetric(3) != null);

		ServicesTab servicesTab = mainNav.switchToServices();

		PuTreeGrid puTreeGrid = servicesTab.getPuTreeGrid();

		assertTrue(puTreeGrid.getProcessingUnit("webui") != null);
		assertTrue(puTreeGrid.getProcessingUnit("rest") != null);
		assertTrue(puTreeGrid.getProcessingUnit(CASSANDRA_SERVICE_FULL_NAME) != null);
		assertTrue(puTreeGrid.getProcessingUnit(TOMCAT_SERVICE_FULL_NAME) != null);
		uninstallApplication(TRAVEL_APPLICATION_NAME, true);

	}

}
