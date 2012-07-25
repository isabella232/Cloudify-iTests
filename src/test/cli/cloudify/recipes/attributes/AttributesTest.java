package test.cli.cloudify.recipes.attributes;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import framework.utils.DumpUtils;
import framework.utils.TeardownUtils;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.core.GigaSpace;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import test.cli.cloudify.AbstractLocalCloudTest;
import test.cli.cloudify.CommandTestUtils;
import test.usm.USMTestUtils;

import org.cloudifysource.dsl.Application;
import org.cloudifysource.dsl.Service;
import org.cloudifysource.dsl.internal.DSLException;
import org.cloudifysource.dsl.internal.ServiceReader;
import org.cloudifysource.dsl.internal.packaging.PackagingException;
import org.cloudifysource.dsl.utils.ServiceUtils;

import framework.utils.LogUtils;

public class AttributesTest extends AbstractLocalCloudTest {

	private final String MAIN_APPLICAION_DIR_PATH = CommandTestUtils
									.getPath("apps/USM/usm/applications/attributesTestApp");
	private final String SECONDARY_APPLICAION_DIR_PATH = CommandTestUtils
			.getPath("apps/USM/usm/applications/attributesTestApp2");
	
	private Application app;	
	private GigaSpace gigaspace;

	@BeforeClass
	public void beforeClass() throws Exception{
        super.beforeClass();
		gigaspace = admin.getSpaces().waitFor("cloudifyManagementSpace", 20, TimeUnit.SECONDS).getGigaSpace();
		installApplication();
		String absolutePUNameSimple1 = ServiceUtils.getAbsolutePUName("attributesTestApp", "getter");
		String absolutePUNameSimple2 = ServiceUtils.getAbsolutePUName("attributesTestApp", "setter");
		ProcessingUnit pu1 = admin.getProcessingUnits().waitFor(absolutePUNameSimple1 , WAIT_FOR_TIMEOUT , TimeUnit.SECONDS);
		ProcessingUnit pu2 = admin.getProcessingUnits().waitFor(absolutePUNameSimple2, WAIT_FOR_TIMEOUT , TimeUnit.SECONDS);
		assertNotNull(pu1);
		assertNotNull(pu2);
		assertTrue("applications was not installed", pu1.waitFor(pu1.getTotalNumberOfInstances(), WAIT_FOR_TIMEOUT, TimeUnit.SECONDS));
		assertTrue("applications was not installed", pu2.waitFor(pu2.getTotalNumberOfInstances(), WAIT_FOR_TIMEOUT, TimeUnit.SECONDS));
		assertNotNull("applications was not installed", admin.getApplications().getApplication("attributesTestApp"));
		assertTrue("USM Service State is NOT RUNNING", USMTestUtils.waitForPuRunningState(absolutePUNameSimple1, 60, TimeUnit.SECONDS, admin));
		assertTrue("USM Service State is NOT RUNNING", USMTestUtils.waitForPuRunningState(absolutePUNameSimple2, 60, TimeUnit.SECONDS, admin));
		
	}

	@Override
	@AfterMethod(alwaysRun=true)
	public void afterTest(){
		if (gigaspace != null) {
			gigaspace.clear(null);
		}
        if (admin != null) {
            TeardownUtils.snapshot(admin);
            DumpUtils.dumpLogs(admin);
        }
	}

    @AfterClass
    public void afterClass() throws IOException, InterruptedException{
        runCommand("connect " + restUrl + ";uninstall-application --verbose attributesTestApp");
    }
		
	@Test(timeOut = DEFAULT_TEST_TIMEOUT , groups="1", enabled = true)
	public void testSimpleApplicationSetAttribute() throws Exception {
		LogUtils.log("setting an application attribute from setter service");
		runCommand("connect " + restUrl + ";use-application attributesTestApp" 
				+ "; invoke setter setApp");

		String simpleGet = runCommand("connect " + restUrl + ";use-application attributesTestApp" 
				+ "; invoke getter getApp");
		
		String simpleGet2 = runCommand("connect " + restUrl + ";use-application attributesTestApp" 
				+ "; invoke setter getApp");
		
		String simpleGet3 = runCommand("connect " + restUrl + ";use-application attributesTestApp" 
				+ "; invoke setter getService");
		
		assertTrue("command did not execute" , simpleGet.contains("OK"));
		assertTrue("command did not execute" , simpleGet2.contains("OK"));
		assertTrue("command did not execute" , simpleGet3.contains("OK"));
		assertTrue("getter service cannot get the application attribute", simpleGet.contains("myValue"));
		assertTrue("setter service cannot get the application attribute", simpleGet2.contains("myValue"));
		assertTrue("setter service shouldn't be able to get the application attribute using getService", 
				   simpleGet3.contains("null"));
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT , groups="1", enabled = true)
	public void testOverrideInstanceAttribute() throws Exception {
		runCommand("connect " + restUrl + ";use-application attributesTestApp" 
				+ "; invoke -instanceid 1 setter setInstanceCustom myKey1 myValue1");
		
		String getBeforeOverride = runCommand("connect " + restUrl + ";use-application attributesTestApp" 
				+ "; invoke -instanceid 1 setter getInstanceCustom myKey1");
		assertEquals("wrong number of objects in space", 1, gigaspace.count(null));
		assertTrue("command did not execute" , getBeforeOverride.contains("OK"));
		assertTrue("service cannot get the instance attribute", getBeforeOverride.contains("myValue1"));
		
		runCommand("connect " + restUrl + ";use-application attributesTestApp" 
				+ "; invoke -instanceid 1 setter setInstanceCustom myKey1 myValue2");
		String getAfterOverride = runCommand("connect " + restUrl + ";use-application attributesTestApp" 
				+ "; invoke -instanceid 1 setter getInstanceCustom myKey1");
		assertEquals("wrong number of objects in space", 1, gigaspace.count(null));
		assertTrue("command did not execute" , getAfterOverride.contains("OK"));
		assertTrue("instance attribute was not overriden properly", getAfterOverride.contains("myValue2") && 
																   !getAfterOverride.contains("myValue1"));
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT , groups="1", enabled = true)
	public void testApplicationSetAttributeCustomParams() throws Exception {
		
		runCommand("connect " + restUrl + ";use-application attributesTestApp" 
				+ "; invoke setter setAppCustom myKey1 myValue1");
		
		String simpleGet = runCommand("connect " + restUrl + ";use-application attributesTestApp" 
				+ "; invoke getter getAppCustom myKey1");
		
		assertTrue("command did not execute" , simpleGet.contains("OK"));
		assertTrue("getter service cannot get the application attribute when using parameters", simpleGet.contains("myValue1"));
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT , groups="1", enabled = true)
	public void testServiceSetAttribute() throws Exception {
		
		runCommand("connect " + restUrl + ";use-application attributesTestApp" 
				+ "; invoke setter setService");
		
		String crossServiceGet = runCommand("connect " + restUrl + ";use-application attributesTestApp" 
				+ "; invoke getter getService");
		
		String serviceGet = runCommand("connect " + restUrl + ";use-application attributesTestApp" 
				+ "; invoke setter getService");
		
		String appGet = runCommand("connect " + restUrl + ";use-application attributesTestApp" 
				+ "; invoke getter getApp");
		
		String getInstance1 = runCommand("connect " + restUrl + ";use-application attributesTestApp" 
				+ "; invoke -instanceid 1 setter getService");
		String getInstance2 = runCommand("connect " + restUrl + ";use-application attributesTestApp" 
				+ "; invoke -instanceid 2 setter getService");
		
		String instanceGetApp = runCommand("connect " + restUrl + ";use-application attributesTestApp" 
				+ "; invoke -instanceid 1 setter getApp");
		
		assertTrue("command did not execute" , crossServiceGet.contains("OK"));
		assertTrue("command did not execute" , serviceGet.contains("OK"));
		assertTrue("command did not execute" , appGet.contains("OK"));
		assertTrue("command did not execute" , getInstance1.contains("OK"));
		assertTrue("command did not execute" , getInstance2.contains("OK"));
		assertTrue("command did not execute" , instanceGetApp.contains("OK"));
		assertTrue("setService should be visible to the same service", serviceGet.contains("myValue"));
		assertTrue("setService should be visible to all service instances", getInstance1.contains("myValue") && getInstance2.contains("myValue"));
		assertTrue("setService should not be visible to a different service", crossServiceGet.contains("null"));
		assertTrue("getApp should be able to get a service attribute", appGet.contains("null"));
		assertTrue("getApp should be able to get a service attribute", instanceGetApp.contains("null"));
	}

    @Test(timeOut = DEFAULT_TEST_TIMEOUT , groups="1", enabled = true)
    public void testServiceSetAttributeByName() throws Exception {

        String setServiceByName = runCommand("connect " + restUrl + ";use-application attributesTestApp"
                + "; invoke setter setServiceByName");

        String getServiceByName = runCommand("connect " + restUrl + ";use-application attributesTestApp"
                + "; invoke setter getServiceByName");

        assertTrue("command did not execute" , setServiceByName.contains("OK"));
        assertTrue("command did not execute" , getServiceByName.contains("OK"));

        assertTrue(getServiceByName.contains("myValue"));
    }

    @Test(timeOut = DEFAULT_TEST_TIMEOUT , groups="1", enabled = true)
    public void testServiceInstanceSetAttribute() throws Exception {

        runCommand("connect " + restUrl + ";use-application attributesTestApp"
                + "; invoke -instanceid 1 setter setInstanceCustom1 myKey1 myValue1");

        runCommand("connect " + restUrl + ";use-application attributesTestApp"
                + "; invoke -instanceid 2 setter setInstanceCustom2 myKey1 myValue2");

        String getInstance1 = runCommand("connect " + restUrl + ";use-application attributesTestApp"
                + "; invoke -instanceid 1 setter getInstanceCustom1 myKey1");

        String getInstance2 = runCommand("connect " + restUrl + ";use-application attributesTestApp"
                + "; invoke -instanceid 2 setter getInstanceCustom2 myKey1");

        String getService = runCommand("connect " + restUrl + ";use-application attributesTestApp"
                + "; invoke setter getAppCustom myKey1");

        String getApp = runCommand("connect " + restUrl + ";use-application attributesTestApp"
                + "; invoke  setter getAppCustom myKey1");

        assertTrue("command did not execute" , getInstance1.contains("OK"));
        assertTrue("command did not execute" , getInstance2.contains("OK"));
        assertTrue("command did not execute" , getService.contains("OK"));
        assertTrue("command did not execute" , getApp.contains("OK"));

        assertTrue("getAppCustom should be return myValue1 when instanceId=1", getInstance1.contains("myValue1"));
        assertTrue("getAppCustom should be return myValue2 when instanceId=2", getInstance2.contains("myValue2"));
        assertTrue("getApp should be able to get a service attribute", getApp.contains("null"));
        assertTrue("getService should be able to get a service attribute", getService.contains("null"));
    }
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT , groups="1", enabled = true)
	public void testSetCustomPojo() throws Exception {
		LogUtils.log("setting a custom pojo on service level");
		runCommand("connect " + restUrl + ";use-application attributesTestApp" 
				+ "; invoke setter setAppCustomPojo");

		String getCustomPojo = runCommand("connect " + restUrl + ";use-application attributesTestApp" 
				+ "; invoke getter getAppCustomPojo");
				
		assertTrue("command did not execute" , getCustomPojo.contains("OK"));
		assertTrue("getter service cannot get the data pojo", getCustomPojo.contains("data"));
	}
	
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT , groups="1", enabled = true)
	public void testInstanceIteration() throws Exception {
		runCommand("connect " + restUrl + ";use-application attributesTestApp" 
				+ "; invoke -instanceid 1 setter setInstanceCustom myKey myValue1;" +
				"invoke -instanceid 2 setter setInstanceCustom myKey myValue2");
		
		String iterateInstances = runCommand("connect " + restUrl + ";use-application attributesTestApp" 
				+ "; invoke -instanceid 1 setter iterateInstances");
		
		assertTrue("command did not execute" , iterateInstances.contains("OK"));
		assertTrue("iteratoring over instances", iterateInstances.contains("myValue1") && iterateInstances.contains("myValue2"));
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT , groups="1", enabled = true )
	public void testRemoveThisService() throws Exception {
		runCommand("connect " + restUrl + ";use-application attributesTestApp" 
				+ "; invoke setter setService; invoke setter setService2");
		String getOutputAfterSet = runCommand("connect " + restUrl + ";use-application attributesTestApp" 
				+ "; invoke setter getService; invoke setter getService2");
		assertTrue("set command did not execute" , getOutputAfterSet.contains("myValue") && getOutputAfterSet.contains("myValue2"));
		
		runCommand("connect " + restUrl + ";use-application attributesTestApp" 
				+ "; invoke setter removeService");
		String getOutputAfterRemove = runCommand("connect " + restUrl + ";use-application attributesTestApp" 
				+ "; invoke setter getService ;invoke setter getService2");
		assertTrue("get myKey command should return null after myKey was removed" , getOutputAfterRemove.contains("null"));
		assertTrue("myKey2 should not be affected by remove myKey" , getOutputAfterRemove.contains("myValue2"));
	}
	@Test(timeOut = DEFAULT_TEST_TIMEOUT , groups="1", enabled = true)
	public void testRemoveInstance() throws Exception {
		runCommand("connect " + restUrl + ";use-application attributesTestApp" 
				+ "; invoke setter setInstance"); 
		String getOutputAfterSet = runCommand("connect " + restUrl + ";use-application attributesTestApp" 
				+ "; invoke setter getInstance");
		assertTrue("set command did not execute" , getOutputAfterSet.contains("myValue"));
		
		runCommand("connect " + restUrl + ";use-application attributesTestApp" 
				+ "; invoke setter removeInstance");
		String getOutputAfterRemove = runCommand("connect " + restUrl + ";use-application attributesTestApp" 
				+ "; invoke setter getInstance");
		assertTrue("getInstance command should return null after key was removed" , getOutputAfterRemove.toLowerCase().contains("null"));
	}
	
	
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT , groups="1", enabled = true)
	public void testCleanInstanceAfterSetService() throws Exception {
		runCommand("connect " + restUrl + ";use-application attributesTestApp" 
				+ "; invoke setter setInstance1");
		runCommand("connect " + restUrl + ";use-application attributesTestApp" 
				+ "; invoke setter setInstance2");
		String getInstanceBeforeRemove = runCommand("connect " + restUrl + ";use-application attributesTestApp" 
				+ "; invoke -instanceid 1 setter getInstance");
		assertTrue("set command did not execute" , getInstanceBeforeRemove.contains("myValue1"));
		getInstanceBeforeRemove = runCommand("connect " + restUrl + ";use-application attributesTestApp" 
				+ "; invoke -instanceid 2 setter getInstance");
		assertTrue("set command did not execute" , getInstanceBeforeRemove.contains("myValue2"));
		
		runCommand("connect " + restUrl + ";use-application attributesTestApp" 
				+ "; invoke -instanceid 1 setter cleanThisInstance");
		String getInstanceAfterRemove = runCommand("connect " + restUrl + ";use-application attributesTestApp" 
				+ "; invoke -instanceid 1 setter getInstance");
		assertTrue("get command should return null after key was removed" , getInstanceAfterRemove.toLowerCase().contains("null"));
		getInstanceAfterRemove = runCommand("connect " + restUrl + ";use-application attributesTestApp" 
				+ "; invoke -instanceid 2 setter getInstance");
		assertTrue("clear on instance 1 should not affect instance 2" , getInstanceAfterRemove.contains("myValue2"));
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT , groups="1", enabled = true)
	public void testCleanAppAfterSetService() throws Exception {
		runCommand("connect " + restUrl + ";use-application attributesTestApp" 
				+ "; invoke setter setService; invoke setter setService2");
		String getServiceBeforeClear = runCommand("connect " + restUrl + ";use-application attributesTestApp" 
				+ "; invoke setter getService; invoke setter getService2");
		assertTrue("set command did not execute" , getServiceBeforeClear.contains("myValue") && getServiceBeforeClear.contains("myValue2"));
				
		runCommand("connect " + restUrl + ";use-application attributesTestApp" 
				+ "; invoke getter cleanThisApp");
		String getServiceAfterClear = runCommand("connect " + restUrl + ";use-application attributesTestApp" 
				+ "; invoke setter setService ; invoke setter setService2");
		assertTrue("clear app should not affect service attributes" , 
				getServiceAfterClear.contains("myValue") && getServiceAfterClear.contains("myValue2"));
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT , groups="1", enabled = true)
	public void testSimpleSetGlobalAttributesOneApp() throws Exception {
		runCommand("connect " + restUrl + ";use-application attributesTestApp" 
				+ "; invoke setter setGlobal myGValue");
		
		String simpleGet = runCommand("connect " + restUrl + ";use-application attributesTestApp" 
				+ "; invoke getter getGlobal");
		
		assertTrue("command did not execute" , simpleGet.contains("OK"));
		assertTrue("getter service cannot get the global attribute when using parameters", simpleGet.contains("myGValue"));
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT , groups="1", enabled = true)
	public void testGlobalSetAttributeCustomParamsTwoApps() throws Exception {
		LogUtils.log("setting an global attribute from setter service");
		runCommand("connect " + restUrl + ";use-application attributesTestApp" 
				+ "; invoke setter setGlobalCustom myGlobalKey myGlobalValue");

		String simpleGet = runCommand("connect " + restUrl + ";use-application attributesTestApp" 
				+ "; invoke getter getGlobalCustom myGlobalKey");
	
		//install a different app
		runCommand("connect " + restUrl + ";install-application " + SECONDARY_APPLICAION_DIR_PATH + ";" );
		
		//Get the attribute from a different application
		String simpleGet2 = runCommand("connect " + restUrl + ";use-application attributesTestApp2" 
				+ "; invoke getter getGlobalCustom myGlobalKey");
		
		assertTrue("command did not execute" , simpleGet.contains("OK"));
		assertTrue("command did not execute" , simpleGet2.contains("OK"));
		assertTrue("getter service cannot get the application attribute", simpleGet.contains("myGlobalValue"));
		assertTrue("setter service cannot get the application attribute", simpleGet2.contains("myGlobalValue"));
		runCommand("connect " + this.restUrl + ";uninstall-application attributesTestApp2" );
	}
	
	private void installApplication() throws PackagingException, IOException, InterruptedException, DSLException {
		Service getter;
		Service setter;
		File applicationDir = new File(MAIN_APPLICAION_DIR_PATH);
		app = ServiceReader.getApplicationFromFile(applicationDir).getApplication();
		getter = app.getServices().get(0).getName().equals("getter") ? app.getServices().get(0) : app.getServices().get(1);
		setter = app.getServices().get(0).getName().equals("setter") ? app.getServices().get(0) : app.getServices().get(1);
		
		runCommand("connect " + restUrl + ";install-application --verbose " + MAIN_APPLICAION_DIR_PATH);
	}
}
