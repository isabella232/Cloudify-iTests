package test.webui.recipes.applications;

import java.io.IOException;
import java.util.List;

import org.cloudifysource.dsl.utils.ServiceUtils;
import org.openspaces.admin.pu.DeploymentStatus;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.gigaspaces.webuitf.LoginPage;
import com.gigaspaces.webuitf.topology.TopologyTab;
import com.gigaspaces.webuitf.topology.applicationmap.ApplicationMap;
import com.gigaspaces.webuitf.topology.applicationmap.ApplicationNode;
import com.gigaspaces.webuitf.topology.applicationmap.Connector;

public class UninstallApplicationTest extends AbstractSeleniumApplicationRecipeTest {

	private static final String TRAVEL_APPLICATION_NAME = "travel";
	private static final String CASSANDRA_SERVICE_FULL_NAME = ServiceUtils.getAbsolutePUName(TRAVEL_APPLICATION_NAME, "cassandra");
	private static final String TOMCAT_SERVICE_FULL_NAME = ServiceUtils.getAbsolutePUName(TRAVEL_APPLICATION_NAME, "tomcat");

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
		
		uninstallApplication(TRAVEL_APPLICATION_NAME, true);
		
		cassandra = appMap.getApplicationNode(CASSANDRA_SERVICE_FULL_NAME);
		assertTrue("cassandra node is still displayed even though the application was uninstalled", cassandra == null);
		
		tomcat = appMap.getApplicationNode(TOMCAT_SERVICE_FULL_NAME);
		assertTrue("tomcat node is still displayed even though the application was uninstalled", tomcat == null);
		
		topologyTab.selectApplication(MANAGEMENT_APPLICATION_NAME);
		
		cassandra = appMap.getApplicationNode(CASSANDRA_SERVICE_FULL_NAME);
		assertTrue("cassandra node is disaplyed in the management application even though the application was uninstalled", cassandra == null);
		
		tomcat = appMap.getApplicationNode(TOMCAT_SERVICE_FULL_NAME);
		assertTrue("tomcat node is disaplyed in the management application even though the application was uninstalled", tomcat == null);
		
	}

}
