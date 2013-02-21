package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud;

import java.io.IOException;

import org.cloudifysource.quality.iTests.framework.utils.LogUtils;
import org.cloudifysource.quality.iTests.test.cli.cloudify.CommandTestUtils.ProcessResult;

/**
 * 
 * verifies the volume was mounted and is writable.
 * 
 * @author adaml
 *
 */
public abstract class AbstractStorageTest extends NewAbstractCloudTest{

	private static final String SERVICE_NAME = "simpleStorage";
	
	final static String WRITE_FILE_COMMAND_NAME = "writeToStorage";
	final static String LIST_FILES_COMMAND_NAME = "listFilesInStorage";
	final static String LIST_MOUNTED_DEVICES_COMMAND_NAME = "listMount";
	
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
	
	protected void testStorageVolumeMounted(String expectedMountOutput) throws IOException, InterruptedException {
		
		LogUtils.log("Listing all mounted devices. running command 'mount -l' on remote machine");
		ProcessResult listMountedResult = invokeCommand(SERVICE_NAME, LIST_MOUNTED_DEVICES_COMMAND_NAME);
		
		assertTrue("failed listing all mounted devices. Output was " + listMountedResult.getOutput(),
				listMountedResult.getExitcode() == 0);
		assertTrue("device is not in the mounted devices list: " + listMountedResult.getOutput(),
				listMountedResult.getOutput().contains(expectedMountOutput));
	}
}
