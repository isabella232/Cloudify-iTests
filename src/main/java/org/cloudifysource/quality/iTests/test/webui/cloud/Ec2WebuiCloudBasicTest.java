package org.cloudifysource.quality.iTests.test.webui.cloud;

import iTests.framework.utils.AssertUtils;
import iTests.framework.utils.AssertUtils.RepetitiveConditionProvider;
import iTests.framework.utils.LogUtils;
import iTests.framework.utils.ScriptUtils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;

import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.NewAbstractCloudTest;
import org.cloudifysource.quality.iTests.test.webui.WebuiTestUtils;
import org.cloudifysource.restclient.GSRestClient;
import org.cloudifysource.restclient.RestException;
import org.openspaces.admin.pu.DeploymentStatus;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.gigaspaces.webuitf.LoginPage;
import com.gigaspaces.webuitf.MainNavigation;
import com.gigaspaces.webuitf.WebConstants;
import com.gigaspaces.webuitf.dashboard.DashboardTab;
import com.gigaspaces.webuitf.dashboard.alerts.AlertsPanel;
import com.gigaspaces.webuitf.dashboard.alerts.AlertsPanel.WebUIAlert;
import com.gigaspaces.webuitf.dashboard.events.DashboardEventsGrid;
import com.gigaspaces.webuitf.dashboard.events.EventsPanel;
import com.gigaspaces.webuitf.events.WebUIAdminEvent;
import com.gigaspaces.webuitf.topology.TopologySubPanel;
import com.gigaspaces.webuitf.topology.TopologyTab;
import com.gigaspaces.webuitf.topology.applicationmap.ApplicationMap;
import com.gigaspaces.webuitf.topology.applicationmap.ApplicationNode;
import com.gigaspaces.webuitf.topology.events.TopologyEventsGrid;
import com.gigaspaces.webuitf.topology.healthpanel.HealthPanel;
import com.gigaspaces.webuitf.topology.logicalpanel.LogicalPanel;
import com.gigaspaces.webuitf.topology.logicalpanel.WebUIProcessingUnitInstance;
import com.gigaspaces.webuitf.topology.logspanel.LogsPanel;
import com.gigaspaces.webuitf.topology.logspanel.PuLogsPanelService;
import com.gigaspaces.webuitf.topology.physicalpanel.HostData;
import com.gigaspaces.webuitf.topology.physicalpanel.PhysicalPanel;
import com.gigaspaces.webuitf.topology.recipes.RecipesPanel;
import com.gigaspaces.webuitf.topology.recipes.selectionpanel.RecipeFolderNode;
import com.j_spaces.kernel.PlatformVersion;

public class Ec2WebuiCloudBasicTest extends NewAbstractCloudTest {
	
	private static final String MANAGEMENT_APPLICATION_NAME = "management";
	private static final String applicationFolderName = "petclinic-simple";
	private String restUrl;
	private static final String applicationName = "petclinic";
	private static final String restApplicationName = "rest";
	//private static final String webUiApplicationName = "webui";	
	private static final String APPLICATION_PU_LABEL = applicationName +".tomcat";	
	private static final String METRICS_ASSERTION_SUFFIX = "No such metric";
	private static final String SERVICE_NAME = applicationName+".mongod";
	private static final String mongodInstanceRestUrl = "ProcessingUnits/Names/petclinic.mongod/Instances/0";
	private static String applicationPath;
	private DashboardTab dashboardTab;
	private TopologyTab topologyTab;
//	private ServicesTab servicesTab;
	private MainNavigation mainNav;
	private static long assertWaitingTime = 10000;
	
	private WebuiTestUtils webuiHelper;

	@Override
	protected String getCloudName() {
		return "ec2";
	}

	@Override
	protected boolean isReusableCloud() {		
		return false;
	}
	
	@BeforeClass(alwaysRun = true)
	protected void bootstrap() throws Exception {
		super.bootstrap();
		try {
			webuiHelper = new WebuiTestUtils(cloudService);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		} catch (UnknownHostException e) {
			throw new RuntimeException(e);
		}
	}
	
	@AfterClass(alwaysRun = true)
	protected void teardown() throws Exception {
		super.teardown();
		try {
			webuiHelper.close();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}
	
		
	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT)
	public void basicTest() throws InterruptedException, IOException, RestException {
		LogUtils.log("Starting test - ec2 webui cloud");
		applicationPath = ScriptUtils.getBuildPath() + "/recipes/apps/" + applicationFolderName;		
		installApplicationAndWait(applicationPath, applicationName);
		restUrl = getRestUrl();	
		
		// begin tests on the webui 
		LoginPage loginPage = webuiHelper.getLoginPage();
		
		mainNav = loginPage.login();

		dashboardTab = mainNav.switchToDashboard();
			
		checkDashboard();  
		
		topologyTab = mainNav.switchToTopology();				
		
		checkApplicationsTab(); 				
		
		mainNav.switchToServices();
		
 
		////// end tests of webui /////
			
		
		uninstallApplicationAndWait(applicationName);
		
		LogUtils.log("End of test - ec2 webui cloud");
	}

	
	@SuppressWarnings("unchecked")
	private void checkApplicationsTab() throws RestException, MalformedURLException {			
		LogUtils.log("Testing the application tab");
		
		topologyTab.selectApplication(MANAGEMENT_APPLICATION_NAME);
		final ApplicationMap appMap = topologyTab.getApplicationMap();
		
		//trying to get rest application node
		tryToGetApplicationNode(appMap, restApplicationName);	
		ApplicationNode applicationRestNode = appMap.getApplicationNode(restApplicationName);		
		AssertUtils.assertTrue("Status of rest is not OK",applicationRestNode.getStatus().equals(DeploymentStatus.INTACT));
		
		topologyTab.selectApplication(applicationName);
		tryToGetApplicationNode(appMap, APPLICATION_PU_LABEL);	
		ApplicationNode applicationTomcatNode = appMap.getApplicationNode(APPLICATION_PU_LABEL);
		AssertUtils.assertTrue("Status of tomcat app is not OK",applicationTomcatNode.getStatus().equals(DeploymentStatus.INTACT));
		
		// Get host name		
		restUrl = getRestUrl();	
		GSRestClient client = new GSRestClient("", "", new URL(this.restUrl), PlatformVersion.getVersionNumber());
		Map<String, Object> entriesJsonMap  = client.getAdminData(mongodInstanceRestUrl);				
		Map<String, Object> details = (Map<String,Object>) entriesJsonMap.get("OSDetails");
		String hostName = (String) details.get("HostName");
		// Got host name
		
		// checking if mongod exists and select it
		tryToGetApplicationNode(appMap, SERVICE_NAME);
		ApplicationNode applicationMongodNode = appMap.getApplicationNode(SERVICE_NAME);
		applicationMongodNode.select();
		// mongod selected
		
		TopologySubPanel topologySubPanel = topologyTab.getTopologySubPanel();
		AssertUtils.assertNotNull("sub topology panel is null" ,topologySubPanel);		
		HealthPanel metricsTab = topologySubPanel.switchToHealthPanel();
		AssertUtils.assertNotNull("metrics panel is null", metricsTab);
		
		// check the metrics of mongod
		AssertUtils.assertTrue("Memory " + METRICS_ASSERTION_SUFFIX , metricsTab.getMetric(WebConstants.METRIC_CLASS_NAMES.OS_MEMORY_METRIC_NAME) != null);
		AssertUtils.assertTrue("CPU " + METRICS_ASSERTION_SUFFIX, metricsTab.getMetric(WebConstants.METRIC_CLASS_NAMES.OS_CPU_METRIC_NAME) != null);		
		
		//check the hosts of mongod
		PhysicalPanel hosts = topologySubPanel.switchToPhysicalPanel();
		AssertUtils.assertNotNull(hosts);		
		HostData hostData = hosts.getHostData(hostName);
		AssertUtils.assertNotNull("host is null",hostData);
		
		//check the services of mongod	
		LogicalPanel services = topologySubPanel.switchToLogicalPanel();
		AssertUtils.assertNotNull(services);		
		String mongodService = "mongod [1]";
		WebUIProcessingUnitInstance servicePuInstance = services.getProcessingUnitInstance(mongodService);
		AssertUtils.assertNotNull(servicePuInstance);
		
		//check the logs of mongod
		LogsPanel logs = topologySubPanel.switchToLogsPanel();
		AssertUtils.assertNotNull(logs);		
		PuLogsPanelService puLogsPanelService = logs.getPuLogsPanelService(mongodService);
		AssertUtils.assertNotNull(puLogsPanelService);
		
		//check the events grid of mongod
		TopologyEventsGrid eventsGrid = topologySubPanel.switchToEventsGrid();
		AssertUtils.assertNotNull(eventsGrid);		
		WebUIAdminEvent chronologicalEvent = eventsGrid.getEvent(1);
		AssertUtils.assertNotNull(chronologicalEvent);
		
		//check the events timeline of mongod		
		EventsPanel eventsTimeline = topologySubPanel.switchToEventsPanel();
		AssertUtils.assertNotNull(eventsTimeline);		
		WebUIAdminEvent firstTimelineEvent = eventsTimeline.getEvent(1);
		AssertUtils.assertNotNull(firstTimelineEvent);
		
		//check the recipes of mongod
		RecipesPanel recipes = topologySubPanel.switchToRecipes();
		AssertUtils.assertNotNull(recipes);		
		RecipeFolderNode recipeFolderNode = recipes.getSelectionPanel().getRecipeFolderNode(SERVICE_NAME);
		AssertUtils.assertNotNull(recipeFolderNode);
		
		
		LogUtils.log("application tab is OK");
	}

	
	private void checkDashboard() {
		LogUtils.log("Testing the dashboard tab");
		// asserting the alerts 
		AlertsPanel alerts = dashboardTab.getDashboardSubPanel().switchToAlertsPanel();
		AssertUtils.assertNotNull("Alerts panel is null",alerts);
		List<WebUIAlert> allAlerts = alerts.getAlertsAppartFrom(null);
		for (int i=0;i<allAlerts.size();i++){
			AssertUtils.assertNotNull("There is one alert which is null",allAlerts.get(i));
		}
		
		// asserting the Event grid
		DashboardEventsGrid eventGrid = dashboardTab.getDashboardSubPanel().switchToEventsGrid();
		AssertUtils.assertNotNull("Event grid panel is null",eventGrid);
		List<WebUIAdminEvent> visibleEvents = eventGrid.getVisibleEvents();
		for (int i=0;i<visibleEvents.size();i++){
			AssertUtils.assertNotNull("There is an event grid which is null",visibleEvents.get(i));
		}
		
		//asserting events timeline
		EventsPanel eventsTimeline = dashboardTab.getDashboardSubPanel().switchToEventsPanel();
		AssertUtils.assertNotNull(eventsTimeline);
		List<WebUIAdminEvent> allEventsTimeline = eventsTimeline.getVisibleEvents();
		for (int i=0;i<allEventsTimeline.size();i++){
			AssertUtils.assertNotNull("There is an event timeline which is null",allEventsTimeline.get(i));
		}
		LogUtils.log("dashboard tab is OK");
	}
	
	private void tryToGetApplicationNode (final ApplicationMap appMap ,final String name){
		RepetitiveConditionProvider condition = new RepetitiveConditionProvider() {
			
			@Override
			public boolean getCondition() {
				ApplicationNode simple = appMap.getApplicationNode(name);
				return simple != null;
			}
		};
				
		AssertUtils.repetitiveAssertTrue("could not find application node after 10 seconds", condition, assertWaitingTime);
	}
	
}
