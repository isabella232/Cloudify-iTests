package org.cloudifysource.quality.iTests.test.webui.recipes.applications;

import iTests.framework.utils.AssertUtils;
import iTests.framework.utils.AssertUtils.RepetitiveConditionProvider;
import iTests.framework.utils.LogUtils;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.gigaspaces.webuitf.LoginPage;
import com.gigaspaces.webuitf.topology.TopologyTab;
import com.gigaspaces.webuitf.topology.applicationmap.ApplicationMap;
import com.gigaspaces.webuitf.topology.applicationmap.ApplicationNode;

public class ApplicationBlueprintTest extends AbstractSeleniumApplicationRecipeTest {
	
	private static final String TRAVEL_APPLICATION_NAME = "travel";
	private static final String CASSANDRA_SERVICE_NAME = "cassandra";
	private static final String TOMCAT_SERVICE_NAME = "tomcat";
	
	private ApplicationNode cassandra;
	private ApplicationNode tomcat;
	
	@Override
	@BeforeMethod
	public void install() throws IOException, InterruptedException {
		setCurrentApplication(TRAVEL_APPLICATION_NAME);
		super.install();
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void blueprintTest() throws IOException, InterruptedException {
		
		LoginPage loginPage = getLoginPage();
		
		TopologyTab topologyTab = loginPage.login().switchToTopology();
		
		final ApplicationMap applicationMap = topologyTab.getApplicationMap();
		
		admin.getApplications().waitFor(TRAVEL_APPLICATION_NAME, waitingTime, TimeUnit.SECONDS);
		topologyTab.selectApplication(TRAVEL_APPLICATION_NAME);
		
		RepetitiveConditionProvider condition = new RepetitiveConditionProvider() {
			@Override
			public boolean getCondition() {
				cassandra = applicationMap.getApplicationNode( CASSANDRA_SERVICE_NAME );
				LogUtils.log( "Within condition, cassandra=" + cassandra );
				return cassandra != null;
			}
		};
		
		AssertUtils.repetitiveAssertTrue( "[" + CASSANDRA_SERVICE_NAME + "] must be displayed", condition, waitingTime );		
		
		final String checkedServiceName = TOMCAT_SERVICE_NAME;
		
		condition = new RepetitiveConditionProvider() {
			@Override
			public boolean getCondition() {
				tomcat = applicationMap.getApplicationNode( checkedServiceName );
				LogUtils.log( "Within condition, " + checkedServiceName + "=" + tomcat );
				return tomcat != null;
			}
		};
		
		AssertUtils.repetitiveAssertTrue( "[" + checkedServiceName + "] must be displayed", condition, waitingTime );		

		Collection<String> connectorSources = applicationMap.getConnectorSources( checkedServiceName );
		Collection<String> connectorTargets = applicationMap.getConnectorTargets( checkedServiceName );
		
		assertEquals( "Number of [" + checkedServiceName + "] service sources must be one", 1, connectorSources.size() );
		assertEquals( "Number of [" + checkedServiceName + "] service targets must be one", 1, connectorTargets.size() );
		
		assertTrue( "Target of [" + checkedServiceName + "] service must be [" + 
				CASSANDRA_SERVICE_NAME + "]", connectorTargets.contains(CASSANDRA_SERVICE_NAME) );
		
		//TODO CHANGE CONNECTORS TESTS		
		
/*		List<Connector> connectors = tomcat.getConnectors();
		assertTrue(connectors.size() == 1);
		List<Connector> targets = tomcat.getTargets();
		assertTrue(targets.size() == 1);
		assertTrue(targets.get(0).getTarget().getName().equals(cassandra.getName()));
*/		uninstallApplication(TRAVEL_APPLICATION_NAME, true);
	}
}
