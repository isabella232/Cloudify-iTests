package test.webui.recipes.applications;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.Assert;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.utils.ServiceUtils;
import org.openspaces.admin.internal.pu.InternalProcessingUnit;
import org.openspaces.admin.pu.DeploymentStatus;
import org.openspaces.admin.pu.statistics.AverageInstancesStatisticsConfig;
import org.openspaces.admin.pu.statistics.ProcessingUnitStatisticsId;
import org.openspaces.admin.pu.statistics.ProcessingUnitStatisticsIdConfigurer;
import org.openspaces.admin.pu.statistics.ThroughputTimeWindowStatisticsConfigurer;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

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
import framework.utils.AssertUtils;
import framework.utils.AssertUtils.RepetitiveConditionProvider;
import framework.utils.LogUtils;
import framework.utils.WebUtils;

public class AutoScalingTomcatTotalRequestsTest extends AbstractSeleniumApplicationRecipeTest {

	private static final String COUNTER_METRIC = "Total Requests Count";
	private static final String APPLICATION_NAME = "travel";
	private static final String SERVICE_NAME = "tomcat";
	private static final String ABSOLUTE_SERVICE_NAME = ServiceUtils.getAbsolutePUName(APPLICATION_NAME,SERVICE_NAME);
	private String applicationUrl;
	private AtomicInteger requestsMade = new AtomicInteger(0);
	
	@Override
	@BeforeMethod
	public void install() throws IOException, InterruptedException {
		super.setCurrentApplication(APPLICATION_NAME);
		super.install();
		applicationUrl = "http://" + InetAddress.getLocalHost().getHostAddress() + ":8080";
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT , enabled = true)
	public void customServiceMonitorsAutoScalingTest() throws Exception {
		
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
		
		Assert.assertTrue(WebUtils.isURLAvailable(new URL(applicationUrl)));
		
		final InternalProcessingUnit pu = (InternalProcessingUnit) admin.getProcessingUnits().waitFor(ABSOLUTE_SERVICE_NAME,OPERATION_TIMEOUT,TimeUnit.MILLISECONDS);
		final ProcessingUnitStatisticsId statisticsId = 
				new ProcessingUnitStatisticsIdConfigurer()
				.monitor(CloudifyConstants.USM_MONITORS_SERVICE_ID)
				.metric(COUNTER_METRIC)
				.instancesStatistics(new AverageInstancesStatisticsConfig())
				.timeWindowStatistics(new ThroughputTimeWindowStatisticsConfigurer().timeWindow(30, TimeUnit.SECONDS).create())
				.create();
		 
		pu.addStatisticsCalculation(statisticsId);
		pu.setStatisticsInterval(1, TimeUnit.SECONDS);
		pu.startStatisticsMonitor();
		
		int threadNum = 10;
		ScheduledExecutorService executor= Executors.newScheduledThreadPool(threadNum);
		for(int i=0 ; i<threadNum ; i++){
			executor.scheduleWithFixedDelay(new HttpRequest(new URL(applicationUrl)), 0, 1, TimeUnit.SECONDS);
		
		}
		repetitiveAssertStatistics(pu, statisticsId, (double)threadNum);
				
		executor.shutdownNow();
		Assert.assertTrue(executor.awaitTermination(30, TimeUnit.SECONDS));
		uninstallApplication("travel", true);
	}
	
	
	
	public class HttpRequest implements Runnable{
		
		private URL url;
		
		
		public HttpRequest(URL url){
			this.url = url;
		}
		@Override
		public void run() {
			HttpClient client = new DefaultHttpClient();
			try {
				HttpGet get = new HttpGet(url.toURI());				
				client.execute(get);
				requestsMade.incrementAndGet();
			} catch (Throwable t) {
				if (!(t instanceof InterruptedException)) {
					LogUtils.log("an HttpRequest thread failed", t);
				}
				throw new RuntimeException(t); // this thread will never be scheduled again
				//barrier.reset();
			}finally{
				client.getConnectionManager().shutdown();
			}
			
		}
		
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
}
