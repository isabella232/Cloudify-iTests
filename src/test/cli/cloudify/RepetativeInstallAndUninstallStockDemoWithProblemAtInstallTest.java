package test.cli.cloudify;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;

import junit.framework.Assert;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import framework.utils.LogUtils;
import framework.utils.WebUtils;

public class RepetativeInstallAndUninstallStockDemoWithProblemAtInstallTest extends AbstractLocalCloudTest {

	private final int repetitions = 4;
	private String cassandraPostStartScriptPath = null;
	private String newPostStartScriptPath = null;
	private URL stockdemoUrl;
		
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * repetitions, groups = "1", enabled = true)
	public void installAndUninstallTest() throws Exception {
		
		stockdemoUrl = new URL(InetAddress.getLocalHost().getHostAddress() + ":8080/stockdemo/stockdemo");
		final String stockdemoAppPath = CommandTestUtils.getPath("apps/USM/usm/applications/stockdemo");	
		cassandraPostStartScriptPath = stockdemoAppPath + "/cassandra/cassandra_poststart.groovy";	
		newPostStartScriptPath = stockdemoAppPath + "/cassandra/cassandra_poststart123.groovy";
		int scenarioSuccessCounter = 0;
		int scenarioFailCounter = 0;
		int firstInstallSuccessCounter = 0;
		
		for(int i=0 ; i < repetitions ; i++){
			switch(installUninstallInstall(stockdemoAppPath, cassandraPostStartScriptPath ,newPostStartScriptPath)){
			case 1: {firstInstallSuccessCounter++;break;}
			case 2: {scenarioSuccessCounter++;break;}
			case 3: {scenarioFailCounter++;break;}
				
			}
			runCommand("connect " + restUrl + ";uninstall-application --verbose stockdemo");
			assertUninstallWasSuccessful();
		}
		LogUtils.log(firstInstallSuccessCounter + "/" + repetitions + " times the first installation succeedded, these runs are irrelavent");
		LogUtils.log(scenarioSuccessCounter + "/" + repetitions + " times the second installation succeedded");
		LogUtils.log(scenarioFailCounter + "/" + repetitions + " times the second installation failed - THIS IS WHAT WE TEST FOR");
		Assert.assertTrue("second install should never fail, it failed " + scenarioFailCounter + " times", scenarioFailCounter==0);
	}

	private int installUninstallInstall(String stockdemoAppPath, String cassandraPostStartScriptPath ,String  newPostStartScriptPath) throws Exception {
		corruptCassandraService(cassandraPostStartScriptPath ,newPostStartScriptPath);
		
		String failOutput = CommandTestUtils.runCommand("connect " + restUrl + ";install-application --verbose -timeout 5 " + stockdemoAppPath, true, true);		
		if(!failOutput.toLowerCase().contains("operation failed"))
			return 1;
		fixCassandraService(cassandraPostStartScriptPath , newPostStartScriptPath);
		runCommand("connect " + restUrl + ";uninstall-application --verbose stockdemo");
		assertUninstallWasSuccessful();
		
		String successOutput = CommandTestUtils.runCommand("connect " + restUrl + ";install-application --verbose -timeout 5 " + stockdemoAppPath, true, true);
		runCommand("connect " + restUrl + ";uninstall-application --verbose stockdemo");
		if(successOutput.toLowerCase().contains("successfully installed") && WebUtils.isURLAvailable(stockdemoUrl))
			return 2;
		else
			return 3;
	}
	
	@Override
	@AfterMethod
	public void afterTest(){
		super.afterTest();
		try {
			fixCassandraService(cassandraPostStartScriptPath , newPostStartScriptPath);
		} catch (IOException e) {
			LogUtils.log("FAILED FIXING CASSANDRA SERVICE!!!");
		}
	}

	private void corruptCassandraService(String cassandraPostStartScriptPath , String newPostStartScriptPath) throws IOException {
		File cassandraPostStartScript = new File(cassandraPostStartScriptPath);
		boolean success = cassandraPostStartScript.renameTo(new File(newPostStartScriptPath));
		if(!success)
			throw new IOException("Test error: failed renaming " +  cassandraPostStartScriptPath + " to " + newPostStartScriptPath);
	}
	
	private void fixCassandraService(String cassandraPostStartScriptPath , String newPostStartScriptPath) throws IOException {
		File cassandraPostStartScript = new File(newPostStartScriptPath);
		boolean success = cassandraPostStartScript.renameTo(new File(cassandraPostStartScriptPath));
		if(!success)
			throw new IOException("Test error: failed renaming " +  newPostStartScriptPath + " to " + cassandraPostStartScriptPath);
	}
	
private void assertUninstallWasSuccessful() throws Exception{
		
		URL cassandraPuAdminUrl = new URL(restUrl + "/admin/ProcessingUnits/Names/stockdemo.cassandra");
		URL stockAnalyticsMirrorPuAdminUrl = new URL(restUrl + "/admin/ProcessingUnits/Names/stockdemo.stockAnalyticsMirror");
		URL stockAnalyticsSpacePuAdminUrl = new URL(restUrl + "/admin/ProcessingUnits/Names/stockdemo.stockAnalyticsSpace");
		URL stockAnalyticsProcessorPuAdminUrl = new URL(restUrl + "/admin/ProcessingUnits/Names/stockdemo.stockAnalyticsProcessor");
		URL StockDemoPuAdminUrl = new URL(restUrl + "/admin/ProcessingUnits/Names/stockdemo.StockDemo");
		URL stockAnalyticsPuAdminUrl = new URL(restUrl + "/admin/ProcessingUnits/Names/stockdemo.stockAnalytics");
		URL stockAnalyticsFeederPuAdminUrl = new URL(restUrl + "/admin/ProcessingUnits/Names/stockdemo.stockAnalyticsFeeder");
		
		assertTrue(!WebUtils.isURLAvailable(cassandraPuAdminUrl));
		assertTrue(!WebUtils.isURLAvailable(stockAnalyticsMirrorPuAdminUrl));
		assertTrue(!WebUtils.isURLAvailable(stockAnalyticsSpacePuAdminUrl));
		assertTrue(!WebUtils.isURLAvailable(stockAnalyticsProcessorPuAdminUrl));
		assertTrue(!WebUtils.isURLAvailable(StockDemoPuAdminUrl));
		assertTrue(!WebUtils.isURLAvailable(stockAnalyticsPuAdminUrl));
		assertTrue(!WebUtils.isURLAvailable(stockAnalyticsFeederPuAdminUrl));
	}
}
