package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.ec2.dynamicstorage;

import org.cloudifysource.quality.iTests.framework.utils.AssertUtils;
import org.cloudifysource.quality.iTests.framework.utils.ServiceInstaller;
import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.services.ec2.Ec2CloudService;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class NonPrivilegedModeTest extends AbstractEc2OneServiceDynamicStorageTest {

	private static final String FOLDER_NAME = "non-sudo";
	private ServiceInstaller installer;
	
	private static final String EXPECTED_OUTPUT = "Cannot format when not running in privileged mode";
	
	@BeforeClass(alwaysRun = true)
	protected void bootstrap() throws Exception {
		super.bootstrap();
	}
	
	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 4, enabled = true)
	public void testLinux() throws Exception {
		super.testLinux();
	}
	
	@Override
	public void doTest() throws Exception {
		
		installer = new ServiceInstaller(getRestUrl(), getServiceName());
		installer.recipePath(FOLDER_NAME);
		installer.timeoutInMinutes(5);
		installer.setDisableSelfHealing(true);
		String installOutput = installer.install();		
		
		// the installation should not succeed because the user is not sudo
		// see src/main/resources/apps/USM/usm/dynamicstroage/non-sudo/groovy.service
		// so we expect the IllegalStateException to propagate to the CLI.
		AssertUtils.assertTrue("installation output should have contained '" + EXPECTED_OUTPUT + "'", installOutput.toLowerCase().contains(EXPECTED_OUTPUT.toLowerCase()));
		
		installer.uninstall();
		
	}
	
	@Override
	protected void customizeCloud() throws Exception {
		((Ec2CloudService)getService()).getAdditionalPropsToReplace().put("privileged true", "privileged false");
		
	}
	
	@AfterClass(alwaysRun = true)
	protected void teardown() throws Exception {
		super.teardown();
	}

	@Override
	public String getServiceFolder() {
		return FOLDER_NAME;
	}

	@Override
	protected boolean isReusableCloud() {
		return false;
	}	
}
