package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud;

import org.cloudifysource.dsl.Service;
import org.cloudifysource.dsl.internal.ServiceReader;
import org.cloudifysource.quality.iTests.framework.utils.AssertUtils;
import org.cloudifysource.quality.iTests.framework.utils.LogUtils;
import org.cloudifysource.quality.iTests.framework.utils.StorageUtils;
import org.cloudifysource.quality.iTests.test.cli.cloudify.CommandTestUtils;
import org.cloudifysource.quality.iTests.test.cli.cloudify.CommandTestUtils.ProcessResult;

import java.io.File;
import java.io.IOException;

/**
 * 
 * verifies the volume was mounted and is writable.
 * contains generic tests.
 *
 * @author adaml, nirb
 *
 */
public abstract class AbstractStorageTest extends NewAbstractCloudTest{

	final static String WRITE_FILE_COMMAND_NAME = "writeToStorage";
	final static String LIST_FILES_COMMAND_NAME = "listFilesInStorage";
	final static String LIST_MOUNTED_DEVICES_COMMAND_NAME = "listMount";

    protected final static String SERVICE_NAME = "simpleStorage";
    protected final static String SERVICE_PATH = CommandTestUtils.getPath("src/main/resources/apps/USM/usm/" + SERVICE_NAME);
    private final static String SERVICE_FILE_PATH = CommandTestUtils.getPath("src/main/resources/apps/USM/usm/" + SERVICE_NAME + "/simple-service.groovy");

    public void bootstrapAndInit() throws Exception{

        super.bootstrap();

        File serviceFile = new File(SERVICE_FILE_PATH);
        Service service = ServiceReader.readService(serviceFile);

        StorageUtils.init(getService().getCloud(), service.getCompute().getTemplate(), service.getStorage().getTemplate(), getService());
    }

    public void cleanup() throws Exception{

        super.teardown();
        StorageUtils.scanAndDeleteLeakedVolumes();
    }

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

    public void testInstallWithStorage() throws Exception{

        installServiceAndWait(SERVICE_PATH, SERVICE_NAME);

        AssertUtils.assertTrue(StorageUtils.verifyVolumeConfiguration(SERVICE_FILE_PATH));

        uninstallServiceIfFound(SERVICE_NAME);

        AssertUtils.assertTrue("volumes were not deleted", !isVolumeUp());

    }

    public void testDeleteOnExitFalse() throws Exception{

        installServiceAndWait(SERVICE_PATH, SERVICE_NAME);

        AssertUtils.assertTrue("volume not started", isVolumeUp());

        uninstallServiceIfFound(SERVICE_NAME);

        AssertUtils.assertTrue("volume was deleted", isVolumeUp());
    }

    private boolean isVolumeUp() throws Exception {
        return !StorageUtils.getServiceNamedVolumes(SERVICE_FILE_PATH).isEmpty();
    }
}
