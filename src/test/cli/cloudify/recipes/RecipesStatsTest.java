package test.cli.cloudify.recipes;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.cloudifysource.dsl.Service;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.ServiceReader;
import org.cloudifysource.dsl.internal.packaging.PackagingException;
import org.cloudifysource.dsl.utils.ServiceUtils;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.openspaces.admin.pu.ProcessingUnitInstanceStatistics;
import org.openspaces.ui.MetricGroup;
import org.openspaces.ui.UserInterface;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.cli.cloudify.AbstractLocalCloudTest;
import test.cli.cloudify.CommandTestUtils;
import test.cli.cloudify.Constants;
import framework.utils.LogUtils;
import framework.utils.ScriptUtils;

public class RecipesStatsTest extends AbstractLocalCloudTest {
	private String recipesDirPath = ScriptUtils.getBuildPath() + "/recipes/services";
	public static volatile boolean portReleasedBeforTimeout;
	protected static volatile boolean portTakenBeforTimeout;

	public RecipesStatsTest() {
		super();
	}

	@Override
	@BeforeMethod
	public void beforeTest() {
		portReleasedBeforTimeout = false;
		portTakenBeforTimeout = false;
	}

	@Override
	@AfterMethod
	public void afterTest() {
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, groups = "1", enabled = false)
	public void testActivemq() throws IOException, InterruptedException, PackagingException {
		testServiceMetrics("activemq");
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, groups = "1", enabled = false)
	public void testSolr() throws IOException, InterruptedException, PackagingException {
		testServiceMetrics("solr");
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, groups = "1", enabled = false)
	public void testHsqldb() throws IOException, InterruptedException, PackagingException {
		testServiceMetrics("hsqldb");
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, groups = "1", enabled = false)
	public void testElasticSearch() throws IOException, InterruptedException, PackagingException {
		testServiceMetrics("elasticsearch");
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, groups = "1", enabled = true)
	public void testTomcat() throws IOException, InterruptedException, PackagingException {
		testServiceMetrics("tomcat");
	}

	/**
	 * Installs a service, gets the available statistics monitors from the PU and alerts if it does not support the required UI metrics.
	 * This method assumes the service path and service file are derived from the service's name (the standard naming convention).
	 * @param serviceName The name of the service
	 * @throws PackagingException Reporting a failure to parse the service's DSL file
	 * @throws InterruptedException Reporting a failure to install the service
	 * @throws IOException Reporting a failure to install the service
	 */
	private void testServiceMetrics(String serviceName) throws PackagingException, InterruptedException, IOException {
		
		String serviceFileName = serviceName + "-service.groovy";
		Set<String> missingMonitors = new HashSet<String>();
		String servicePath = recipesDirPath + "/" + serviceName;
		File serviceFile = new File(servicePath + "/" + serviceFileName);
		assertTrue("The service file is missing: " + serviceFile.getAbsolutePath(), serviceFile.exists());
		Set<String> metrics = getServiceMetrics(serviceFile);
		
		if (metrics.size()>0) {
			//install the service and get 1 PU instance
			ProcessingUnitInstance puInstance = installServiceWaitForInstance(serviceName, servicePath);

			// analyze the PU's statistics using the first instance (the same stats settings apply for
			// all instances)
			ProcessingUnitInstanceStatistics stats = puInstance.getStatistics();
			Set<String> puMonitorsNames = stats.getMonitors().get("USM").getMonitors().keySet();
			Set<String> defaultMonitorsNames = getDefaultProcessMonitors();
			
			//iterate the metrics configured in the service file and verify they are supported by the PU
			Iterator<String> metricsIterator = metrics.iterator();
			while (metricsIterator.hasNext()) {
				String metric = metricsIterator.next();
				if (!puMonitorsNames.contains(metric) && !defaultMonitorsNames.contains(metric)) {
					missingMonitors.add(metric);
				}
			}
			assertTrue("missingMonitors:" + missingMonitors, missingMonitors.size() == 0);
			
			LogUtils.log("Uninstalling service " + serviceName);
			CommandTestUtils.runCommandAndWait("connect " + restUrl + "; uninstall-service " + serviceName + "; exit;");
		}
	}
	
	/**
	 * Gets a list of built-in USM process monitors.
	 * This is not the best implementation, but for now it'll do...
	 * @return a list of built-in USM process monitors
	 */
	private Set<String> getDefaultProcessMonitors() {
		Set<String> defaultMonitors = new HashSet<String>();
		defaultMonitors.add(CloudifyConstants.USM_METRIC_PROCESS_CPU_USAGE);
		defaultMonitors.add(CloudifyConstants.USM_METRIC_PROCESS_CPU_TIME);
		defaultMonitors.add(CloudifyConstants.USM_METRIC_PROCESS_CPU_KERNEL_TIME);
		defaultMonitors.add(CloudifyConstants.USM_METRIC_PROCESS_TOTAL_CPU_TIME);
		defaultMonitors.add(CloudifyConstants.USM_METRIC_PROCESS_GROUP_ID);
		defaultMonitors.add(CloudifyConstants.USM_METRIC_PROCESS_USER_ID);
		defaultMonitors.add(CloudifyConstants.USM_METRIC_PROCESS_TOTAL_PAGE_FAULTS);
		defaultMonitors.add(CloudifyConstants.USM_METRIC_PROCESS_TOTAL_RESIDENTAL_MEMORY);
		defaultMonitors.add(CloudifyConstants.USM_METRIC_PROCESS_TOTAL_SHARED_MEMORY);
		defaultMonitors.add(CloudifyConstants.USM_METRIC_PROCESS_CPU_TOTAL_VIRTUAL_MEMORY);
		defaultMonitors.add(CloudifyConstants.USM_METRIC_PROCESS_KERNEL_SCHEDULING_PRIORITY);
		defaultMonitors.add(CloudifyConstants.USM_METRIC_PROCESS_ACTIVE_THREADS);
		defaultMonitors.add(CloudifyConstants.USM_METRIC_AVAILABLE_PROCESSORS);
		defaultMonitors.add(CloudifyConstants.USM_METRIC_COMMITTED_VIRTUAL_MEM_SIZE);
		defaultMonitors.add(CloudifyConstants.USM_METRIC_THREAD_COUNT);
		defaultMonitors.add(CloudifyConstants.USM_METRIC_PEAK_THREAD_COUNT);
		
		return defaultMonitors;
	}

	/**
	 * Gets the configured UI metrics for this service
	 * @param serviceFile The service DSL file 
	 * @return A set of metric names
	 * @throws PackagingException Reporting a failure to parse the service's DSL file
	 */
	private Set<String> getServiceMetrics(File serviceFile) throws PackagingException {
		Set<String> metrics = new HashSet<String>();

		Service service = ServiceReader.getServiceFromFile(serviceFile);
		UserInterface serviceUI = service.getUserInterface();
		if (serviceUI != null) {
			for (MetricGroup metricGroup : serviceUI.getMetricGroups()) {
				metrics.addAll(metricGroup.getMetrics());
			}	
		}
		
		return metrics;
	}
	
	/**
	 * Installs the service and waits until at least 1 PU instance is available. Returns the first PU instance.  
	 * @param serviceName The name of the service
	 * @param servicePath The path to the service's folder
	 * @return The first PU instance
	 * @throws InterruptedException Reporting a failure to install the service
	 * @throws IOException Reporting a failure to install the service
	 */
	private ProcessingUnitInstance installServiceWaitForInstance(String serviceName, String servicePath) throws InterruptedException, IOException {
		
		// deploying on the default application
		LogUtils.log("Installing service " + serviceName);
		CommandTestUtils.runCommandAndWait("connect " + restUrl + ";install-service --verbose " + servicePath);
		final String absolutePUName = ServiceUtils.getAbsolutePUName(DEFAULT_APPLICATION_NAME, serviceName);

		final ProcessingUnit processingUnit = admin.getProcessingUnits().waitFor(absolutePUName, Constants.PROCESSINGUNIT_TIMEOUT_SEC, TimeUnit.SECONDS);

		// asserting at least 1 processing unit instance is up
		assertTrue("Instance of '" + absolutePUName + "' service was not found", processingUnit != null
				&& processingUnit.waitFor(1, Constants.PROCESSINGUNIT_TIMEOUT_SEC, TimeUnit.SECONDS));
		
		ProcessingUnitInstance[] procUnitsInstances = admin.getProcessingUnits().getProcessingUnit(absolutePUName)
				.getInstances();
		
		return procUnitsInstances[0];
	}

}
