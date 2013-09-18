package org.cloudifysource.quality.iTests.test.webui.recipes.applications;

import iTests.framework.utils.AssertUtils;
import iTests.framework.utils.LogUtils;
import iTests.framework.utils.AssertUtils.RepetitiveConditionProvider;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import org.cloudifysource.dsl.utils.ServiceUtils;
import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
import org.cloudifysource.quality.iTests.test.cli.cloudify.AbstractLocalCloudTest;
import org.cloudifysource.quality.iTests.test.webui.AbstractWebUILocalCloudTest;
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
import com.gigaspaces.webuitf.services.ServicesTab;
import com.gigaspaces.webuitf.topology.TopologyTab;
import com.gigaspaces.webuitf.topology.applicationmap.ApplicationMap;
import com.gigaspaces.webuitf.topology.applicationmap.ApplicationNode;
import com.gigaspaces.webuitf.topology.healthpanel.HealthPanel;

public class TravelTest extends AbstractSeleniumApplicationRecipeTest {
	
	@Override
	@BeforeMethod(enabled = true)
	public void install() throws IOException, InterruptedException {
		setCurrentApplication(TRAVEL_APPLICATION_NAME);
		super.install();
	}

	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 2, enabled = true)
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
		AssertUtils.repetitiveAssertTrue("No esm in showing in the dashboard", condition, AbstractWebUILocalCloudTest.waitingTime);


		ServicesGrid servicesGrid = dashboardTab.getServicesGrid();

		ApplicationsMenuPanel appMenu = servicesGrid.getApplicationsMenuPanel();

		appMenu.selectApplication(AbstractLocalCloudTest.MANAGEMENT_APPLICATION_NAME);

		final ApplicationServicesGrid applicationServicesGrid = servicesGrid.getApplicationServicesGrid();

		condition = new RepetitiveConditionProvider() {		
			@Override
			public boolean getCondition() {
				return applicationServicesGrid.getWebModule().getCount() == 2;
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition, AbstractWebUILocalCloudTest.waitingTime);

		condition = new RepetitiveConditionProvider() {		
			@Override
			public boolean getCondition() {
				return applicationServicesGrid.getWebModule().getIcon().equals(Icon.OK);
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition, AbstractWebUILocalCloudTest.waitingTime);

		appMenu.selectApplication(TRAVEL_APPLICATION_NAME);

		condition = new RepetitiveConditionProvider() {		
			@Override
			public boolean getCondition() {
				return applicationServicesGrid.getAppServerModule().getCount() == 1;
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition, AbstractWebUILocalCloudTest.waitingTime);

		condition = new RepetitiveConditionProvider() {		
			@Override
			public boolean getCondition() {
				return applicationServicesGrid.getAppServerModule().getIcon().equals(Icon.OK);
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition, AbstractWebUILocalCloudTest.waitingTime);

		condition = new RepetitiveConditionProvider() {		
			@Override
			public boolean getCondition() {
				return applicationServicesGrid.getNoSqlDbModule().getCount() == 1;
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition, AbstractWebUILocalCloudTest.waitingTime);

		condition = new RepetitiveConditionProvider() {		
			@Override
			public boolean getCondition() {
				return applicationServicesGrid.getNoSqlDbModule().getIcon().equals(Icon.OK);
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition, AbstractWebUILocalCloudTest.waitingTime);

		TopologyTab topologyTab = mainNav.switchToTopology();

		final ApplicationMap appMap = topologyTab.getApplicationMap();

		topologyTab.selectApplication(AbstractLocalCloudTest.MANAGEMENT_APPLICATION_NAME);

		//check [rest] service
		String checkedServiceName = DEFAULT_REST_SERVICE_NAME;
		ApplicationNode restful = appMap.getApplicationNode( checkedServiceName );

		AbstractTestSupport.assertTrue(restful != null);
		String status = appMap.getApplicationNodeStatus( checkedServiceName );
		AbstractTestSupport.assertTrue("Status of [" + checkedServiceName + 
				"] service must be 'ok'", status != null && status.equals(ApplicationMap.CONN_STATUS_OK));

		//check [webui] service
		checkedServiceName = DEFAULT_WEBUI_SERVICE_NAME;
		ApplicationNode webui = appMap.getApplicationNode(checkedServiceName);

		AbstractTestSupport.assertTrue(webui != null);
		status = appMap.getApplicationNodeStatus(checkedServiceName);
		AbstractTestSupport.assertTrue("Status of [" + checkedServiceName + 
				"] service must be 'ok'", status != null && status.equals(ApplicationMap.CONN_STATUS_OK));

		//pass to 'travel' application
		topologyTab.selectApplication(TRAVEL_APPLICATION_NAME);

		//check [cassandra] service
		checkedServiceName = DEFAULT_CASSANDRA_SERVICE_NAME;
		ApplicationNode cassandra = appMap.getApplicationNode(checkedServiceName);

		AbstractTestSupport.assertTrue(cassandra != null);
		status = appMap.getApplicationNodeStatus(checkedServiceName);
		AbstractTestSupport.assertTrue("Status of [" + checkedServiceName + 
				"] service must be 'ok'", status != null && status.equals(ApplicationMap.CONN_STATUS_OK));

		//check [tomcat] service
		checkedServiceName = DEFAULT_TOMCAT_SERVICE_NAME;
		String checkedFullServiceName = ServiceUtils.getAbsolutePUName(TRAVEL_APPLICATION_NAME, DEFAULT_TOMCAT_SERVICE_NAME);
		String checkedCassandraFullServiceName = ServiceUtils.getAbsolutePUName(TRAVEL_APPLICATION_NAME, DEFAULT_CASSANDRA_SERVICE_NAME);
		ApplicationNode tomcat = appMap.getApplicationNode(checkedServiceName);

		AbstractTestSupport.assertTrue(tomcat != null);
		status = appMap.getApplicationNodeStatus(checkedServiceName);
		AbstractTestSupport.assertTrue("Status of [" + checkedServiceName + 
				"] service must be 'ok'", status != null && status.equals(ApplicationMap.CONN_STATUS_OK));

		
		Collection<String> connectorSources = appMap.getConnectorSources( checkedFullServiceName );
		Collection<String> connectorTargets = appMap.getConnectorTargets( checkedFullServiceName );
		
		LogUtils.log( "Sources for service [" + checkedFullServiceName + "] are: " + 
				Arrays.toString( connectorSources.toArray( new String[connectorSources.size()] ) ) );
		LogUtils.log( "Targets for service [" + checkedFullServiceName + "] are: " + 
				Arrays.toString( connectorTargets.toArray( new String[connectorTargets.size()] ) ) );			
		
		assertEquals( "Number of [" + checkedFullServiceName + "] service sources must be one", 0, connectorSources.size() );
		assertEquals( "Number of [" + checkedFullServiceName + "] service targets must be one", 1, connectorTargets.size() );
		
		assertTrue( "Target of [" + checkedFullServiceName + "] service must be [" + checkedCassandraFullServiceName + "]", connectorTargets.contains(checkedCassandraFullServiceName) );
		
		//TODO CHANGE CONNECTORS TESTS		
		
/*		List<Connector> connectors = tomcat.getConnectors();
		AbstractTestSupport.assertTrue(connectors.size() == 1);
		List<Connector> targets = tomcat.getTargets();
		AbstractTestSupport.assertTrue(targets.size() == 1);
		AbstractTestSupport.assertTrue(targets.get(0).getTarget().getName().equals(cassandra.getName()));*/

		cassandra.select();

		HealthPanel healthPanel = topologyTab.getTopologySubPanel().switchToHealthPanel();

		tomcat.select();

		AbstractTestSupport.assertTrue("metric 0 is not displayed for tomcat recipe", healthPanel.getMetric(0) != null);
		AbstractTestSupport.assertTrue("metric 1 is not displayed for tomcat recipe", healthPanel.getMetric(1) != null);
		AbstractTestSupport.assertTrue("metric 2 is not displayed for tomcat recipe", healthPanel.getMetric(2) != null);
		AbstractTestSupport.assertTrue("metric 3 is not displayed for tomcat recipe", healthPanel.getMetric(3) != null);

		ServicesTab servicesTab = mainNav.switchToServices();

		uninstallApplication(TRAVEL_APPLICATION_NAME, true);
	}
}