package test.cli.cloudify.pu;

import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.pu.DeploymentStatus;
import org.testng.annotations.Test;

import org.cloudifysource.dsl.utils.ServiceUtils;

import test.cli.cloudify.AbstractLocalCloudTest;
import test.cli.cloudify.CommandTestUtils;

import framework.tools.SGTestHelper;
import framework.utils.LogUtils;
/**
 * StockDemoApplicationTest verifies the StockDemo application is installed properly.
 * The StockDemo application files are being created and placed in the SG apps folder
 * automatically by the build from the SVN and therefore, the application file do not reside in the SG apps folder.
 * This means that in-order to run the test locally, you must first reset the application path (Locally). 
 * @author adaml
 *
 */
public class StockDemoApplicationTest extends AbstractLocalCloudTest {
	
	private String STOCK_DEMO_APPLICATION_NAME = "stockdemo";

	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, enabled = true)
	public void testStockdemoApplication() throws Exception {
		//If running locally, read test description first.
		String applicationDir = SGTestHelper.getSGTestRootDir().replace("\\", "/") + "/apps/USM/usm/applications/stockdemo";
		String command = "connect " + restUrl + ";" + "install-application " + "--verbose -timeout 25 " + applicationDir;
		CommandTestUtils.runCommandAndWait(command);
		
		assertTrue(STOCK_DEMO_APPLICATION_NAME + " is not installed" , admin.getApplications().getApplication("stockdemo") != null);
		
		int currentNumberOfInstances;
		currentNumberOfInstances = getProcessingUnitInstanceCount(ServiceUtils.getAbsolutePUName(STOCK_DEMO_APPLICATION_NAME, "cassandra"));
		assertTrue("Expected 2 PU instances. Actual number of instances is " + currentNumberOfInstances, currentNumberOfInstances == 1);
		assertPuStatusInstact("stockdemo.cassandra");
		
		currentNumberOfInstances = getProcessingUnitInstanceCount(ServiceUtils.getAbsolutePUName(STOCK_DEMO_APPLICATION_NAME, "stockAnalyticsMirror"));
		assertTrue("Expected 1 PU instances. Actual number of instances is " + currentNumberOfInstances, currentNumberOfInstances == 1);
		assertPuStatusInstact("stockdemo.stockAnalyticsMirror");
		
		currentNumberOfInstances = getProcessingUnitInstanceCount(ServiceUtils.getAbsolutePUName(STOCK_DEMO_APPLICATION_NAME, "stockAnalyticsSpace"));
		assertTrue("Expected 2 PU instances. Actual number of instances is " + currentNumberOfInstances, currentNumberOfInstances == 2);
		assertPuStatusInstact("stockdemo.stockAnalyticsSpace");
		
		currentNumberOfInstances = getProcessingUnitInstanceCount(ServiceUtils.getAbsolutePUName(STOCK_DEMO_APPLICATION_NAME, "stockAnalyticsProcessor"));
		assertTrue("Expected 2 PU instances. Actual number of instances is " + currentNumberOfInstances, currentNumberOfInstances == 2);
		assertPuStatusInstact("stockdemo.stockAnalyticsProcessor");
		
		currentNumberOfInstances = getProcessingUnitInstanceCount(ServiceUtils.getAbsolutePUName(STOCK_DEMO_APPLICATION_NAME, "StockDemo"));
		assertTrue("Expected 1 PU instances. Actual number of instances is " + currentNumberOfInstances, currentNumberOfInstances == 1);
		assertPuStatusInstact("stockdemo.StockDemo");
		
		currentNumberOfInstances = getProcessingUnitInstanceCount(ServiceUtils.getAbsolutePUName(STOCK_DEMO_APPLICATION_NAME, "stockAnalytics"));
		assertTrue("Expected 1 PU instances. Actual number of instances is " + currentNumberOfInstances, currentNumberOfInstances == 1);
		assertPuStatusInstact("stockdemo.stockAnalytics");
		
		for (GridServiceContainer gsc : admin.getGridServiceContainers().getContainers()) {
			LogUtils.scanContainerLogsFor(gsc, "[SEVERE]");
		}
	}
	
	private void assertPuStatusInstact(String puName) {
		assertTrue(admin.getProcessingUnits().getProcessingUnit(puName).getStatus().equals(DeploymentStatus.INTACT));
	}

}
