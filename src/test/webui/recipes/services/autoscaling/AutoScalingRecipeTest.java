package test.webui.recipes.services.autoscaling;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.utils.ServiceUtils;
import org.openspaces.admin.internal.pu.InternalProcessingUnit;
import org.openspaces.admin.pu.DeploymentStatus;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.openspaces.admin.pu.statistics.AverageInstancesStatisticsConfig;
import org.openspaces.admin.pu.statistics.AverageTimeWindowStatisticsConfigurer;
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
import framework.utils.GridServiceContainersCounter;
import framework.utils.LogUtils;

public class AutoScalingRecipeTest extends AbstractSeleniumServiceRecipeTest {

	private static final String SERVICE_NAME = "customServiceMonitor";
	private static final String SERVICE_RELATIVE_PATH = "apps\\cloudify\\recipes\\" + SERVICE_NAME;
	
	private static final String COUNTER_METRIC = "counter";
	private static final String APPLICATION_NAME = "default";
	private static final String ABSOLUTE_SERVICE_NAME = ServiceUtils.getAbsolutePUName(APPLICATION_NAME,SERVICE_NAME);
	
	@Override
	@BeforeMethod
	public void install() throws IOException, InterruptedException {
		super.setPathToServiceRelativeToSGTestRootDir(SERVICE_NAME, SERVICE_RELATIVE_PATH);
		super.install();
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT)
	public void customServiceMonitorsAutoScalingTest() throws InterruptedException, IOException {

		// get new login page
		LoginPage loginPage = getLoginPage();

		MainNavigation mainNav = loginPage.login();

		DashboardTab dashboardTab = mainNav.switchToDashboard();
		
		final InfrastructureServicesGrid infrastructureServicesGrid = dashboardTab.getServicesGrid().getInfrastructureGrid();
		{
		RepetitiveConditionProvider condition = new RepetitiveConditionProvider() {
			
			@Override
			public boolean getCondition() {
				return ((infrastructureServicesGrid.getESMInst().getCount() == 1) 
						&& (infrastructureServicesGrid.getESMInst().getIcon().equals(Icon.OK)));
			}
		};
		AssertUtils.repetitiveAssertTrue("No esm in showing in the dashboard", condition, waitingTime);
		}
		ServicesGrid servicesGrid = dashboardTab.getServicesGrid();

		ApplicationsMenuPanel appMenu = servicesGrid.getApplicationsMenuPanel();

		appMenu.selectApplication(MANAGEMENT);

		final ApplicationServicesGrid applicationServicesGrid = servicesGrid.getApplicationServicesGrid();
		repetitiveAssertTwoWebModules(applicationServicesGrid);
				
		appMenu.selectApplication(APPLICATION_NAME);

		repetitiveAssertOneWebServerModule(applicationServicesGrid);
				
		TopologyTab topologyTab = mainNav.switchToTopology();

		final ApplicationMap appMap = topologyTab.getApplicationMap();

		appMap.selectApplication(APPLICATION_NAME);
		
		takeScreenShot(this.getClass(), "customServiceMonitorsAutoScalingTest","topology");

		final ApplicationNode simple = appMap.getApplicationNode(SERVICE_NAME);

		assertTrue(simple != null);
		
		repetitiveAssertApplicationNodeIntact(simple);
		
		HealthPanel healthPanel = topologyTab.getTopologySubPanel().switchToHealthPanel();

		takeScreenShot(this.getClass(),"customServiceMonitorsAutoScalingTest", "topology-healthpanel");
		
		assertNotNull("counter " + METRICS_ASSERTION_SUFFIX , healthPanel.getMetric(COUNTER_METRIC));
		
		final InternalProcessingUnit pu = (InternalProcessingUnit) admin.getProcessingUnits().waitFor(ABSOLUTE_SERVICE_NAME,OPERATION_TIMEOUT,TimeUnit.MILLISECONDS);
		final GridServiceContainersCounter gscCounter = new GridServiceContainersCounter(pu);
		try {
			pu.waitFor(2,OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
			ProcessingUnitInstance instance = pu.getInstances()[0];
			assertNotNull(instance);
			final ProcessingUnitStatisticsId averageStatisticsId = 
					new ProcessingUnitStatisticsIdConfigurer()
					.monitor(CloudifyConstants.USM_MONITORS_SERVICE_ID)
					.metric(COUNTER_METRIC)
					.instancesStatistics(new AverageInstancesStatisticsConfig())
					.timeWindowStatistics(new AverageTimeWindowStatisticsConfigurer().timeWindow(5, TimeUnit.SECONDS).create())
					.create();
			pu.addStatisticsCalculation(averageStatisticsId);
			pu.setStatisticsInterval(1, TimeUnit.SECONDS);
			pu.startStatisticsMonitor();
			try {
				repetitiveAssertStatistics(pu, averageStatisticsId, 0);
				
				//automatic scale out test
				//set metric value of 2 instances to 100
				setStatistics(100);

				// the high threshold is 90, and the average value is set to 100.0 - which means auto scale out (from 2 instances to 3 instances)
				// then the average value would be (100+100+0)/3 = 66 (two instance are 100 and the third one is still 0) which is within thresholds and should stop the scale out process
				// the maximum #instances is 4, so we do not expect 4 GSCs added at any point.
				repetitiveAssertNumberOfContainersAddedAndRemoved(gscCounter,/*expectedAdded=*/3 , /*expectedRemoved=*/0);
				repetitiveAssertStatistics(pu, averageStatisticsId, 100.0*2/3);
				
				// set metric value of 3 instances to 1000
				// the high threshold is 90, and the average value is set to 100.0 - which means auto scale out (from 3 instances to 4 instances)
				// then the average value would be (1000+1000+1000+0)/4 = 660 (three instance are 1000 and the fourth one is still 0)
				// the maximum #instances is 4, so we expect at most 4 GSCs added at any point.
				setStatistics(1000);
				repetitiveAssertNumberOfContainersAddedAndRemoved(gscCounter,/*expectedAdded=*/4 , /*expectedRemoved=*/0);
				repetitiveAssertStatistics(pu, averageStatisticsId, 1000.0*3/4);
				
				// the low threshold is 30, and the average value is set to 0.0 - which means auto scale in (from 3 instances to 2 instances)
				// the minimum #instances is 2, so we do not expect less than 2 GSCs added at any point.
				setStatistics(0);
				repetitiveAssertNumberOfContainersAddedAndRemoved(gscCounter,/*expectedAdded=*/4 , /*expectedRemoved=*/2);
				repetitiveAssertStatistics(pu, averageStatisticsId, 0);			
			}
			finally {
				pu.stopStatisticsMonitor();
			}
		}
		finally {
			gscCounter.close();		
		}
	}

	private void repetitiveAssertNumberOfContainersAddedAndRemoved(
			final GridServiceContainersCounter gscCounter,
			final int expectedAdded, final int expectedRemoved) {
		final RepetitiveConditionProvider condition = new RepetitiveConditionProvider() {
			
			@Override
			public boolean getCondition() {
				final int added = gscCounter.getNumberOfGSCsAdded();
				final int removed = gscCounter.getNumberOfGSCsRemoved();
				if (added != expectedAdded) {
					LogUtils.log("Expected " + expectedAdded + " added containers, actual added containers is " + added);
				}
				if (removed != expectedRemoved) {
					LogUtils.log("Expected " + expectedRemoved + " removed containers, actual removed containers is " + removed);
				}
				return added == expectedAdded && removed == expectedRemoved;
			}
		};
		
		repetitiveAssertTrue("Automatic scale out did not perform as expected", condition, OPERATION_TIMEOUT);
	}

	private void repetitiveAssertApplicationNodeIntact(final ApplicationNode simple) {
		final RepetitiveConditionProvider condition = new RepetitiveConditionProvider() {
		
			@Override
			public boolean getCondition() {
				return simple.getStatus().equals(DeploymentStatus.INTACT);
			}
		};
		repetitiveAssertTrueWithScreenshot(
				"customServiceMonitor service is displayed as " + simple.getStatus() + 
					"even though it is installed", condition, this.getClass(), "customServiceMonitorsAutoScalingTest", SERVICE_NAME);
	}

	private void repetitiveAssertTwoWebModules(
			final ApplicationServicesGrid applicationServicesGrid) {
		final RepetitiveConditionProvider condition = new RepetitiveConditionProvider() {		
			@Override
			public boolean getCondition() {
				return applicationServicesGrid.getWebModule().getCount() == 2;
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition, waitingTime);
	}

	private void repetitiveAssertOneWebServerModule(
			final ApplicationServicesGrid applicationServicesGrid) {
		final RepetitiveConditionProvider condition = new RepetitiveConditionProvider() {		
			@Override
			public boolean getCondition() {
				return applicationServicesGrid.getWebServerModule().getCount() == 1;
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition, waitingTime);
	}

	private void repetitiveAssertStatistics(final InternalProcessingUnit pu,
			final ProcessingUnitStatisticsId statisticsId,
			final Number expectedResult) {
		repetitiveAssertTrue("Failed waiting for counter to be "+ expectedResult, new RepetitiveConditionProvider() {
			
			@Override
			public boolean getCondition() {
				
				final Object counter = pu.getStatistics().getStatistics().get(statisticsId);
				if (counter == null) {
					LogUtils.log("Cannot get statistics " + statisticsId);
					return false;
				}
				
				if (!(counter instanceof Number)) {
					LogUtils.log("Cannot get Number from statistics " + statisticsId);
					return false;
				}
				
				boolean equals = ((Number)counter).doubleValue() == expectedResult.doubleValue();
				if (!equals) {
					LogUtils.log("Waiting for value of average(counter), to be " + expectedResult + " current value is " + counter);
				}
				return equals;
			
				
			}
		}, OPERATION_TIMEOUT);
	}
	
	public void setStatistics(long value) throws IOException, InterruptedException {
		String command = "connect localhost;invoke --verbose " + SERVICE_NAME + " set " + value;
		CommandTestUtils.runCommandAndWait(command);
	}


}
