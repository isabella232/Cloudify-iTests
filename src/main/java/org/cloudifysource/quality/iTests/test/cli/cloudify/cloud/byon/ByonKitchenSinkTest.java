package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.byon;

import iTests.framework.utils.LogUtils;

import java.io.IOException;

import junit.framework.Assert;

import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
import org.cloudifysource.quality.iTests.test.cli.cloudify.CommandTestUtils;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class ByonKitchenSinkTest extends AbstractByonCloudTest {
	
	

	private static final String KEY_FILE_NAME = "testkey.pem";
	private static final String SERVICE_NAME = "simpleByonKitchensink";

	@Override
	protected String getCloudName() {
//		this.getService().getUser();
		return "byon";
	}
	
	@BeforeClass(alwaysRun = true)
	protected void bootstrap() throws Exception {
		super.bootstrap();
	}
	
	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 2, enabled = true)
	public void kitchenSinkTest() throws Exception {
		
		installServiceAndWait(getServicePath(SERVICE_NAME), SERVICE_NAME);
		
		LogUtils.log("asserting key file not copied to agent machines.");
		assertMachineKeyFileNotCopied();
		
	}
	
    private void assertMachineKeyFileNotCopied() throws IOException, InterruptedException {
    	String filesList = invokeListRemoteFilesCommand();
    	Assert.assertTrue("list of remote files contains the key file name.", 
    			!filesList.contains(KEY_FILE_NAME));
	}
    
	private String invokeListRemoteFilesCommand() throws IOException, InterruptedException {
		final String invokeCommand = "connect " + this.getRestUrl() + ";" 
    			+ " invoke " + SERVICE_NAME + " listRemoteFiles";
		LogUtils.log("invoking command: " + invokeCommand);
    	final String output = CommandTestUtils.runCommandAndWait(invokeCommand);
    	LogUtils.log("command output was: " + output);
    	assertTrue("invocation failed", output.contains("invocation completed successfully."));
    	return output;
	}

	protected String getServicePath(final String serviceName) {
    	return CommandTestUtils.getPath("src/main/resources/apps/USM/usm/" + serviceName);
    }
	
	@Override
	protected void customizeCloud() throws Exception {
		super.customizeCloud();
		getService().setSudo(false);
		getService().getProperties().put("keyFile", KEY_FILE_NAME);
	}
	
	@Override
	@AfterClass(alwaysRun = true)
	protected void teardown() throws Exception {
		uninstallServiceIfFound(SERVICE_NAME);
		super.teardown();
	}
}
