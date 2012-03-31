package test.webui.recipes.applications;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.webui.objects.LoginPage;
import test.webui.objects.topology.TopologyTab;
import test.webui.objects.topology.applicationmap.ApplicationMap;
import test.webui.objects.topology.applicationmap.ApplicationNode;
import test.webui.objects.topology.applicationmap.Connector;

public class ApplicationBlueprintTest extends AbstractSeleniumApplicationRecipeTest {
	
	@Override
	@BeforeMethod
	public void install() throws IOException, InterruptedException {
		setCurrentApplication("travel");
		setWait(false);
		super.install();
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void blueprintTest() throws IOException, InterruptedException {
		
		LoginPage loginPage = getLoginPage();
		
		TopologyTab topologyTab = loginPage.login().switchToTopology();
		
		ApplicationMap applicationMap = topologyTab.getApplicationMap();
		
		admin.getApplications().waitFor("travel", waitingTime, TimeUnit.SECONDS);
		applicationMap.selectApplication("travel");
		
		ApplicationNode cassandra = applicationMap.getApplicationNode("cassandra");

		assertTrue(cassandra != null);
//		DeploymentStatus cassandraStatus = cassandra.getStatus();
//		assertTrue("cassandra status is" + cassandraStatus, 
//				cassandraStatus.equals(DeploymentStatus.SCHEDULED)
//				|| cassandraStatus.equals(DeploymentStatus.INTACT));	

		ApplicationNode tomcat = applicationMap.getApplicationNode("tomcat");

		assertTrue(tomcat != null);
//		DeploymentStatus tomcatStatus = tomcat.getStatus();
//		assertTrue("tomcat status is " + tomcatStatus,
//				tomcatStatus.equals(DeploymentStatus.SCHEDULED)
//				|| tomcatStatus.equals(DeploymentStatus.INTACT));		

		List<Connector> connectors = tomcat.getConnectors();
		assertTrue(connectors.size() == 1);
		List<Connector> targets = tomcat.getTargets();
		assertTrue(targets.size() == 1);
		assertTrue(targets.get(0).getTarget().getName().equals(cassandra.getName()));
	}
}
