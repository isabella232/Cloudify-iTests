package test.cli.cloudify.cloud;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import junit.framework.Assert;

import org.apache.commons.io.FileUtils;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.cli.cloudify.CommandTestUtils;
import test.cli.cloudify.cloud.ec2.Ec2StockDemoCloudService;
import framework.tools.SGTestHelper;
import framework.utils.LogUtils;
import framework.utils.WebUtils;

public class RepetativeInstallAndUninstallStockDemoWithProblemAtInstallEc2Test extends AbstractCloudTest {

	private final int repetitions = 3 ;
	private String cassandraPostStartScriptPath = null;
	private String newPostStartScriptPath = null;
	private AbstractCloudService service;
	private URL stockdemoUrl;
	
	@BeforeMethod
	public void bootstrap() throws IOException, InterruptedException {	
		service = new Ec2StockDemoCloudService();
		service.bootstrapCloud();
		setService(service);
		String hostIp = service.getRestUrl().substring(0, service.getRestUrl().lastIndexOf(':'));
		stockdemoUrl = new URL(hostIp + ":8080/stockdemo/stockdemo");
	}
	
	
	@AfterMethod(alwaysRun = true)
	public void teardown() throws IOException {
		try {
			service.teardownCloud();
		}
		catch (Throwable e) {
			LogUtils.log("caught an exception while tearing down ec2", e);
			sendTeardownCloudFailedMail(EC2, e);
		}
		File backupEc2Dir = new File(SGTestHelper.getBuildDir() + "/tools/cli/plugins/esc/ec2.backup");
		File currentEc2Dir = new File(SGTestHelper.getBuildDir() + "/tools/cli/plugins/esc/ec2");
		if (backupEc2Dir.exists()){
			FileUtils.deleteDirectory(currentEc2Dir);
			FileUtils.moveDirectory(backupEc2Dir, currentEc2Dir);
		}
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT *(1 + repetitions), groups = "1", enabled = true)
	public void installAndUninstallTest() throws Exception {
		
		
		final String stockdemoAppPath = CommandTestUtils.getPath("apps/USM/usm/applications/stockdemo");	
		cassandraPostStartScriptPath = stockdemoAppPath + "/cassandra/cassandra_poststart.groovy";	
		newPostStartScriptPath = stockdemoAppPath + "/cassandra/cassandra_poststart123.groovy";
		int scenarioSuccessCounter = 0;
		int scenarioFailCounter = 0;
		int firstInstallSuccessCounter = 0;
		
		for(int i=0 ; i < repetitions ; i++){
			switch(installUninstallInstall(stockdemoAppPath, cassandraPostStartScriptPath ,newPostStartScriptPath)){
			case 1: {firstInstallSuccessCounter++;
					break;
					}
			case 2: {scenarioSuccessCounter++;
					break;
					}
			case 3: {scenarioFailCounter++;
					break;
					}				
			}
			uninstallApplicationAndWait("stockdemo");
			assertUninstallWasSuccessful();
		}
		LogUtils.log(firstInstallSuccessCounter + "/" + repetitions + " times the first installation succeedded, this should not happen");
		LogUtils.log(scenarioSuccessCounter + "/" + repetitions + " times the second installation succeedded");
		LogUtils.log(scenarioFailCounter + "/" + repetitions + " times the second installation failed - THIS IS WHAT WE TEST FOR");
		Assert.assertTrue("second install should never fail, it failed " + scenarioFailCounter + " times", scenarioFailCounter==0);
	}

	private int installUninstallInstall(String stockdemoAppPath, String cassandraPostStartScriptPath ,String  newPostStartScriptPath) throws Exception {
		corruptCassandraService(cassandraPostStartScriptPath ,newPostStartScriptPath);
		try{
			installApplication(stockdemoAppPath, "stockdemo", 5, true, true);		
		}catch(AssertionError e){
			return 1;
		}
		fixCassandraService(cassandraPostStartScriptPath , newPostStartScriptPath);
		uninstallApplicationAndWait("stockdemo");
		assertUninstallWasSuccessful();
		try{
			installApplication(stockdemoAppPath, "stockdemo", 5, true, true);		
			Assert.assertTrue("The applications home page isn't available, counts as not installed properly" ,
					WebUtils.isURLAvailable(stockdemoUrl));
			return 2;
		}catch(AssertionError e){
			return 3;
		}

	}
	
	@Override
	@AfterMethod
	public void afterTest(){
		try {
			fixCassandraService(cassandraPostStartScriptPath , newPostStartScriptPath);
		} catch (IOException e) {
			LogUtils.log("FAILED FIXING CASSANDRA SERVICE AFTER TEST");
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
		
		URL cassandraPuAdminUrl = new URL(service.getRestUrl() + "/admin/ProcessingUnits/Names/stockdemo.cassandra");
		URL stockAnalyticsMirrorPuAdminUrl = new URL(service.getRestUrl() + "/admin/ProcessingUnits/Names/stockdemo.stockAnalyticsMirror");
		URL stockAnalyticsSpacePuAdminUrl = new URL(service.getRestUrl() + "/admin/ProcessingUnits/Names/stockdemo.stockAnalyticsSpace");
		URL stockAnalyticsProcessorPuAdminUrl = new URL(service.getRestUrl() + "/admin/ProcessingUnits/Names/stockdemo.stockAnalyticsProcessor");
		URL StockDemoPuAdminUrl = new URL(service.getRestUrl() + "/admin/ProcessingUnits/Names/stockdemo.StockDemo");
		URL stockAnalyticsPuAdminUrl = new URL(service.getRestUrl() + "/admin/ProcessingUnits/Names/stockdemo.stockAnalytics");
		URL stockAnalyticsFeederPuAdminUrl = new URL(service.getRestUrl() + "/admin/ProcessingUnits/Names/stockdemo.stockAnalyticsFeeder");
		
		assertTrue(!WebUtils.isURLAvailable(cassandraPuAdminUrl));
		assertTrue(!WebUtils.isURLAvailable(stockAnalyticsMirrorPuAdminUrl));
		assertTrue(!WebUtils.isURLAvailable(stockAnalyticsSpacePuAdminUrl));
		assertTrue(!WebUtils.isURLAvailable(stockAnalyticsProcessorPuAdminUrl));
		assertTrue(!WebUtils.isURLAvailable(StockDemoPuAdminUrl));
		assertTrue(!WebUtils.isURLAvailable(stockAnalyticsPuAdminUrl));
		assertTrue(!WebUtils.isURLAvailable(stockAnalyticsFeederPuAdminUrl));
	}
	

	

	
}
