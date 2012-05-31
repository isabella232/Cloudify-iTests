package test.cli.cloudify;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.junit.Assert;
import org.openspaces.admin.Admin;
import org.openspaces.admin.AdminFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.AbstractTest;
import framework.utils.LogUtils;
import framework.utils.SetupUtils;

public class LocalcloudBootstrapAfterTeardownTest extends AbstractTest {
	
	private String restPort = "8100";
	private String restUrl;
	private Set<String> localcloudPids; 
	@Override
	@BeforeMethod
	public void beforeTest() { 	
        LogUtils.log("Test Configuration Started: "+ this.getClass());
		try {
			Set <String> startPids = SetupUtils.getLocalProcesses();
			admin = getAdminWithLocators();		
			LogUtils.log("bootstrapping localcloud");
			CommandTestUtils.runCommandAndWait("bootstrap-localcloud -lookup-groups abc");
			restUrl = "http://" + InetAddress.getLocalHost().getHostAddress() + ":" + restPort;
			Set <String> endPids = SetupUtils.getLocalProcesses();
			localcloudPids = SetupUtils.getClientProcessesIDsDelta(startPids, endPids);
		}catch(Exception e){
			LogUtils.log("before test failed", e);
			Assert.assertTrue(false);
		}	  
		Assert.assertNotNull("initial bootstrap was not sucessfull" ,
				admin.getGridServiceAgents().waitForAtLeastOne(1, TimeUnit.MINUTES));
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = false)
	public void test() throws Exception{
		
		LogUtils.log("tearing down localcloud");
		CommandTestUtils.runCommandAndWait("connect " + restUrl + ";teardown-localcloud -lookup-groups abc");
		Set <String> pidsAfterTeardown = SetupUtils.getLocalProcesses();
		for(String pid : localcloudPids)
			Assert.assertFalse("localcloud process with pid [" + pid + "] is still running after teardown", 
					pidsAfterTeardown.contains(pid));
		LogUtils.log("bootstrapping localcloud");
		CommandTestUtils.runCommandAndWait("bootstrap-localcloud -lookup-groups abc");
		Assert.assertTrue("bootstrap-localcloud failed after teardown-localcloud: agent is down", admin.getGridServiceAgents().waitFor(1, 1, TimeUnit.MINUTES));
		Assert.assertNotNull("bootstrap-localcloud failed after teardown-localcloud: rest is down", admin.getProcessingUnits().waitFor("rest", 1, TimeUnit.MINUTES));
		Assert.assertNotNull("bootstrap-localcloud failed after teardown-localcloud: webui is down", admin.getProcessingUnits().waitFor("webui", 1, TimeUnit.MINUTES));
		Assert.assertNotNull("bootstrap-localcloud failed after teardown-localcloud: mngSpace is down", admin.getProcessingUnits().waitFor("cloudifyManagementSpace", 1, TimeUnit.MINUTES));
	}	
	
	@Override
	@AfterMethod
	public void afterTest() throws Exception {
		try {
			CommandTestUtils.runCommandAndWait("connect " + restUrl + ";teardown-localcloud -lookup-groups abc");
		} catch (Exception e) {
			LogUtils.log("theardown-localcloud at after test failed", e);
			Assert.assertTrue(false);
		} 
		super.afterTest();
	}
	
	private Admin getAdminWithLocators()
			throws UnknownHostException {
		// Class LocalhostGridAgentBootsrapper defines the locator discovery addresses.
		final String nicAddress = "127.0.0.1"; // Constants.getHostAddress();

		// int defaultLusPort = Constants.getDiscoveryPort();
		final AdminFactory factory = new AdminFactory();
		LogUtils.log("adding locator to admin : " + nicAddress + ":" + CloudifyConstants.DEFAULT_LOCALCLOUD_LUS_PORT);
		factory.addLocator(nicAddress + ":" + CloudifyConstants.DEFAULT_LOCALCLOUD_LUS_PORT);
		return factory.createAdmin();
	}
}
