package test.cli.cloudify;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import junit.framework.Assert;

import org.openspaces.admin.pu.ProcessingUnit;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import framework.utils.LogUtils;

public class CustomCommandsOnMultipleInstancesTest extends AbstractCommandTest {
	
	private final String RECIPE_DIR_PATH = CommandTestUtils
									.getPath("apps/USM/usm/simpleCustomCommandsMultipleInstances");
	private int totalInstances;

	
	@Override
	@BeforeMethod
	public void beforeTest(){
		super.beforeTest();
		try {
			runCommand("connect " + this.restUrl + ";install-service --verbose " + RECIPE_DIR_PATH);
		} catch (Exception e) {
			e.printStackTrace();
		} 
		ProcessingUnit pu = admin.getProcessingUnits().waitFor("simpleCustomCommandsMultipleInstances");
		assertTrue("service was not installed", pu.waitFor(pu.getTotalNumberOfInstances(), 30, TimeUnit.SECONDS));
		totalInstances = pu.getTotalNumberOfInstances();
	}
	
	@Override
	@AfterMethod
	public void afterTest(){
		try {
			runCommand("connect " + this.restUrl + ";uninstall-service --verbose simpleCustomCommandsMultipleInstances");
		} catch (Exception e) {
			e.printStackTrace();
		} 
		super.afterTest();
	}
//////////////////////////////////////////////////////////////////////////////////////////////////////////	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT , groups="1", enabled = false)
	public void testPrintCommand() throws Exception {
		LogUtils.log("Checking print command on all instances");
		checkPrintCommand();
		
		LogUtils.log("Starting to check print command by instance id");
		for(int i=1 ; i<= totalInstances ; i++)
			checkPrintCommand(i);
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT , groups="1", enabled = false)
	public void testParamsCommand() throws Exception {
		LogUtils.log("Checking params command on all instances");
		checkParamsCommand();
		
		LogUtils.log("Starting to check params command by instance id");
		for(int i=1 ; i<= totalInstances ; i++)
			checkParamsCommand(i);		
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT , groups="1", enabled = false)
	public void testExceptionCommand() throws Exception {
		LogUtils.log("Checking exception command on all instances");
		checkExceptionCommand();
		
		LogUtils.log("Starting to check exception command by instance id");
		for(int i=1 ; i<= totalInstances ; i++)
			checkExceptionCommand(i);
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT , groups="1", enabled = false)
	public void testRunScriptCommand() throws Exception {
		LogUtils.log("Checking runScript command on all instances");
		checkRunScriptCommand();
		
		LogUtils.log("Starting to check runScript command by instance id");
		for(int i=1 ; i<= totalInstances ; i++)
			checkRunScriptCommand(i);
	}
	
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT , groups="1", enabled = false)
	public void testContextCommand() throws Exception {
		LogUtils.log("Checking context command on all instances");
		checkContextCommand();
		
		LogUtils.log("Starting to check context command by instance id");
		for(int i=1 ; i<= totalInstances ; i++)
			checkContextCommand(i);
	}
	
////////////////////////////////////////////////////////////////////////////////////////////////////////	
	
	private void checkPrintCommand() throws IOException, InterruptedException {
		String invokePrintResult = runCommand("connect " + this.restUrl
				+ "; invoke simpleCustomCommandsMultipleInstances print");
		
		for(int i=1 ; i <= totalInstances ; i++){
			assertTrue("Custom command 'print' returned unexpected result from instance #" + i +": " + invokePrintResult
					,invokePrintResult.contains("OK from instance #" + i));
		}
	}
	private void checkPrintCommand(int instanceId) throws IOException, InterruptedException {
		String invokePrintResult = runCommand("connect " + this.restUrl
				+ "; invoke -instanceid " + instanceId + " simpleCustomCommandsMultipleInstances print");
		
		assertTrue("Custom command 'print' returned unexpected result from instance #" + instanceId +": " + invokePrintResult
				,invokePrintResult.contains("OK from instance #" + instanceId));
		
		for(int i=1 ; i <= totalInstances ; i++){
			if(i == instanceId)
				continue;
			Assert.assertFalse("should not recive any output from instance" + i ,invokePrintResult.contains("instance #" + i));
		}
	}
	
	private void checkParamsCommand() throws IOException, InterruptedException {
		String invokeParamsResult = runCommand("connect " + this.restUrl
				+ "; invoke simpleCustomCommandsMultipleInstances params ['x=2' 'y=3']");
		
		for(int i=1 ; i <= totalInstances ; i++){
			assertTrue("Custom command 'params' returned unexpected result from instance #" + i + ": " + invokeParamsResult
					,invokeParamsResult.contains("OK from instance #" + i) && invokeParamsResult.contains("Result: this is the custom parameters command. expecting 123: 123"));
		}
	}
	
	private void checkParamsCommand(int instanceId) throws IOException, InterruptedException {
		String invokeParamsResult = runCommand("connect " + this.restUrl
				+ "; invoke -instanceid " + instanceId + " simpleCustomCommandsMultipleInstances params");
		
		assertTrue("Custom command 'params' returned unexpected result from instance #" + instanceId +": " + invokeParamsResult
				,invokeParamsResult.contains("OK from instance #" + instanceId) && invokeParamsResult.contains("Result: this is the custom parameters command. expecting 123: 123"));
		
		for(int i=1 ; i <= totalInstances ; i++){
			if(i == instanceId)
				continue;	
			Assert.assertFalse("should not recive any output from instance" + i ,invokeParamsResult.contains("instance #" + i));
		}
	}
	
	private void checkExceptionCommand() throws IOException, InterruptedException {
		String invokeExceptionResult = runCommand("connect " + this.restUrl
				+ "; invoke simpleCustomCommandsMultipleInstances exception");
		
		for(int i=1 ; i <= totalInstances ; i++){
			assertTrue("Custom command 'exception' returned unexpected result from instance #" + i +": " + invokeExceptionResult
					,invokeExceptionResult.contains("FAILED from instance #" + i) && invokeExceptionResult.contains("This is an error test"));
		}
	}
	
	private void checkExceptionCommand(int instanceId) throws IOException, InterruptedException {
		String invokeExceptionResult = runCommand("connect " + this.restUrl
				+ "; invoke -instanceid " + instanceId + " simpleCustomCommandsMultipleInstances exception");
		
		assertTrue("Custom command 'exception' returned unexpected result from instance #" + instanceId +": " + invokeExceptionResult
				,invokeExceptionResult.contains("FAILED from instance #" + instanceId) && invokeExceptionResult.contains("This is an error test"));
		
		for(int i=1 ; i <= totalInstances ; i++){
			if(i == instanceId)
				continue;	
			Assert.assertFalse("should not recive any output from instance" + i ,invokeExceptionResult.contains("instance #" + i));
		}
	}
	
	private void checkRunScriptCommand() throws IOException, InterruptedException {
		String invokeRunScriptResult = runCommand("connect " + this.restUrl
				+ "; invoke simpleCustomCommandsMultipleInstances runScript");
		
		for(int i=1 ; i <= totalInstances ; i++){
			assertTrue("Custom command 'runScript' returned unexpected result from instance #" + i +": " + invokeRunScriptResult
					,invokeRunScriptResult.contains("OK from instance #" + i) && invokeRunScriptResult.contains("Result: 2"));
		}
	}
	
	private void checkRunScriptCommand(int instanceId) throws IOException, InterruptedException {
		String invokeRunScriptResult = runCommand("connect " + this.restUrl
				+ "; invoke -instanceid " + instanceId + " simpleCustomCommandsMultipleInstances runScript");
		
		assertTrue("Custom command 'exception' returned unexpected result from instance #" + instanceId +": " + invokeRunScriptResult
				,invokeRunScriptResult.contains("OK from instance #" + instanceId) && invokeRunScriptResult.contains("Result: 2"));
		
		for(int i=1 ; i <= totalInstances ; i++){
			if(i == instanceId)
				continue;	
			Assert.assertFalse("should not recive any output from instance" + i ,invokeRunScriptResult.contains("instance #" + i));
		}
	}
	
	private void checkContextCommand() throws IOException, InterruptedException {
		String invokeContextResult = runCommand("connect " + this.restUrl
				+ "; invoke simpleCustomCommandsMultipleInstances context");
		
		for(int i=1 ; i <= totalInstances ; i++){
			assertTrue("Custom command 'runScript' returned unexpected result from instance #" + i +": " + invokeContextResult
					,invokeContextResult.contains("OK from instance #" + i) && invokeContextResult.contains("Service Dir is:"));
		}
	}
	
	private void checkContextCommand(int instanceId) throws IOException, InterruptedException {
		String invokeContextResult = runCommand("connect " + this.restUrl
				+ "; invoke -instanceid " + instanceId + " simpleCustomCommandsMultipleInstances context");
		
		assertTrue("Custom command 'exception' returned unexpected result from instance #" + instanceId +": " + invokeContextResult
				,invokeContextResult.contains("OK from instance #" + instanceId) && invokeContextResult.contains("Service Dir is:"));
		
		for(int i=1 ; i <= totalInstances ; i++){
			if(i == instanceId)
				continue;	
			Assert.assertFalse("should not recive any output from instance" + i ,invokeContextResult.contains("instance #" + i));
		}
	}
	
}
