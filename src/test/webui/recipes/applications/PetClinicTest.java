package test.webui.recipes.applications;

import java.io.IOException;
import java.util.List;

import org.openspaces.admin.pu.DeploymentStatus;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.webui.objects.LoginPage;
import test.webui.objects.MainNavigation;
import test.webui.objects.dashboard.DashboardTab;
import test.webui.objects.dashboard.ServicesGrid.ApplicationServicesGrid;
import test.webui.objects.dashboard.ServicesGrid.ApplicationsMenuPanel;
import test.webui.objects.dashboard.ServicesGrid.Icon;
import test.webui.objects.dashboard.ServicesGrid.InfrastructureServicesGrid;
import test.webui.objects.services.PuTreeGrid;
import test.webui.objects.services.ServicesTab;
import test.webui.objects.topology.TopologyTab;
import test.webui.objects.topology.applicationmap.ApplicationMap;
import test.webui.objects.topology.applicationmap.ApplicationNode;
import test.webui.objects.topology.applicationmap.Connector;
import test.webui.recipes.services.AbstractSeleniumServiceRecipeTest;
import framework.utils.AssertUtils;
import framework.utils.AssertUtils.RepetitiveConditionProvider;

public class PetClinicTest extends AbstractSeleniumApplicationRecipeTest {

	@Override
	@BeforeMethod
	public void bootstrapAndInstall() throws IOException, InterruptedException {
		setCurrentApplication("petclinic");
		super.bootstrapAndInstall();
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 4)
	public void petClinicDemoTest() throws Exception {
		
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

		ApplicationsMenuPanel appMenuPanel = dashboardTab.getServicesGrid().getApplicationsMenuPanel();

		appMenuPanel.selectApplication(AbstractSeleniumServiceRecipeTest.MANAGEMENT);

		final ApplicationServicesGrid applicationServices = dashboardTab.getServicesGrid().getApplicationServicesGrid();

		condition = new RepetitiveConditionProvider() {

			@Override
			public boolean getCondition() {
				return ((applicationServices.getWebModule().getCount() == 2)
						&& (applicationServices.getWebModule().getIcon().equals(Icon.OK)));
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition, waitingTime);
		
		appMenuPanel.selectApplication("petclinic-mongo");
		
		condition = new RepetitiveConditionProvider() {

			@Override
			public boolean getCondition() {
				return ((applicationServices.getWebModule().getCount() == 1)
						&& (applicationServices.getWebModule().getIcon().equals(Icon.OK)));
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition, waitingTime);
		
		condition = new RepetitiveConditionProvider() {

			@Override
			public boolean getCondition() {
				return ((applicationServices.getNoSqlDbModule().getCount() == 3)
						&& (applicationServices.getNoSqlDbModule().getIcon().equals(Icon.OK)));
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition, waitingTime);
		
		TopologyTab topologyTab = mainNav.switchToTopology();

		final ApplicationMap appMap = topologyTab.getApplicationMap();

		appMap.selectApplication(AbstractSeleniumServiceRecipeTest.MANAGEMENT);

		takeScreenShot(this.getClass(), "management-application");

		condition = new RepetitiveConditionProvider() {

			@Override
			public boolean getCondition() {
				ApplicationNode restNode = appMap.getApplicationNode("rest");
				return ((restNode != null) && (restNode.getStatus().equals(DeploymentStatus.INTACT)));
			}
		};
		repetitiveAssertTrueWithScreenshot(null, condition, this.getClass(), "failed");

		condition = new RepetitiveConditionProvider() {

			@Override
			public boolean getCondition() {
				ApplicationNode webuiNode = appMap.getApplicationNode("webui");
				return ((webuiNode != null) && (webuiNode.getStatus().equals(DeploymentStatus.INTACT)));
			}
		};
		repetitiveAssertTrueWithScreenshot(null, condition, this.getClass(), "failed");
		
		takeScreenShot(this.getClass(), "passed-topology");
		
		appMap.selectApplication("petclinic-mongo");
		
		condition = new RepetitiveConditionProvider() {

			@Override
			public boolean getCondition() {
				ApplicationNode mongodNode = appMap.getApplicationNode("petclinic-mongo.mongod");
				return ((mongodNode != null) && (mongodNode.getStatus().equals(DeploymentStatus.INTACT)));
			}
		};
		repetitiveAssertTrueWithScreenshot(null, condition, this.getClass(), "failed");
		
		condition = new RepetitiveConditionProvider() {

			@Override
			public boolean getCondition() {
				ApplicationNode mongosNode = appMap.getApplicationNode("petclinic-mongo.mongos");
				return ((mongosNode != null) && (mongosNode.getStatus().equals(DeploymentStatus.INTACT)));
			}
		};
		repetitiveAssertTrueWithScreenshot(null, condition, this.getClass(), "failed");
		
		condition = new RepetitiveConditionProvider() {

			@Override
			public boolean getCondition() {
				ApplicationNode mongocfgNode = appMap.getApplicationNode("petclinic-mongo.mongo-cfg");
				return ((mongocfgNode != null) && (mongocfgNode.getStatus().equals(DeploymentStatus.INTACT)));
			}
		};
		repetitiveAssertTrueWithScreenshot(null, condition, this.getClass(), "failed");	
		
		condition = new RepetitiveConditionProvider() {

			@Override
			public boolean getCondition() {
				ApplicationNode tomcatNode = appMap.getApplicationNode("petclinic-mongo.tomcat");
				return ((tomcatNode != null) && (tomcatNode.getStatus().equals(DeploymentStatus.INTACT)));
			}
		};
		repetitiveAssertTrueWithScreenshot(null, condition, this.getClass(), "failed");
		
		
		List<Connector> tomcatConnectors = appMap.getApplicationNode("petclinic-mongo.tomcat").getConnectors();
		assertTrue(tomcatConnectors.size() == 1);
		assertTrue(tomcatConnectors.get(0).getTarget().getName().equals("petclinic-mongo.mongos"));

		List<Connector> mongosConnectors = appMap.getApplicationNode("petclinic-mongo.mongos").getConnectors();
		assertTrue(mongosConnectors.size() == 2);
		for (Connector c : mongosConnectors) {
			String name = c.getTarget().getName();
			assertTrue(name.equals("petclinic-mongo.mongod") ||name.equals("petclinic-mongo.mongo-cfg"));
		}

		ServicesTab servicesTab = mainNav.switchToServices();
		
		PuTreeGrid puTreeGrid = servicesTab.getPuTreeGrid();
		
		assertTrue(puTreeGrid.getProcessingUnit("petclinic-mongo.tomcat") != null);
		assertTrue(puTreeGrid.getProcessingUnit("petclinic-mongo.mongod") != null);
		assertTrue(puTreeGrid.getProcessingUnit("petclinic-mongo.mongos") != null);
		assertTrue(puTreeGrid.getProcessingUnit("petclinic-mongo.mongo-cfg") != null);
		
		takeScreenShot(this.getClass(), "passed-services");
	}

}
