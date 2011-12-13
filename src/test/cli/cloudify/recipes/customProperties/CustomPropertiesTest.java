package test.cli.cloudify.recipes.customProperties;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.openspaces.admin.pu.ProcessingUnit;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import test.cli.cloudify.AbstractSingleBootstrapTest;
import test.cli.cloudify.CommandTestUtils;
import test.usm.USMTestUtils;

import com.gigaspaces.cloudify.dsl.Application;
import com.gigaspaces.cloudify.dsl.Service;
import com.gigaspaces.cloudify.dsl.internal.ServiceReader;
import com.gigaspaces.cloudify.dsl.internal.packaging.PackagingException;
import com.gigaspaces.cloudify.dsl.utils.ServiceUtils;

public class CustomPropertiesTest extends AbstractSingleBootstrapTest {
	
	private final String APPLICAION_DIR_PATH = CommandTestUtils
									.getPath("apps/USM/usm/applications/serviceContextProperties");
	
	private Application app;
	
	@Override
	@BeforeClass
	public void beforeClass() throws FileNotFoundException, PackagingException, IOException, InterruptedException{
		super.beforeClass();
		installApplication();
		String absolutePUNameSimple1 = ServiceUtils.getAbsolutePUName("serviceContextProperties", "getter");
		String absolutePUNameSimple2 = ServiceUtils.getAbsolutePUName("serviceContextProperties", "setter");
		ProcessingUnit pu1 = admin.getProcessingUnits().waitFor(absolutePUNameSimple1 , WAIT_FOR_TIMEOUT , TimeUnit.SECONDS);
		ProcessingUnit pu2 = admin.getProcessingUnits().waitFor(absolutePUNameSimple2, WAIT_FOR_TIMEOUT , TimeUnit.SECONDS);
		assertNotNull(pu1);
		assertNotNull(pu2);
		assertTrue("applications was not installed", pu1.waitFor(pu1.getTotalNumberOfInstances(), WAIT_FOR_TIMEOUT, TimeUnit.SECONDS));
		assertTrue("applications was not installed", pu2.waitFor(pu2.getTotalNumberOfInstances(), WAIT_FOR_TIMEOUT, TimeUnit.SECONDS));
		assertNotNull("applications was not installed", admin.getApplications().getApplication("simpleCustomCommandsMultipleInstances"));
		assertTrue("USM Service State is NOT RUNNING", USMTestUtils.waitForPuRunningState(absolutePUNameSimple1, 60, TimeUnit.SECONDS, admin));
		assertTrue("USM Service State is NOT RUNNING", USMTestUtils.waitForPuRunningState(absolutePUNameSimple2, 60, TimeUnit.SECONDS, admin));
	}

	@Override
	@AfterClass
	public void afterClass() throws IOException, InterruptedException{
		
		runCommand("connect " + this.restUrl + 
				";uninstall-application --verbose serviceContextProperties");	
		super.afterClass();
	}
	@Override
	@AfterTest
	public void afterTest(){
		for(Service service : app.getServices())
			service.getCustomProperties().clear();
				
		super.afterTest();
	}
//////////////////////////////////////////////////////////////////////////////////////////////////////////	
		
	@Test(timeOut = DEFAULT_TEST_TIMEOUT , groups="1", enabled = false)
	public void testSimpleApplicationSetContext() throws Exception {
		
		runCommand("connect " + this.restUrl + ";use-application serviceContextProperties" 
				+ "; invoke setter setApp");
		
		String simpleGet = runCommand("connect " + this.restUrl + ";use-application serviceContextProperties" 
				+ "; invoke getter getApp");
		
		assertTrue("no match", simpleGet.contains("myValue"));
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT , groups="1", enabled = false)
	public void testApplicationSetContextCustomParams() throws Exception {
		
		runCommand("connect " + this.restUrl + ";use-application serviceContextProperties" 
				+ "; invoke setter setAppCustom ['x=myKey1' 'y=myValue1']");
		
		String simpleGet = runCommand("connect " + this.restUrl + ";use-application serviceContextProperties" 
				+ "; invoke getter getAppCustom ['x=myKey1']");
		
		assertTrue("no match", simpleGet.contains("myValue1"));
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT , groups="1", enabled = false)
	public void testServiceSetContext() throws Exception {
		
		runCommand("connect " + this.restUrl + ";use-application serviceContextProperties" 
				+ "; invoke setter setService");
		
		String crossServiceGet = runCommand("connect " + this.restUrl + ";use-application serviceContextProperties" 
				+ "; invoke getter getService");
		
		String serviceGet = runCommand("connect " + this.restUrl + ";use-application serviceContextProperties" 
				+ "; invoke setter getService");
		
		String appGet = runCommand("connect " + this.restUrl + ";use-application serviceContextProperties" 
				+ "; invoke getter getApp");
		
		String getInstance1 = runCommand("connect " + this.restUrl + ";use-application serviceContextProperties" 
				+ "; invoke -instanceId=1 setter getApp");
		String getInstance2 = runCommand("connect " + this.restUrl + ";use-application serviceContextProperties" 
				+ "; invoke -instanceId=2 setter getApp");
		
		assertTrue("setService shuold be visible to the same service", serviceGet.contains("myValue"));
		assertTrue("setService shuold be visible to the application", appGet.contains("myValue"));
		assertTrue("setService shuold be visible to all service instances", getInstance1.contains("myValue") && getInstance2.contains("myValue"));
		assertTrue("setService shuold not be visible to a different service", !crossServiceGet.contains("myValue"));
	}
	
////////////////////////////////////////////////////////////////////////////////////////////////////////	

	
	
	private void installApplication() throws FileNotFoundException,
		PackagingException, IOException, InterruptedException {
		File applicationDir = new File(APPLICAION_DIR_PATH);
		app = ServiceReader.getApplicationFromFile(applicationDir).getApplication();
		
		runCommand("connect " + this.restUrl + ";install-application --verbose " + APPLICAION_DIR_PATH);
	}
}
