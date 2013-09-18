package org.cloudifysource.quality.iTests.test.webui.recipes.applications;

import iTests.framework.utils.AssertUtils;
import iTests.framework.utils.AssertUtils.RepetitiveConditionProvider;
import iTests.framework.utils.LogUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import org.cloudifysource.dsl.utils.ServiceUtils;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.gigaspaces.webuitf.LoginPage;
import com.gigaspaces.webuitf.topology.TopologyTab;
import com.gigaspaces.webuitf.topology.applicationmap.ApplicationMap;
import com.gigaspaces.webuitf.topology.applicationmap.ApplicationNode;

public class ApplicationBlueprintTest extends AbstractSeleniumApplicationRecipeTest {
	
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
				cassandra = applicationMap.getApplicationNode( DEFAULT_CASSANDRA_SERVICE_NAME );
				LogUtils.log( "Within condition, cassandra=" + cassandra );
				return cassandra != null;
			}
		};
		
		AssertUtils.repetitiveAssertTrue( "[" + DEFAULT_CASSANDRA_SERVICE_NAME + "] must be displayed", condition, waitingTime );		
		
		final String checkedServiceName = DEFAULT_TOMCAT_SERVICE_NAME;
		final String checkedServiceFullName = 
				ServiceUtils.getAbsolutePUName(TRAVEL_APPLICATION_NAME,  DEFAULT_TOMCAT_SERVICE_NAME);
		
		condition = new RepetitiveConditionProvider() {
			@Override
			public boolean getCondition() {
				tomcat = applicationMap.getApplicationNode( checkedServiceName );
				LogUtils.log( "Within condition, " + checkedServiceName + "=" + tomcat );
				return tomcat != null;
			}
		};
		
		AssertUtils.repetitiveAssertTrue( "[" + checkedServiceName + "] must be displayed", condition, waitingTime );		

		Collection<String> connectorSources = applicationMap.getConnectorSources( checkedServiceFullName );
		Collection<String> connectorTargets = applicationMap.getConnectorTargets( checkedServiceFullName );
		
		assertEquals( "Number of [" + checkedServiceFullName + "] service sources must be one", 0, connectorSources.size() );
		assertEquals( "Number of [" + checkedServiceFullName + "] service targets must be one", 1, connectorTargets.size() );
		
		LogUtils.log( "Sources for service [" + checkedServiceName + "] are: " + 
				Arrays.toString( connectorSources.toArray( new String[connectorSources.size()] ) ) );
		LogUtils.log( "Targets for service [" + checkedServiceName + "] are: " + 
				Arrays.toString( connectorTargets.toArray( new String[connectorTargets.size()] ) ) );
		
		String checkedCassandraFullServiceName = ServiceUtils.getAbsolutePUName(TRAVEL_APPLICATION_NAME, DEFAULT_CASSANDRA_SERVICE_NAME );
		
		assertTrue( "Target of [" + checkedServiceFullName + "] service must be [" + 
				checkedCassandraFullServiceName + "]", connectorTargets.contains(checkedCassandraFullServiceName) );
		
		//TODO CHANGE CONNECTORS TESTS		
		
/*		List<Connector> connectors = tomcat.getConnectors();
		assertTrue(connectors.size() == 1);
		List<Connector> targets = tomcat.getTargets();
		assertTrue(targets.size() == 1);
		assertTrue(targets.get(0).getTarget().getName().equals(cassandra.getName()));
*/		uninstallApplication(TRAVEL_APPLICATION_NAME, true);
	}
}
