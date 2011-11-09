package test.webui.recipes.applications;

import java.io.IOException;
import java.util.ArrayList;
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
import test.webui.objects.topology.ApplicationMap;
import test.webui.objects.topology.ApplicationMap.ApplicationNode;
import test.webui.objects.topology.ApplicationMap.ApplicationNode.Connector;
import test.webui.objects.topology.TopologyTab;
import test.webui.recipes.services.AbstractSeleniumServiceRecipeTest;
import framework.utils.AssertUtils;
import framework.utils.AssertUtils.RepetitiveConditionProvider;

public class StockDemoTest extends AbstractSeleniumApplicationRecipeTest {

	@Override
	@BeforeMethod
	public void bootstrapAndInstall() throws IOException, InterruptedException {
		setCurrentApplication("stockdemo");
		super.bootstrapAndInstall();
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 4)
	public void stockDemoTest() throws Exception {

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

		appMenuPanel.selectApplication("stockdemo");

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
				return ((applicationServices.getNoSqlDbModule().getCount() == 1)
						&& (applicationServices.getNoSqlDbModule().getIcon().equals(Icon.OK)));
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition, waitingTime);

		condition = new RepetitiveConditionProvider() {

			@Override
			public boolean getCondition() {
				return ((applicationServices.getMirrorModule().getCount() == 1)
						&& (applicationServices.getMirrorModule().getIcon().equals(Icon.OK)));
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition, waitingTime);

		condition = new RepetitiveConditionProvider() {

			@Override
			public boolean getCondition() {
				return ((applicationServices.getStatefullModule().getCount() == 4)
						&& (applicationServices.getStatefullModule().getIcon().equals(Icon.OK)));
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
		
		takeScreenShot(this.getClass(), "passed-dashboard");

		appMap.selectApplication("stockdemo");

		condition = new RepetitiveConditionProvider() {

			@Override
			public boolean getCondition() {
				ApplicationNode cassandraNode = appMap.getApplicationNode("cassandra");
				return ((cassandraNode != null) && (cassandraNode.getStatus().equals(DeploymentStatus.INTACT)));
			}
		};
		repetitiveAssertTrueWithScreenshot(null, condition, this.getClass(), "failed");
		
		List<Connector> cassandraConnectors = appMap.getApplicationNode("cassandra").getConnectors();
		assertTrue(cassandraConnectors.size() == 1);
		Connector connector = cassandraConnectors.get(0);
		assertTrue(connector.getSource().getName().equals("stockAnalyticsMirror"));

		
		condition = new RepetitiveConditionProvider() {

			@Override
			public boolean getCondition() {
				ApplicationNode stockAnalyticsMirrorNode = appMap.getApplicationNode("stockAnalyticsMirror");
				return ((stockAnalyticsMirrorNode != null) && (stockAnalyticsMirrorNode.getStatus().equals(DeploymentStatus.INTACT)));
			}
		};
		repetitiveAssertTrueWithScreenshot(null, condition, this.getClass(), "failed");
		
		List<Connector> stockAnalyticsMirrorConnectors = appMap.getApplicationNode("stockAnalyticsMirror").getConnectors();
		assertTrue(stockAnalyticsMirrorConnectors.size() == 2);
		for (Connector c : stockAnalyticsMirrorConnectors) {
			if (c.getSource().getName().equals("stockAnalyticsMirror")) {
				assertTrue(c.getTarget().getName().equals("cassandra"));
			}
			if (c.getTarget().getName().equals("stockAnalyticsMirror")) {
				assertTrue(c.getSource().getName().equals("stockAnalyticsSpace"));
				
			}
		}
		
		condition = new RepetitiveConditionProvider() {

			@Override
			public boolean getCondition() {
				ApplicationNode stockAnalyticsSpaceNode = appMap.getApplicationNode("stockAnalyticsSpace");
				return ((stockAnalyticsSpaceNode != null) && (stockAnalyticsSpaceNode.getStatus().equals(DeploymentStatus.INTACT)));
			}
		};
		repetitiveAssertTrueWithScreenshot(null, condition, this.getClass(), "failed");
		
		List<Connector> stockAnalyticsSpaceConnectors = appMap.getApplicationNode("stockAnalyticsSpace").getConnectors();
		assertTrue(stockAnalyticsSpaceConnectors.size() == 4);
		List<String> sourceNodeNames = new ArrayList<String>();
		for (Connector c : stockAnalyticsSpaceConnectors) {
			if (c.getSource().getName().equals("stockAnalyticsSpace")) {
				assertTrue(c.getTarget().getName().equals("stockAnalyticsMirror"));
			}
			if (c.getTarget().getName().equals("stockAnalyticsSpace")) {
				sourceNodeNames.add(c.getSource().getName());
			}
		}
		assertTrue(sourceNodeNames.size() == 3);
		assertTrue(sourceNodeNames.contains("stockAnalyticsProcessor"));
		assertTrue(sourceNodeNames.contains("StockDemo"));
		assertTrue(sourceNodeNames.contains("stockAnalytics"));
		
		condition = new RepetitiveConditionProvider() {

			@Override
			public boolean getCondition() {
				ApplicationNode stockAnalyticsProcessorNode = appMap.getApplicationNode("stockAnalyticsProcessor");
				return ((stockAnalyticsProcessorNode != null) && (stockAnalyticsProcessorNode.getStatus().equals(DeploymentStatus.INTACT)));
			}
		};
		repetitiveAssertTrueWithScreenshot(null, condition, this.getClass(), "failed");
		
		
		List<Connector> stockAnalyticsProcessorConnectors = appMap.getApplicationNode("stockAnalyticsProcessor").getConnectors();
		assertTrue(stockAnalyticsProcessorConnectors.size() == 3);
		sourceNodeNames = new ArrayList<String>();
		for (Connector c : stockAnalyticsSpaceConnectors) {
			if (c.getSource().getName().equals("stockAnalyticsProcessor")) {
				assertTrue(c.getTarget().getName().equals("stockAnalyticsSpace"));
			}
			if (c.getTarget().getName().equals("stockAnalyticsProcessor")) {
				sourceNodeNames.add(c.getSource().getName());
			}
		}
		assertTrue(sourceNodeNames.size() == 2);
		assertTrue(sourceNodeNames.contains("stockAnalyticsFeeder"));
		assertTrue(sourceNodeNames.contains("StockDemo"));
		
		condition = new RepetitiveConditionProvider() {

			@Override
			public boolean getCondition() {
				ApplicationNode StockDemoNode = appMap.getApplicationNode("StockDemo");
				return ((StockDemoNode != null) && (StockDemoNode.getStatus().equals(DeploymentStatus.INTACT)));
			}
		};
		repetitiveAssertTrueWithScreenshot(null, condition, this.getClass(), "failed");
		
		
		List<Connector> stockDemoConnectors = appMap.getApplicationNode("StockDemo").getConnectors();
		assertTrue(stockDemoConnectors.size() == 2);
		List<String> targetNodeNames = new ArrayList<String>();
		for (Connector c : stockAnalyticsSpaceConnectors) {
			if (c.getSource().getName().equals("StockDemo")) {
				targetNodeNames.add(c.getTarget().getName());
			}
		}
		assertTrue(targetNodeNames.size() == 2);
		assertTrue(targetNodeNames.contains("stockAnalyticsProcessor"));
		assertTrue(targetNodeNames.contains("stockAnalyticsSpace"));
		
		condition = new RepetitiveConditionProvider() {

			@Override
			public boolean getCondition() {
				ApplicationNode stockAnalyticsNode = appMap.getApplicationNode("stockAnalytics");
				return ((stockAnalyticsNode != null) && (stockAnalyticsNode.getStatus().equals(DeploymentStatus.INTACT)));
			}
		};
		repetitiveAssertTrueWithScreenshot(null, condition, this.getClass(), "failed");
		
		List<Connector> stockAnalyticsConnectors = appMap.getApplicationNode("stockAnalytics").getConnectors();
		assertTrue(stockAnalyticsConnectors.size() == 1);
		connector = cassandraConnectors.get(0);
		assertTrue(connector.getTarget().getName().equals("stockAnalyticsSpace"));
		
		condition = new RepetitiveConditionProvider() {

			@Override
			public boolean getCondition() {
				ApplicationNode stockAnalyticsFeederNode = appMap.getApplicationNode("stockAnalyticsFeeder");
				return ((stockAnalyticsFeederNode != null) && (stockAnalyticsFeederNode.getStatus().equals(DeploymentStatus.INTACT)));
			}
		};
		repetitiveAssertTrueWithScreenshot(null, condition, this.getClass(), "failed");
		
		List<Connector> stockAnalyticsFeederConnectors = appMap.getApplicationNode("stockAnalyticsFeeder").getConnectors();
		assertTrue(stockAnalyticsFeederConnectors.size() == 1);
		connector = cassandraConnectors.get(0);
		assertTrue(connector.getTarget().getName().equals("stockAnalyticsProcessor"));
		
		takeScreenShot(this.getClass(), "passed-topology");
		
		ServicesTab servicesTab = mainNav.switchToServices();
		
		PuTreeGrid puTreeGrid = servicesTab.getPuTreeGrid();
		
		assertTrue(puTreeGrid.getProcessingUnit("rest") != null);
		assertTrue(puTreeGrid.getProcessingUnit("webui") != null);
		assertTrue(puTreeGrid.getProcessingUnit("cassandra") != null);
		assertTrue(puTreeGrid.getProcessingUnit("stockAnalyticsMirror") != null);
		assertTrue(puTreeGrid.getProcessingUnit("stockAnalyticsSpace") != null);
		assertTrue(puTreeGrid.getProcessingUnit("stockAnalyticsProcessor") != null);
		assertTrue(puTreeGrid.getProcessingUnit("StockDemo") != null);
		assertTrue(puTreeGrid.getProcessingUnit("stockAnalytics") != null);
		assertTrue(puTreeGrid.getProcessingUnit("stockAnalyticsFeeder") != null);
		
		takeScreenShot(this.getClass(), "passed-services");

	}

}
