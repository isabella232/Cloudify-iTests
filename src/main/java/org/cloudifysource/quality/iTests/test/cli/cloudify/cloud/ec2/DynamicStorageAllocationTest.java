package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.ec2;

import org.cloudifysource.esc.driver.provisioning.storage.aws.EbsStorageDriver;
import org.cloudifysource.quality.iTests.framework.utils.LogUtils;
import org.cloudifysource.quality.iTests.framework.utils.ServiceInstaller;
import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.NewAbstractCloudTest;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.services.ec2.Ec2CloudService;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class DynamicStorageAllocationTest extends NewAbstractCloudTest {
	
	private static final String PATH_TO_SERVICE = "";
	
	private static final String STORAGE_DRIVER_CLASS_NAME = "org.cloudifysource.pingstorage.PingableStorageDriver";
	
	@Override
	protected void customizeCloud() throws Exception {
		final String oldStorageDriverClassName = EbsStorageDriver.class.getName();
		((Ec2CloudService)getService()).getAdditionalPropsToReplace().put(toClassName(oldStorageDriverClassName),toClassName(STORAGE_DRIVER_CLASS_NAME));
		super.customizeCloud();
	}
	
	@Override
	protected String getCloudName() {
		return "ec2";
	}
	
	@BeforeClass(alwaysRun = true)
	protected void bootstrap() throws Exception {
		super.bootstrap();
	}
	
	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 4, enabled = false)
	public void testAllocationInCustomCommnad() throws Exception {
		
		// NOTE : this is a mock for now, until we have the implementation ready.
		// we just to see the the driver is accessible to the service file.
		
		ServiceInstaller installer = new ServiceInstaller(getRestUrl(), "groovy");
		installer.recipePath(PATH_TO_SERVICE);
		installer.install();
		
		// call storage allocation
		String output = installer.invoke("ping");
		LogUtils.log(output);
	}
	
	@AfterClass(alwaysRun = true)
	protected void teardown() throws Exception {
		super.teardown();
	}

	@Override
	protected boolean isReusableCloud() {
		return false;
	}

	public String toClassName(String className) {
		return "storageClassName \""+className+"\"";
	}
}
