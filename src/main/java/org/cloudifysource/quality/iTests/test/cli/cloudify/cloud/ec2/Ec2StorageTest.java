package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.ec2;

import java.io.IOException;

import org.cloudifysource.quality.iTests.framework.utils.LogUtils;
import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
import org.cloudifysource.quality.iTests.test.cli.cloudify.CommandTestUtils;
import org.cloudifysource.quality.iTests.test.cli.cloudify.CommandTestUtils.ProcessResult;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.NewAbstractCloudTest;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * 
 * This test executes some custom commands on the service instance and 
 * verifies the volume was mounted and is writable.
 *  
 * @author adaml
 *
 */
public class Ec2StorageTest extends NewAbstractCloudTest{

	private static final String SERVICE_NAME = "simpleStorage";
	private static String SERVICE_PATH = CommandTestUtils.getPath("src/main/resources/apps/USM/usm/" + SERVICE_NAME);
	
	final static String WRITE_FILE_COMMAND_NAME = "writeToStorage";
	final static String LIST_FILES_COMMAND_NAME = "listFilesInStorage";
	final static String LIST_MOUNTED_DEVICES_COMMAND_NAME = "listMount";
	
	@Override
	protected String getCloudName() {
		return "ec2";
	}
	
	@BeforeClass(alwaysRun = true)
	protected void bootstrap() throws Exception {
		super.bootstrap();
		installServiceAndWait(SERVICE_PATH, SERVICE_NAME);
	}
	
	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 4, enabled = true)
	public void testWriteToStorage() throws IOException, InterruptedException {
		LogUtils.log("creating a new file called foo.txt in the storage volume. " 
						+ "running 'touch ~/storage/foo.txt' command on remote machine.");
		ProcessResult invokeCommandResult = invokeCommand(SERVICE_NAME, WRITE_FILE_COMMAND_NAME); 
		assertTrue("create file command exited with an abnormal exit code. Output was " + invokeCommandResult.getOutput(),
				invokeCommandResult.getExitcode() == 0);
		LogUtils.log("listing all files inside mounted storage folder. running 'ls ~/storage/' command");
		ProcessResult listFilesResult = invokeCommand(SERVICE_NAME, LIST_FILES_COMMAND_NAME); 
		assertTrue("failed listing files. Output was " + listFilesResult.getOutput(), listFilesResult.getExitcode() == 0);
		assertTrue("File was not created in storage volume. Output was " + listFilesResult.getOutput(),
				listFilesResult.getOutput().contains("foo.txt"));
	}
	
	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 4, enabled = true)
	protected void testStorageVolumeMounted() throws IOException, InterruptedException {
		LogUtils.log("Listing all mounted devices. running command 'mount -l' on remote machine");
		ProcessResult listMountedResult = invokeCommand(SERVICE_NAME, LIST_MOUNTED_DEVICES_COMMAND_NAME);
		assertTrue("failed listing all mounted devices. Output was " + listMountedResult.getOutput(),
				listMountedResult.getExitcode() == 0);
		assertTrue("device is not in the mounted devices list: " + listMountedResult.getOutput(),
				listMountedResult.getOutput().contains("/dev/xvdc on /home/ec2-user/storage type ext4 (rw)"));
	}
	
	@AfterClass(alwaysRun = true)
	protected void teardown() throws Exception {
		super.teardown();
	}

	@Override
	protected boolean isReusableCloud() {
		return false;
	}
}
