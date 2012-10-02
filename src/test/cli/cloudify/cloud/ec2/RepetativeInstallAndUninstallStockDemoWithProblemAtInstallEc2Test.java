package test.cli.cloudify.cloud.ec2;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import junit.framework.Assert;

import org.apache.commons.io.FileUtils;
import org.testng.ITestContext;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import test.cli.cloudify.CommandTestUtils;
import test.cli.cloudify.cloud.NewAbstractCloudTest;
import test.cli.cloudify.cloud.services.ec2.Ec2CloudService;
import framework.tools.SGTestHelper;
import framework.utils.LogUtils;
import framework.utils.ScriptUtils;
import framework.utils.WebUtils;
/**
 * runs on ec2<p>
 * loops several times:<p>
	1. try to install stockdemo on and fail<p>
	2. uninstall<p>
	3. try to install again <p>

	Details: the failour at step 1 is achieved by renaming cassandra's post start script.
	before step 3 is done the script is renamed back to the original name, this is why step 3 
	is asserted.
 * @author gal
 *
 */
public class RepetativeInstallAndUninstallStockDemoWithProblemAtInstallEc2Test extends NewAbstractCloudTest {

	private static final String APPLICATION_NAME = "stockdemo";
	private final int repetitions = 1;
	private String cassandraPostStartScriptPath = null;
	private String newPostStartScriptPath = null;
	private Ec2CloudService service;
	private String restUrl;
	private final String stockdemoAppPath = CommandTestUtils.getPath("apps/USM/usm/applications/stockdemo");

	private File cloudPluginDir = new File(ScriptUtils.getBuildPath() + "/tools/cli/plugins/esc/ec2");
	private URL stockdemoUrl;


	@BeforeClass
	public void bootstrap(ITestContext iTestContext) {	

		LogUtils.log("put cloudify-xap license in upload dir, needed to run stockdemo app");
		File xapLicense = new File(SGTestHelper.getSGTestRootDir() + "/apps/cloudify/cloud/gslicense.xml");
		File cloudifyOverrides = new File(cloudPluginDir.getAbsolutePath() + "/upload/cloudify-overrides");
		if (!cloudifyOverrides.exists()) {
			cloudifyOverrides.mkdir();
		}
		try {
			FileUtils.copyFileToDirectory(xapLicense, cloudifyOverrides);
		}
		catch (IOException e) {
			AssertFail(e.getMessage());
		}

		service = new Ec2CloudService(this.getClass().getName());

		super.bootstrap(iTestContext, service);

		restUrl = super.getRestUrl();

		cloudPluginDir = new File(service.getPathToCloudFolder());
		String hostIp = restUrl.substring(0, restUrl.lastIndexOf(':'));
		try {
			stockdemoUrl = new URL(hostIp + ":8080/stockdemo.StockDemo/");
		} catch (MalformedURLException e) {
			AssertFail(e.getMessage() , e);
		}
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 6, groups = "1", enabled = false)
	public void installAndUninstallTest() throws Exception {

		cassandraPostStartScriptPath = stockdemoAppPath + "/cassandra/cassandra_poststart.groovy";	
		newPostStartScriptPath = stockdemoAppPath + "/cassandra/cassandra_poststart123.groovy";
		int secondInstallationSuccessCounter = 0;
		int secondInstallationFailCounter = 0;
		int firstInstallSuccessCounter = 0;

		for(int i=0 ; i < repetitions ; i++){
			LogUtils.log("starting iteration " + i);
			switch(installUninstallInstall(stockdemoAppPath, cassandraPostStartScriptPath ,newPostStartScriptPath)){
			case 1: {firstInstallSuccessCounter++;
			break;
			}
			case 2: {secondInstallationSuccessCounter++;
			break;
			}
			case 3: {secondInstallationFailCounter++;
			break;
			}				
			}
			LogUtils.log("uninstalling stockdemo after iteration " + i);
			uninstallApplicationAndWait(APPLICATION_NAME);
			LogUtils.log("asserting all services are down");
			assertUninstallWasSuccessful();
		}
		LogUtils.log(firstInstallSuccessCounter + "/" + repetitions + " times the first installation succeedded, this should not happen");
		LogUtils.log(secondInstallationSuccessCounter + "/" + repetitions + " times the second installation succeedded");
		LogUtils.log(secondInstallationFailCounter + "/" + repetitions + " times the second installation failed - THIS IS WHAT WE TEST FOR");
		Assert.assertTrue("second install should never fail, it failed " + secondInstallationFailCounter + " times", secondInstallationFailCounter==0);
	}

	private int installUninstallInstall(String stockdemoAppPath, String cassandraPostStartScriptPath ,String  newPostStartScriptPath) throws Exception {
		LogUtils.log("corrupting cassandra service");
		corruptCassandraService(cassandraPostStartScriptPath ,newPostStartScriptPath);
		try {
			LogUtils.log("first installation of stockdemo - this should fail");
			installApplication(stockdemoAppPath, APPLICATION_NAME, 5, true, true);		
		} catch(AssertionError e){
			return 1;
		}
		LogUtils.log("fixing cassandra service");
		fixCassandraService(cassandraPostStartScriptPath , newPostStartScriptPath);
		LogUtils.log("uninstalling stockdemo");
		uninstallApplicationAndWait(APPLICATION_NAME);
		LogUtils.log("asserting all services are down");
		assertUninstallWasSuccessful();
		try {
			LogUtils.log("second installation of stockdemo - this should succeed");
			installApplication(stockdemoAppPath, APPLICATION_NAME, 45, true, false);
			LogUtils.log("checking second installation's result");
			Assert.assertTrue("The applications home page isn't available, counts as not installed properly" , WebUtils.isURLAvailable(stockdemoUrl));
			return 2;
		} catch(AssertionError e){
			LogUtils.log(e.getMessage());
			return 3;
		}

	}

	private void corruptCassandraService(String cassandraPostStartScriptPath , String newPostStartScriptPath) throws IOException {
		File cassandraPostStartScript = new File(cassandraPostStartScriptPath);
		FileUtils.moveFile(cassandraPostStartScript, new File(newPostStartScriptPath));
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
	
	@AfterClass
	public void teardown() {
		super.teardown();
	}
	
	@AfterMethod
	public void cleanUp() throws IOException, InterruptedException {
		if ((getService() != null) && (getService().getRestUrls() != null)) {
			String command = "connect " + getRestUrl() + ";list-applications";
			String output = CommandTestUtils.runCommandAndWait(command);
			if (output.contains(APPLICATION_NAME)) {
				uninstallApplicationAndWait(APPLICATION_NAME);
			}
		}
	}


	@Override
	protected String getCloudName() {
		return "ec2";
	}


	@Override
	protected boolean isReusableCloud() {
		// TODO Auto-generated method stub
		return false;
	}


	@Override
	protected void customizeCloud() throws Exception {
		// TODO Auto-generated method stub

	}
}
