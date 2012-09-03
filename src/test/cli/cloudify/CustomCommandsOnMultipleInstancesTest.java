package test.cli.cloudify;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import junit.framework.Assert;

import org.cloudifysource.dsl.utils.ServiceUtils;
import org.openspaces.admin.pu.ProcessingUnit;
import org.testng.annotations.Test;

import test.usm.USMTestUtils;
import framework.utils.LogUtils;

public class CustomCommandsOnMultipleInstancesTest extends AbstractLocalCloudTest {
	
	private static final String SERVICE_NAME = "simpleCustomCommandsMultipleInstances";
	
	private int totalInstances;

	private void installService() {
		installService(SERVICE_NAME);
        
		final String absolutePUName = ServiceUtils.getAbsolutePUName(DEFAULT_APPLICATION_NAME, SERVICE_NAME);
		final ProcessingUnit pu = admin.getProcessingUnits().waitFor(absolutePUName , WAIT_FOR_TIMEOUT_SECONDS , TimeUnit.SECONDS);
		assertTrue("USM Service State is NOT RUNNING", USMTestUtils.waitForPuRunningState(absolutePUName, WAIT_FOR_TIMEOUT_SECONDS, TimeUnit.SECONDS, admin));
		totalInstances = pu.getTotalNumberOfInstances();
	}

    

	private void uninstallService() {
		super.uninstallService(SERVICE_NAME);
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT , groups="1", enabled = true)
	public void testPrintCommand() throws Exception {
		installService();
		LogUtils.log("Checking print command on all instances");
		checkPrintCommand();
		
		LogUtils.log("Starting to check print command by instance id");
		for(int i=1 ; i<= totalInstances ; i++) {
			checkPrintCommand(i);
		}
		uninstallService();
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT , groups="1", enabled = true)
	public void testParamsCommand() throws Exception {
		installService();
		LogUtils.log("Checking params command on all instances");
		checkParamsCommand();
		
		LogUtils.log("Starting to check params command by instance id");
		for(int i=1 ; i<= totalInstances ; i++) {
			checkParamsCommand(i);
		}
		uninstallService();
	}
	
	//TODO: enable test once the dependency bug in the CLI is resolved.
	@Test(timeOut = DEFAULT_TEST_TIMEOUT , groups="1", enabled = true)
	public void testXceptionCommand() throws Exception {
		installService();
		LogUtils.log("Checking exception command on all instances");
		checkExceptionCommand();
		
		LogUtils.log("Starting to check exception command by instance id");
		for(int i=1 ; i<= totalInstances ; i++) {
			checkExceptionCommand(i);
		}
		uninstallService();
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT , groups="1", enabled = true)
	public void testRunScriptCommand() throws Exception {
		installService();
		LogUtils.log("Checking runScript command on all instances");
		checkRunScriptCommand();
		
		LogUtils.log("Starting to check runScript command by instance id");
		for(int i=1 ; i<= totalInstances ; i++) {
			checkRunScriptCommand(i);
		}
		uninstallService();
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT , groups="1", enabled = true)
	public void testContextCommand() throws Exception {
		installService();
		LogUtils.log("Checking context command on all instances");
		checkContextCommand();
		
		LogUtils.log("Starting to check context command by instance id");
		for(int i=1 ; i<= totalInstances ; i++) {
			checkContextCommand(i);
		}
		uninstallService();
	}
	
	private void checkPrintCommand() throws IOException, InterruptedException {
		String invokePrintResult = runCommand("connect " + restUrl + ";use-application default" 
				+ "; invoke simpleCustomCommandsMultipleInstances print");
		
		for(int i=1 ; i <= totalInstances ; i++){
			assertTrue("Custom command 'print' returned unexpected result from instance #" + i +": " + invokePrintResult
					,invokePrintResult.contains("OK from instance #" + i));
		}
	}
	private void checkPrintCommand(int instanceid) throws IOException, InterruptedException {
		String invokePrintResult = runCommand("connect " + restUrl + ";use-application default" 
				+ "; invoke -instanceid " + instanceid + " simpleCustomCommandsMultipleInstances print");
		
		assertTrue("Custom command 'print' returned unexpected result from instance #" + instanceid +": " + invokePrintResult
				,invokePrintResult.contains("OK from instance #" + instanceid));
		
		for(int i = 1 ; i <= totalInstances ; i++){
			if(i == instanceid)
				continue;
			Assert.assertFalse("should not recive any output from instance" + i ,invokePrintResult.contains("instance #" + i));
		}
	}
	
	private void checkParamsCommand() throws IOException, InterruptedException {
		String invokeParamsResult = runCommand("connect " + restUrl + ";use-application default" +
				"; invoke simpleCustomCommandsMultipleInstances params 2 3");
		
		for(int i=1 ; i <= totalInstances ; i++){
			assertTrue("Custom command 'params' returned unexpected result from instance #" + i + ": " + invokeParamsResult
					,invokeParamsResult.contains("OK from instance #" + i) && invokeParamsResult.contains("Result: this is the custom parameters command. expecting 123: 123"));
		}
	}
	
	private void checkParamsCommand(int instanceid) throws IOException, InterruptedException {
		String invokeParamsResult = runCommand("connect " + restUrl + ";use-application default" 
				+ "; invoke -instanceid " + instanceid + " simpleCustomCommandsMultipleInstances params 2 3");
		
		assertTrue("Custom command 'params' returned unexpected result from instance #" + instanceid +": " + invokeParamsResult
				,invokeParamsResult.contains("OK from instance #" + instanceid) && invokeParamsResult.contains("Result: this is the custom parameters command. expecting 123: 123"));
		
		for(int i=1 ; i <= totalInstances ; i++){
			if(i == instanceid)
				continue;	
			Assert.assertFalse("should not recive any output from instance" + i ,invokeParamsResult.contains("instance #" + i));
		}
	}
	
	private void checkExceptionCommand() throws IOException, InterruptedException {
		String invokeExceptionResult = CommandTestUtils.runCommandExpectedFail("connect " + restUrl + ";use-application default" 
				+ "; invoke simpleCustomCommandsMultipleInstances exception");
		
		for(int i=1 ; i <= totalInstances ; i++){
			assertTrue("Custom command 'exception' returned unexpected result from instance #" + i +": " + invokeExceptionResult
					,invokeExceptionResult.contains("FAILED from instance #" + i) && invokeExceptionResult.contains("This is an error test"));
		}
	}
	
	private void checkExceptionCommand(int instanceid) throws IOException, InterruptedException {
		String invokeExceptionResult = CommandTestUtils.runCommandExpectedFail("connect " + restUrl + ";use-application default" 
				+ "; invoke -instanceid " + instanceid + " simpleCustomCommandsMultipleInstances exception");
		
		assertTrue("Custom command 'exception' returned unexpected result from instance #" + instanceid +": " + invokeExceptionResult
				,invokeExceptionResult.contains("FAILED from instance #" + instanceid) && invokeExceptionResult.contains("This is an error test"));
		
		for(int i=1 ; i <= totalInstances ; i++){
			if(i == instanceid)
				continue;	
			Assert.assertFalse("should not recive any output from instance" + i ,invokeExceptionResult.contains("instance #" + i));
		}
	}
	
	private void checkRunScriptCommand() throws IOException, InterruptedException {
		String invokeRunScriptResult = runCommand("connect " + restUrl + ";use-application default" 
				+ "; invoke simpleCustomCommandsMultipleInstances runScript");
		
		for(int i=1 ; i <= totalInstances ; i++){
			assertTrue("Custom command 'runScript' returned unexpected result from instance #" + i +": " + invokeRunScriptResult
					,invokeRunScriptResult.contains("OK from instance #" + i) && invokeRunScriptResult.contains("Result: 2"));
		}
	}
	
	private void checkRunScriptCommand(int instanceid) throws IOException, InterruptedException {
		String invokeRunScriptResult = runCommand("connect " + restUrl + ";use-application default" 
				+ "; invoke -instanceid " + instanceid + " simpleCustomCommandsMultipleInstances runScript");
		
		assertTrue("Custom command 'exception' returned unexpected result from instance #" + instanceid +": " + invokeRunScriptResult
				,invokeRunScriptResult.contains("OK from instance #" + instanceid) && invokeRunScriptResult.contains("Result: 2"));
		
		for(int i=1 ; i <= totalInstances ; i++){
			if(i == instanceid)
				continue;	
			Assert.assertFalse("should not recive any output from instance" + i ,invokeRunScriptResult.contains("instance #" + i));
		}
	}
	
	private void checkContextCommand() throws IOException, InterruptedException {
		String invokeContextResult = runCommand("connect " + restUrl + ";use-application default" 
				+ "; invoke simpleCustomCommandsMultipleInstances context");
		
		for(int i=1 ; i <= totalInstances ; i++){
			assertTrue("Custom command 'context' returned unexpected result from instance #" + i +": " + invokeContextResult
					,invokeContextResult.contains("OK from instance #" + i) && invokeContextResult.contains("Service Dir is:"));
		}
	}
	
	private void checkContextCommand(int instanceid) throws IOException, InterruptedException {
		String invokeContextResult = runCommand("connect " + restUrl + ";use-application default" 
				+ "; invoke -instanceid " + instanceid + " simpleCustomCommandsMultipleInstances context");
		
		assertTrue("Custom command 'context' returned unexpected result from instance #" + instanceid +": " + invokeContextResult
				,invokeContextResult.contains("OK from instance #" + instanceid) && invokeContextResult.contains("Service Dir is:"));
		
		for(int i=1 ; i <= totalInstances ; i++){
			if(i == instanceid)
				continue;	
			Assert.assertFalse("should not recive any output from instance" + i ,invokeContextResult.contains("instance #" + i));
		}
	}

	
}
