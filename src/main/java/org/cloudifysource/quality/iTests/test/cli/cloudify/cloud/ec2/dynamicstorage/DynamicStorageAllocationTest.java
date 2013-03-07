package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.ec2.dynamicstorage;

import java.util.concurrent.TimeoutException;

import org.cloudifysource.quality.iTests.framework.utils.AssertUtils;
import org.cloudifysource.quality.iTests.framework.utils.ServiceInstaller;
import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
import org.jclouds.ec2.domain.Volume;
import org.jclouds.ec2.domain.Volume.Status;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class DynamicStorageAllocationTest extends AbstractDynamicStorageTest {
	
	private static final String FOLDER_NAME = "create-and-attach";
	private ServiceInstaller installer;
		
	@Override
	protected String getCloudName() {
		return "ec2";
	}
	
	@BeforeClass(alwaysRun = true)
	protected void bootstrap() throws Exception {
		super.bootstrap();
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
		
		// the install should have created and attached a volume with a name prefix of the class name. see customizeCloud below.
		Volume ourVolume = storageHelper.getVolumeByName(System.getProperty("user.name") + "-" + this.getClass().getSimpleName().toLowerCase());
		
		AssertUtils.assertNotNull("could not find the required volume after install service", ourVolume);
		// also check it is attached.
		AssertUtils.assertEquals("the volume should have one attachements", 1, ourVolume.getAttachments().size());
		
		installer.uninstall();	
		
		final String volId = ourVolume.getId();
		
		// after uninstall the volume should be deleted
		ourVolume = storageHelper.getVolumeById(ourVolume.getId());
		AssertUtils.assertTrue("volume with id " + volId + " was not deleted after uninstall", ourVolume == null || ourVolume.getStatus().equals(Status.DELETING));
	}	
	
	@AfterMethod
	public void scanForLeakes() throws TimeoutException {
		super.scanForLeakedVolumesCreatedViaTemplate("SMALL_BLOCK");
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
