package test.webui.recipes.applications;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.cloudifysource.dsl.utils.ServiceUtils;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.gigaspaces.webuitf.LoginPage;
import com.gigaspaces.webuitf.topology.TopologyTab;
import com.gigaspaces.webuitf.topology.applicationmap.ApplicationMap;
import com.gigaspaces.webuitf.topology.applicationmap.ApplicationNode;
import com.gigaspaces.webuitf.topology.applicationmap.Connector;

public class ApplicationBlueprintTest extends AbstractSeleniumApplicationRecipeTest {
	
	private static final String TRAVEL_APPLICATION_NAME = "travel";
	private static final String CASSANDRA_SERVICE_FULL_NAME = ServiceUtils.getAbsolutePUName(TRAVEL_APPLICATION_NAME, "cassandra");
	private static final String TOMCAT_SERVICE_FULL_NAME = ServiceUtils.getAbsolutePUName(TRAVEL_APPLICATION_NAME, "tomcat");
	
	@Override
	@BeforeMethod
	public void install() throws IOException, InterruptedException {
		setCurrentApplication(TRAVEL_APPLICATION_NAME);
		setWait(false);
		super.install();
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void blueprintTest() throws IOException, InterruptedException {
		
		LoginPage loginPage = getLoginPage();
		
		TopologyTab topologyTab = loginPage.login().switchToTopology();
		
		ApplicationMap applicationMap = topologyTab.getApplicationMap();
		
		admin.getApplications().waitFor(TRAVEL_APPLICATION_NAME, waitingTime, TimeUnit.SECONDS);
		topologyTab.selectApplication(TRAVEL_APPLICATION_NAME);
		
		ApplicationNode cassandra = applicationMap.getApplicationNode(CASSANDRA_SERVICE_FULL_NAME);

		assertTrue(cassandra != null);

		ApplicationNode tomcat = applicationMap.getApplicationNode(TOMCAT_SERVICE_FULL_NAME);

		assertTrue(tomcat != null);	

		List<Connector> connectors = tomcat.getConnectors();
		assertTrue(connectors.size() == 1);
		List<Connector> targets = tomcat.getTargets();
		assertTrue(targets.size() == 1);
		assertTrue(targets.get(0).getTarget().getName().equals(cassandra.getName()));
		uninstallApplication(TRAVEL_APPLICATION_NAME, true);
	}
}
