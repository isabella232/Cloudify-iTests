package test.cli.cloudify;

import java.io.IOException;

import org.testng.annotations.Test;

import framework.utils.LogUtils;
/**
 * Validate a Timeout exception is being thrown in the CLI 
 * after the service/application installation timeout has ended.
 * 
 * installs a service/application that has a sleep in its post_start event closure.
 * The installation defines a timeout of 1 minute and is expected to fail.
 * 
 * @author adaml
 *
 */
public class InstallTimoutTest extends AbstractLocalCloudTest{

	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
	public void testServiceInstallTimout() throws IOException, InterruptedException{
		
		String usmServicePath = getUsmApplicationPath("simpleTimeout/simpleTimeout");
		
		long preInstallTimeMillis = System.currentTimeMillis();
		LogUtils.log("Starting service installation " + preInstallTimeMillis);
		
		//this service holds a 'Thread.sleep' command in the post_start event that waits for a minute.  
		String commandOutput = CommandTestUtils.runCommandExpectedFail("connect " + this.restUrl + ";install-service -timeout 1 " + usmServicePath);
		
		long postInstallTimeMillis = System.currentTimeMillis();
		LogUtils.log("Service installation ended" + preInstallTimeMillis);
		
		long totalInstallationTime = postInstallTimeMillis - preInstallTimeMillis;
		LogUtils.log("The total installation time in millis is " + totalInstallationTime + " (Should be more then 1 minute)" );
		
		assertTrue("Timeout parameter ignored", totalInstallationTime > 60000);
		assertTrue("Service installation did not throw a Timeout exception.", commandOutput.contains("Service installation timed out"));
		uninstallService("simpleTimeout");
		
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
	public void testApplicationInstallTimout() throws IOException, InterruptedException{
		String usmServicePath = getUsmApplicationPath("simpleTimeout");
		
		long preInstallTimeMillis = System.currentTimeMillis();
		LogUtils.log("Starting application installation " + preInstallTimeMillis);
		
		//this service holds a 'Thread.sleep' command in the post_start event that waits for a minute.  
		String commandOutput = CommandTestUtils.runCommandExpectedFail("connect " + this.restUrl + ";install-application -timeout 1 " + usmServicePath);
		
		long postInstallTimeMillis = System.currentTimeMillis();
		LogUtils.log("Service installation ended" + preInstallTimeMillis);
		
		long totalInstallationTime = postInstallTimeMillis - preInstallTimeMillis;
		LogUtils.log("The total installation time in millis is " + totalInstallationTime + " (Should be more then 1 minute)" );
		
		assertTrue("Timeout parameter ignored", totalInstallationTime > 60000);
		assertTrue("Application installation did not throw a Timeout exception.", commandOutput.contains("Application installation timed out"));
		uninstallApplication("simpleTimeout");
	}
	
	private String getUsmApplicationPath(String dirOrFilename) {
		return CommandTestUtils.getPath("apps/USM/usm/applications/" + dirOrFilename);
	}
}
