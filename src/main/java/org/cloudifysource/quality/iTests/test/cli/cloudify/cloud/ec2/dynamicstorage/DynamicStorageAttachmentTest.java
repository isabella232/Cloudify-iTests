package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.ec2.dynamicstorage;

import java.io.File;
import java.util.concurrent.TimeoutException;

import org.cloudifysource.dsl.Service;
import org.cloudifysource.dsl.internal.ServiceReader;
import org.cloudifysource.quality.iTests.framework.utils.*;
import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.ec2.domain.Volume;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class DynamicStorageAttachmentTest extends AbstractEc2OneServiceDynamicStorageTest {
	
	private static final String FOLDER_NAME = "attach-only";
	
	private static final String STORAGE_NAME = System.getProperty("user.name") + "-" + DynamicStorageAttachmentTest.class.getSimpleName();
	
	private ServiceInstaller installer;
	private Ec2ComputeApiHelper computeHelper;

	
	@BeforeClass(alwaysRun = true)
	protected void bootstrap() throws Exception {
		super.bootstrap();
		this.computeHelper = new Ec2ComputeApiHelper(getService().getCloud());

	}
	
	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 4, enabled = true)
	public void testLinux() throws Exception {
		super.testLinux();
	}
	
	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 4, enabled = true)
	public void testUbuntu() throws Exception  {
		super.testUbuntu();
	}
	
	@Override
	public void doTest() throws Exception {
		
		installer = new ServiceInstaller(getRestUrl(), getServiceName());
		installer.recipePath(FOLDER_NAME);
        installer.setDisableSelfHealing(true);
		installer.install();
		
		String machinePrefix = null;

        LogUtils.log("Retrieving machine prefix for the installed service");
        Service service = ServiceReader.getServiceFromFile(new File(ScriptUtils.getBuildRecipesServicesPath() + "/" + getServiceFolder(), getServiceName() + "-service.groovy"));
		if (service.getIsolationSLA().getGlobal().isUseManagement()) {
			machinePrefix = getService().getCloud().getProvider().getManagementGroup();
		} else {
			machinePrefix = getService().getCloud().getProvider().getMachineNamePrefix();			
		}
        LogUtils.log("Machine prefix is " + machinePrefix);

		NodeMetadata node = computeHelper.getServerByName(machinePrefix + "1"); // what??
        LogUtils.log("NodeMetaData for server with prefix " + machinePrefix + " is " + node);

        LogUtils.log("Creating volume in location " + node.getLocation().getId());
		Volume details = storageHelper.createVolume(node.getLocation().getId(), 5, STORAGE_NAME);
        LogUtils.log("Volume created : " + details);

        LogUtils.log("Attaching volume with id " + details.getId() + " to service");
		installer.invoke("attachVolume " + details.getId() + " " + "/dev/xvdc");

        LogUtils.log("Checking volume with id " + details.getId() + " is really attached");
		Volume volume = storageHelper.getVolumeById(details.getId());
		AssertUtils.assertEquals("volume with id " + volume.getId() + " should have one attachement after invoking attachVolume", 1, volume.getAttachments().size());
        LogUtils.log("Volume attachment is " + volume.getAttachments());

        LogUtils.log("Detaching volume with id " + details.getId() + " from service");
		installer.invoke("detachVolume" + " " + details.getId());

        LogUtils.log("Checking volume with id " + details.getId() + " is really detached");
		volume = storageHelper.getVolumeById(details.getId());
		// volume should still exist
		AssertUtils.assertTrue("volume with id " + details.getId() + " should not have been deleted after calling detachVolume(delteOnExit = false)", volume != null);
		// though it should have no attachments
		AssertUtils.assertTrue("volume with id " + details.getId() + " should not have no attachments after calling detachVolume(delteOnExit = false)",
				volume.getAttachments() == null || volume.getAttachments().isEmpty());

        LogUtils.log("Deleting volume with id " + details.getId());
		storageHelper.deleteVolume(volume.getId());
		
		installer.uninstall();
	}
	
	@AfterMethod
	public void scanForLeakes() throws TimeoutException {
		super.scanForLeakedVolumes(STORAGE_NAME);
	}
	
	@AfterClass(alwaysRun = true)
	protected void teardown() throws Exception {
		super.teardown();
	}

	@Override
	protected boolean isReusableCloud() {
		return false;
	}

	@Override
	public String getServiceFolder() {
		return FOLDER_NAME;
	}

}
