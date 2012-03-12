package test.rest;

import java.util.concurrent.TimeUnit;

import junit.framework.Assert;
import junit.framework.AssertionFailedError;

import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.pu.ProcessingUnit;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.cli.cloudify.AbstractLocalCloudTest;

public class RestDependencyOnManagementSpaceTest extends AbstractLocalCloudTest{
	
	private GridServiceManager gsm;
	
	
	@Override
	@BeforeMethod
	public void beforeTest() {	
		super.beforeTest();
		gsm = admin.getGridServiceManagers().waitForAtLeastOne(1, TimeUnit.MINUTES);
		gsm.undeploy("cloudifyManagementSpace");
		gsm.undeploy("rest");
		ProcessingUnit rest = admin.getProcessingUnits().waitFor("rest", 1, TimeUnit.MINUTES);
		ProcessingUnit mngSpace = admin.getProcessingUnits().waitFor("cloudifyManagementSpace", 1, TimeUnit.MINUTES);
		Assert.assertTrue(mngSpace==null && rest==null);
	}
	
	@Test(timeOut = OPERATION_TIMEOUT, groups = "1", enabled = true)
	public void test() throws Exception{
		try{
			
			RestConsistencyTestUtil.deployRestServer(admin);
			Assert.assertTrue("Rest should not be able to deploy without management space", false);
		}catch(AssertionFailedError e){
			boolean deployRestFailed = e.getMessage().toLowerCase().contains("failed to deploy rest server");
			Assert.assertTrue(deployRestFailed);	
		}
		
	}
	
	public static boolean isWindows() {
		return System.getProperty("os.name").toLowerCase().startsWith("win");
	}
	
}
