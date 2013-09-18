package org.cloudifysource.quality.iTests.test.webui.recipes.applications;

import iTests.framework.utils.AssertUtils;
import iTests.framework.utils.AssertUtils.RepetitiveConditionProvider;
import iTests.framework.utils.LogUtils;

import java.io.IOException;
import java.util.Collection;

import org.cloudifysource.dsl.utils.ServiceUtils;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.gigaspaces.webuitf.LoginPage;
import com.gigaspaces.webuitf.topology.TopologyTab;
import com.gigaspaces.webuitf.topology.applicationmap.ApplicationMap;
import com.gigaspaces.webuitf.topology.applicationmap.ApplicationNode;

public class UninstallApplicationTest extends AbstractSeleniumApplicationRecipeTest {

	private ApplicationNode cassandra;
	private ApplicationNode tomcat;

	@Override
	@BeforeMethod
	public void install() throws IOException, InterruptedException {
		setCurrentApplication(TRAVEL_APPLICATION_NAME);
		super.install();
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, enabled = true)
	public void uninstallApplicationTest() throws InterruptedException, IOException {
		
		// get new login page
		LoginPage loginPage = getLoginPage();

		TopologyTab topologyTab = loginPage.login().switchToTopology();
		
		final ApplicationMap appMap = topologyTab.getApplicationMap();
		
		topologyTab.selectApplication(TRAVEL_APPLICATION_NAME);
		
//		ApplicationNode cassandra = appMap.getApplicationNode(CASSANDRA_SERVICE_NAME);
		RepetitiveConditionProvider condition = new RepetitiveConditionProvider() {
			@Override
			public boolean getCondition() {
				cassandra = appMap.getApplicationNode( DEFAULT_CASSANDRA_SERVICE_NAME );
				LogUtils.log( "Within condition, cassandra=" + cassandra );
				return cassandra != null;
			}
		};
		
		AssertUtils.repetitiveAssertTrue( "[" + DEFAULT_CASSANDRA_SERVICE_NAME + "] must be displayed", condition, waitingTime );	
		
		
		condition = new RepetitiveConditionProvider() {
			@Override
			public boolean getCondition() {
				
				String status = appMap.getApplicationNodeStatus( DEFAULT_CASSANDRA_SERVICE_NAME );
				LogUtils.log( "Within condition, current status is [" + status + "]");
				return status.equals(ApplicationMap.CONN_STATUS_OK);
			}
		};
		
		AssertUtils.repetitiveAssertTrue( "Status of [" + DEFAULT_CASSANDRA_SERVICE_NAME + 
				"] service must be [" + ApplicationMap.CONN_STATUS_OK + "]" , condition, waitingTime );		

//		ApplicationNode tomcat = appMap.getApplicationNode(TOMCAT_SERVICE_NAME);

		condition = new RepetitiveConditionProvider() {
			@Override
			public boolean getCondition() {
				tomcat = appMap.getApplicationNode( DEFAULT_TOMCAT_SERVICE_NAME );
				LogUtils.log( "Within condition, tomcat=" + tomcat );
				return tomcat != null;
			}
		};
		
		AssertUtils.repetitiveAssertTrue( "[" + DEFAULT_TOMCAT_SERVICE_NAME + "] must be displayed", condition, waitingTime );			
		
		condition = new RepetitiveConditionProvider() {
			@Override
			public boolean getCondition() {
				String status = appMap.getApplicationNodeStatus( DEFAULT_TOMCAT_SERVICE_NAME );
				LogUtils.log( "Within condition, current status is [" + status + "]");
				return status.equals(ApplicationMap.CONN_STATUS_OK);
			}
		};
		
		AssertUtils.repetitiveAssertTrue( "Status of [" + DEFAULT_TOMCAT_SERVICE_NAME + 
				"] service must be [" + ApplicationMap.CONN_STATUS_OK + "]" , condition, waitingTime );			

		String checkedFullServiceName =  ServiceUtils.getAbsolutePUName(TRAVEL_APPLICATION_NAME, DEFAULT_CASSANDRA_SERVICE_NAME);
		Collection<String> connectorSources = appMap.getConnectorSources( checkedFullServiceName );
		Collection<String> connectorTargets = appMap.getConnectorTargets( checkedFullServiceName );
		
		assertEquals( "Number of [" + checkedFullServiceName + "] service sources must be one", 1, connectorSources.size() );
		assertEquals( "Number of [" + checkedFullServiceName + "] service targets must be one", 0, connectorTargets.size() );
		
		String checkedTomcatFullServiceName = ServiceUtils.getAbsolutePUName(TRAVEL_APPLICATION_NAME, DEFAULT_TOMCAT_SERVICE_NAME);
		assertTrue( "Source of [" + checkedFullServiceName + "] service must be [" + checkedTomcatFullServiceName + "]", connectorSources.contains(checkedTomcatFullServiceName) );
		
		uninstallApplication(TRAVEL_APPLICATION_NAME, true);
		
		
		condition = new RepetitiveConditionProvider() {
			@Override
			public boolean getCondition() {
				cassandra = appMap.getApplicationNode( DEFAULT_CASSANDRA_SERVICE_NAME );
				LogUtils.log( "Within condition, cassandra=" + cassandra );
				return cassandra == null;
			}
		};
		AssertUtils.repetitiveAssertTrue( "[" + DEFAULT_CASSANDRA_SERVICE_NAME + "] node is still displayed even though the application was uninstalled", condition, waitingTime );			
		
//		tomcat = appMap.getApplicationNode(TOMCAT_SERVICE_NAME);
		condition = new RepetitiveConditionProvider() {
			@Override
			public boolean getCondition() {
				tomcat = appMap.getApplicationNode( DEFAULT_TOMCAT_SERVICE_NAME );
				LogUtils.log( "Within condition, tomcat=" + tomcat );
				return tomcat == null;
			}
		};		
		AssertUtils.repetitiveAssertTrue( "[" + DEFAULT_TOMCAT_SERVICE_NAME + "] node is still displayed even though the application was uninstalled", condition, waitingTime );
		
		topologyTab.selectApplication(MANAGEMENT_APPLICATION_NAME);
		
		cassandra = appMap.getApplicationNode(DEFAULT_CASSANDRA_SERVICE_NAME);
		assertTrue("cassandra node is disaplyed in the management application even though the application was uninstalled", cassandra == null);
		
		tomcat = appMap.getApplicationNode(DEFAULT_TOMCAT_SERVICE_NAME);
		assertTrue("tomcat node is disaplyed in the management application even though the application was uninstalled", tomcat == null);
		
	}

}
