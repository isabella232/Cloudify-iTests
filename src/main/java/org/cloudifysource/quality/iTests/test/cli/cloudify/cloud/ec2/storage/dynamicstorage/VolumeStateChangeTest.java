package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.ec2.storage.dynamicstorage;

import iTests.framework.utils.LogUtils;

import java.io.IOException;

import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
import org.cloudifysource.quality.iTests.test.cli.cloudify.CommandTestUtils;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.AbstractStorageAllocationTest;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
/**
 * This test will install a service with static and dynamic storage. It will then change the state of one of the volumes and
 * assert both volumes state is valid.
 *  
 * @author adaml
 *
 */
public class VolumeStateChangeTest extends AbstractStorageAllocationTest {

	private static final String SERVICE_NAME = "groovy";
	static final String SERVICE_PATH = CommandTestUtils.getPath("/src/main/resources/apps/USM/usm/dynamicstorage/create-only");
	@BeforeClass(alwaysRun = true)
	protected void bootstrap() throws Exception {
		super.bootstrap();
	}
	
	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 4, enabled = true)
	public void testStateChangeOfDynamicVolume() throws Exception {
		//this will install a service having one static storage and one dynamic storage started on the postStart event.
        installServiceAndWait(SERVICE_PATH, SERVICE_NAME, false);
        
        // getting the volume state for both volumes
        LogUtils.log("getting volumes state from space..");
        final String beforeFormat = invoke("getDevicesSpaceState");
        
        // assert volume state is currect for both volumes.
        LogUtils.log("Asserting volume state of both storage volumes.");
        assertTrue("Expecting dynamic storage state of device '/dev/sdf' to be ATTACHED.", beforeFormat.contains("Found volume with device: /dev/sdf and state: ATTACHED"));
        assertTrue("Expecting static storage state of device '/dev/sdc' to be MOUNTED.", beforeFormat.contains("Found volume with device: /dev/sdc and state: MOUNTED"));
        
        // format dynamic volume. Change volume state.
        LogUtils.log("Changing state of one of the volumes.");
        final String formatOutput = invoke("formatVolume /dev/sdf ext4");
        assertTrue("Fromat volume invocation failed.",formatOutput.contains("1: OK from instance #1"));
        
        // getting the volume state for both volumes
        final String afterFormat = invoke("getDevicesSpaceState");
        
        // assert only one of the volume states have changed.
        LogUtils.log("Asserting only one of the volumes state has changed.");
        assertTrue("Expecting static storage state of device '/dev/sdc' to be MOUNTED.", afterFormat.contains("Found volume with device: /dev/sdc and state: MOUNTED"));
        assertTrue("Expecting dynamic storage state of device '/dev/sdf' to be FORMATTED.", afterFormat.contains("Found volume with device: /dev/sdf and state: FORMATTED"));
        
        // uninstalling service. This will also remove the dynamic storage.
        LogUtils.log("Uninstalling service and removing dynamic storage.");
        uninstallServiceAndWait(SERVICE_NAME);
	}
	
    private String invoke(final String command) throws IOException, InterruptedException {
		return CommandTestUtils.runCommandAndWait("connect " + getRestUrl() + "; invoke " + SERVICE_NAME + " " + command);
	}

	@AfterClass(alwaysRun = true)
	protected void teardown() throws Exception {
		try {
			super.scanForLeakedVolumesCreatedViaTemplate("SMALL_BLOCK");
		} finally {
			super.teardown();
		}
	}

    @Override
    protected String getCloudName() {
        return "ec2";
    }

    @Override
	protected boolean isReusableCloud() {
		return false;
	}
}
