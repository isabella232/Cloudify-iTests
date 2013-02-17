package org.cloudifysource.quality.iTests.test.webui.recipes.services;

import java.io.IOException;

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
import com.gigaspaces.webuitf.topology.healthpanel.HealthPanel;

import org.cloudifysource.quality.iTests.framework.utils.AssertUtils;
import org.cloudifysource.quality.iTests.framework.utils.AssertUtils.RepetitiveConditionProvider;

public class TomcatServiceTest extends AbstractSeleniumServiceRecipeTest {
	
	private static final String ACTIVE_SESSIONS = "gs-metric-title-CUSTOM_Active_Sessions";
	private static final String REQUEST_BACKLOG = "gs-metric-title-CUSTOM_Request_Backlog";
	private static final String CURRENT_HTTP_THREADS_BUSY = "gs-metric-title-CUSTOM_Current_Http_Threads_Busy";
	private static final String NUM_OF_ACTIVE_THREADS = "gs-metric-title-CUSTOM_Num_Of_Active_Threads";
	private static final String TOTAL_PROCESS_VIRTUAL_MEMORY = "gs-metric-title-CUSTOM_Total_Process_Virtual_Memory";
	private static final String PROCESS_CPU_USAGE = "gs-metric-title-CUSTOM_Process_Cpu_Usage";

	@Override
	@BeforeMethod
	public void install() throws IOException, InterruptedException {
		setCurrentRecipe("tomcat");
		super.install();
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void tomcatRecipeTest() throws InterruptedException, IOException {

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

		appMenu.selectApplication(DEFAULT_APPLICATION_NAME);

		condition = new RepetitiveConditionProvider() {		
			@Override
			public boolean getCondition() {
				return applicationServicesGrid.getAppServerModule().getCount() == 1;
			}
		};
		AssertUtils.repetitiveAssertTrue("web server module - expected: 1, actual: " + applicationServicesGrid.getAppServerModule().getCount(), condition, waitingTime);

		TopologyTab topologyTab = mainNav.switchToTopology();

		final ApplicationMap appMap = topologyTab.getApplicationMap();

		topologyTab.selectApplication(MANAGEMENT_APPLICATION_NAME);
		
		ApplicationNode restful = appMap.getApplicationNode("rest");

		assertTrue(restful != null);
		assertTrue(restful.getStatus().equals(DeploymentStatus.INTACT));

		ApplicationNode webui = appMap.getApplicationNode("webui");

		assertTrue(webui != null);
		assertTrue(webui.getStatus().equals(DeploymentStatus.INTACT));

		topologyTab.selectApplication(DEFAULT_APPLICATION_NAME);
				
		condition = new RepetitiveConditionProvider() {
			
			@Override
			public boolean getCondition() {
				ApplicationNode simple = appMap.getApplicationNode(DEFAULT_TOMCAT_SERVICE_FULL_NAME);
				return simple != null;
			}
		};
		AssertUtils.repetitiveAssertTrue("could not find tomcat application node after 10 seconds", condition, 10 * 1000);
		
		final ApplicationNode tomcatNode = appMap.getApplicationNode(DEFAULT_TOMCAT_SERVICE_FULL_NAME);
		
		takeScreenShot(this.getClass(), "tomcatRecipeTest","topology");
		
		condition = new RepetitiveConditionProvider() {
			
			@Override
			public boolean getCondition() {
				return tomcatNode.getStatus().equals(DeploymentStatus.INTACT);
			}
		};
		repetitiveAssertTrueWithScreenshot(
				"tomcat service is displayed as " + appMap.getApplicationNode(DEFAULT_TOMCAT_SERVICE_FULL_NAME).getStatus() + 
					"even though it is installed", condition, this.getClass(), "tomcatRecipeTest", "tomcat-service");

		HealthPanel healthPanel = topologyTab.getTopologySubPanel().switchToHealthPanel();

		takeScreenShot(this.getClass(),"tomcatRecipeTest", "topology-healthpanel");
		
		assertTrue("Process Cpu Usage " + METRICS_ASSERTION_SUFFIX , healthPanel.getMetric(PROCESS_CPU_USAGE) != null);
		assertTrue("Total Process Virtual Memory" + METRICS_ASSERTION_SUFFIX, healthPanel.getMetric(TOTAL_PROCESS_VIRTUAL_MEMORY) != null);
		assertTrue("Num Of Active Threads" + METRICS_ASSERTION_SUFFIX , healthPanel.getMetric(NUM_OF_ACTIVE_THREADS) != null);
		assertTrue("Current Http Threads Busy" + METRICS_ASSERTION_SUFFIX, healthPanel.getMetric(CURRENT_HTTP_THREADS_BUSY) != null);
		assertTrue("Request Backlog" + METRICS_ASSERTION_SUFFIX , healthPanel.getMetric(REQUEST_BACKLOG) != null);
		assertTrue("Active Sessions" + METRICS_ASSERTION_SUFFIX, healthPanel.getMetric(ACTIVE_SESSIONS) != null);

		ServicesTab servicesTab = mainNav.switchToServices();

		PuTreeGrid puTreeGrid = servicesTab.getPuTreeGrid();

		assertTrue(puTreeGrid.getProcessingUnit("webui") != null);
		assertTrue(puTreeGrid.getProcessingUnit("rest") != null);
		assertTrue(puTreeGrid.getProcessingUnit(DEFAULT_TOMCAT_SERVICE_FULL_NAME) != null);
		uninstallService("tomcat", true);

	}

}
