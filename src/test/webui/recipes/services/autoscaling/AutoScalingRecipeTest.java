package test.webui.recipes.services.autoscaling;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.utils.ServiceUtils;
import org.openspaces.admin.internal.pu.InternalProcessingUnit;
import org.openspaces.admin.pu.DeploymentStatus;
import org.openspaces.admin.pu.statistics.AverageInstancesStatisticsConfig;
import org.openspaces.admin.pu.statistics.LastSampleTimeWindowStatisticsConfig;
import org.openspaces.admin.pu.statistics.ProcessingUnitStatisticsId;
import org.openspaces.admin.pu.statistics.ProcessingUnitStatisticsIdConfigurer;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.cli.cloudify.CommandTestUtils;
import test.webui.objects.LoginPage;
import test.webui.objects.MainNavigation;
import test.webui.objects.dashboard.DashboardTab;
import test.webui.objects.dashboard.ServicesGrid;
import test.webui.objects.dashboard.ServicesGrid.ApplicationServicesGrid;
import test.webui.objects.dashboard.ServicesGrid.ApplicationsMenuPanel;
import test.webui.objects.dashboard.ServicesGrid.Icon;
import test.webui.objects.dashboard.ServicesGrid.InfrastructureServicesGrid;
import test.webui.objects.topology.TopologyTab;
import test.webui.objects.topology.applicationmap.ApplicationMap;
import test.webui.objects.topology.applicationmap.ApplicationNode;
import test.webui.objects.topology.healthpanel.HealthPanel;
import test.webui.recipes.services.AbstractSeleniumServiceRecipeTest;
import framework.utils.AssertUtils;
import framework.utils.AssertUtils.RepetitiveConditionProvider;
import framework.utils.LogUtils;

public class AutoScalingRecipeTest extends AbstractSeleniumServiceRecipeTest {

	private static final String COUNTER_METRIC = "counter";
	private static final String APPLICATION_NAME = "default";
	private static final String SERVICE_NAME = "customServiceMonitor";
	private static final String ABSOLUTE_SERVICE_NAME = ServiceUtils.getAbsolutePUName(APPLICATION_NAME,SERVICE_NAME);

	@Override
	@BeforeMethod
	public void install() throws IOException, InterruptedException {
		super.setPathToServiceRelativeToSGTestRootDir("apps\\cloudify\\recipes\\customServiceMonitor");
		super.install();
	}
	
	//@Test(timeOut = DEFAULT_TEST_TIMEOUT)
	public void customServiceMonitorsAutoScalingTest() throws InterruptedException, IOException {

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

		ServicesGrid servicesGrid = dashboardTab.getServicesGrid();

		ApplicationsMenuPanel appMenu = servicesGrid.getApplicationsMenuPanel();

		appMenu.selectApplication(MANAGEMENT);

		final ApplicationServicesGrid applicationServicesGrid = servicesGrid.getApplicationServicesGrid();

		condition = new RepetitiveConditionProvider() {		
			@Override
			public boolean getCondition() {
				return applicationServicesGrid.getWebModule().getCount() == 2;
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition, waitingTime);

		appMenu.selectApplication(APPLICATION_NAME);

		condition = new RepetitiveConditionProvider() {		
			@Override
			public boolean getCondition() {
				return applicationServicesGrid.getWebServerModule().getCount() == 1;
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition, waitingTime);

		TopologyTab topologyTab = mainNav.switchToTopology();

		final ApplicationMap appMap = topologyTab.getApplicationMap();

		appMap.selectApplication(APPLICATION_NAME);
		
		takeScreenShot(this.getClass(), "customServiceMonitorsAutoScalingTest","topology");

		final ApplicationNode simple = appMap.getApplicationNode(SERVICE_NAME);

		assertTrue(simple != null);
		
		condition = new RepetitiveConditionProvider() {
			
			@Override
			public boolean getCondition() {
				return simple.getStatus().equals(DeploymentStatus.INTACT);
			}
		};
		repetitiveAssertTrueWithScreenshot(
				"customServiceMonitor service is displayed as " + simple.getStatus() + 
					"even though it is installed", condition, this.getClass(), "customServiceMonitorsAutoScalingTest", SERVICE_NAME);

		HealthPanel healthPanel = topologyTab.getTopologySubPanel().switchToHealthPanel();

		takeScreenShot(this.getClass(),"customServiceMonitorsAutoScalingTest", "topology-healthpanel");
		
		assertNotNull("counter " + METRICS_ASSERTION_SUFFIX , healthPanel.getMetric(COUNTER_METRIC));
		
		final InternalProcessingUnit pu = (InternalProcessingUnit) admin.getProcessingUnits().waitFor(ABSOLUTE_SERVICE_NAME,OPERATION_TIMEOUT,TimeUnit.MILLISECONDS);
		final ProcessingUnitStatisticsId statisticsId = 
				new ProcessingUnitStatisticsIdConfigurer()
				.monitor(CloudifyConstants.USM_MONITORS_SERVICE_ID)
				.metric(COUNTER_METRIC)
				.instancesStatistics(new AverageInstancesStatisticsConfig())
				.timeWindowStatistics(new LastSampleTimeWindowStatisticsConfig())
				.create();
		pu.addStatisticsCalculation(statisticsId);
		pu.setStatisticsInterval(1, TimeUnit.SECONDS);
		pu.startStatisticsMonitor();
		repetitiveAssertStatistics(pu, statisticsId, 0.0);
		
		setStatistics(1);
		repetitiveAssertStatistics(pu, statisticsId, 1.0);
		
		//TODO: check expected autoscaling based on recipe auto scaling rules
	}

	private void repetitiveAssertStatistics(final InternalProcessingUnit pu,
			final ProcessingUnitStatisticsId statisticsId,
			final Double expectedResult) {
		repetitiveAssertTrue("Failed waiting for counter to be "+ expectedResult, new RepetitiveConditionProvider() {
			
			@Override
			public boolean getCondition() {
				
				final Object counter = pu.getStatistics().getStatistics().get(statisticsId);
				if (counter == null) {
					LogUtils.log("Cannot get statistics " + statisticsId);
				}
				else if (!(counter instanceof Double)) {
					LogUtils.log("Cannot get Double from statistics " + statisticsId);
				} else {
				
					if (((Double)counter) != expectedResult) {
						LogUtils.log("Waiting for value of average(counter), to be " + expectedResult + " current value is " + counter);
					}
				}
				return expectedResult.equals(counter);
			}
		}, OPERATION_TIMEOUT);
	}
	
	public void setStatistics(long value) throws IOException, InterruptedException {
		String command = "connect localhost;invoke --verbose " + SERVICE_NAME + " set " + value;
		CommandTestUtils.runCommandAndWait(command);
	}


}
