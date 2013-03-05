package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.ec2.dynamicstorage;

import java.io.File;
import java.util.concurrent.TimeoutException;

import org.cloudifysource.dsl.Service;
import org.cloudifysource.dsl.internal.ServiceReader;
import org.cloudifysource.quality.iTests.framework.utils.AssertUtils;
import org.cloudifysource.quality.iTests.framework.utils.Ec2ComputeApiHelper;
import org.cloudifysource.quality.iTests.framework.utils.ScriptUtils;
import org.cloudifysource.quality.iTests.framework.utils.ServiceInstaller;
import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.ec2.domain.Volume;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class DynamicStorageAttachmentTest extends AbstractDynamicStorageTest {
	
	private static final String FOLDER_NAME = "attach-only";
	
	private static final String STORAGE_NAME = System.getProperty("user.name") + "-" + DynamicStorageAttachmentTest.class.getSimpleName();
	
	private ServiceInstaller installer;
	private Ec2ComputeApiHelper computeHelper;


	@Override
	protected String getCloudName() {
		return "ec2";
	}
	
	@BeforeClass(alwaysRun = true)
	protected void bootstrap() throws Exception {
		super.bootstrap();
		this.computeHelper = new Ec2ComputeApiHelper(getService().getCloud());

	}
	
	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 4, enabled = true)
	public void testLinux() throws Exception {
		super.testLinux();
	}
	
	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 4, enabled = false)
	public void testUbuntu() throws Exception  {
		super.testUbuntu();
	}
	
	@Override
	public void doTest() throws Exception {
		
		installer = new ServiceInstaller(getRestUrl(), SERVICE_NAME);
		installer.recipePath(FOLDER_NAME);
		installer.install();
		
		String machinePrefix = null;
		
		Service service = ServiceReader.getServiceFromFile(new File(ScriptUtils.getBuildRecipesServicesPath() + "/" + getServiceFolder(), SERVICE_NAME + "-service.groovy"));
		if (service.getIsolationSLA().getGlobal().isUseManagement()) {
			machinePrefix = getService().getCloud().getProvider().getManagementGroup();
		} else {
			machinePrefix = getService().getCloud().getProvider().getMachineNamePrefix();			
		}
		
		NodeMetadata node = computeHelper.getServerByName(machinePrefix + "1"); // what??
				
		Volume details = storageHelper.createVolume(node.getLocation().getId(), 5, STORAGE_NAME);
		
		installer.invoke("attachVolume " + details.getId() + " " + "/dev/sdc");
		
		Volume volume = storageHelper.getVolumeById(details.getId());
		AssertUtils.assertEquals("volume with id " + volume.getId() + " should have one attachement after invoking attachVolume", 1, volume.getAttachments().size());
		
		installer.invoke("detachVolume" + " " + details.getId());
		
		volume = storageHelper.getVolumeById(details.getId());
		// volume should still exist
		AssertUtils.assertTrue("volume with id " + details.getId() + " should not have been deleted after calling detachVolume(delteOnExit = false)", volume != null);
		// though it should have no attachments
		AssertUtils.assertTrue("volume with id " + details.getId() + " should not have no attachements after calling detachVolume(delteOnExit = false)", 
				volume.getAttachments() == null || volume.getAttachments().isEmpty());
		
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
