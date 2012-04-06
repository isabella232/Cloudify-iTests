package test.webui.recipes.applications;

import java.io.IOException;
import java.util.List;

import org.openspaces.admin.pu.DeploymentStatus;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.webui.objects.LoginPage;
import test.webui.objects.topology.TopologyTab;
import test.webui.objects.topology.applicationmap.ApplicationMap;
import test.webui.objects.topology.applicationmap.ApplicationNode;
import test.webui.objects.topology.applicationmap.Connector;

public class UninstallApplicationTest extends AbstractSeleniumApplicationRecipeTest {
	
	@Override
	@BeforeMethod
	public void install() throws IOException, InterruptedException {
		setCurrentApplication("travel");
		super.install();
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, enabled = true)
	public void uninstallApplicationTest() throws InterruptedException, IOException {
		
		// get new login page
		LoginPage loginPage = getLoginPage();

		TopologyTab topologyTab = loginPage.login().switchToTopology();
		
		final ApplicationMap appMap = topologyTab.getApplicationMap();
		
		appMap.selectApplication("travel");
		
		ApplicationNode cassandra = appMap.getApplicationNode("cassandra");

		assertTrue(cassandra != null);
		assertTrue(cassandra.getStatus().equals(DeploymentStatus.INTACT));	

		ApplicationNode tomcat = appMap.getApplicationNode("tomcat");

		assertTrue(tomcat != null);
		assertTrue(tomcat.getStatus().equals(DeploymentStatus.INTACT));		

		List<Connector> connectors = tomcat.getConnectors();
		assertTrue(connectors.size() == 1);
		List<Connector> targets = tomcat.getTargets();
		assertTrue(targets.size() == 1);
		assertTrue(targets.get(0).getTarget().getName().equals(cassandra.getName()));
		
		uninstallApplication("travel", true);
		
		cassandra = appMap.getApplicationNode("cassandra");
		assertTrue(cassandra == null);
		
		tomcat = appMap.getApplicationNode("tomcat");
		assertTrue(tomcat == null);
		
		appMap.selectApplication(AbstractSeleniumApplicationRecipeTest.MANAGEMENT);
		
		cassandra = appMap.getApplicationNode("cassandra");
		assertTrue(cassandra == null);
		
		tomcat = appMap.getApplicationNode("tomcat");
		assertTrue(tomcat == null);
		
	}

}
