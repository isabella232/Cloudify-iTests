package test.cli.cloudify.cloud.ec2;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import test.cli.cloudify.cloud.NewAbstractSecurityCloudTest;
import test.cli.cloudify.security.SecurityConstants;
import framework.utils.AssertUtils;
import framework.utils.CloudBootstrapper;

public class Ec2TeardownAfterInstallSecurityTest extends NewAbstractSecurityCloudTest{
	
	@BeforeClass
	public void bootstrap() throws Exception {
		super.bootstrap();	
	}
	
	@AfterClass(alwaysRun = true)
	public void teardown() throws Exception {
		super.teardown();
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void teardownWithInstallTest() throws Exception{
				
		teardownAndVerify(SecurityConstants.USER_PWD_APP_MANAGER, SecurityConstants.USER_PWD_APP_MANAGER, SecurityConstants.APP_MANAGER_DESCRIPTIN, true);

		teardownAndVerify(SecurityConstants.USER_PWD_CLOUD_ADMIN_AND_APP_MANAGER, SecurityConstants.USER_PWD_CLOUD_ADMIN_AND_APP_MANAGER, SecurityConstants.CLOUD_ADMIN_AND_APP_MANAGER_DESCRIPTION, false);
		
	}

	private void teardownAndVerify(String user, String password, String userDescription ,boolean isExpectedToFail) throws Exception{
		CloudBootstrapper bootstrapper = (CloudBootstrapper) getService().getBootstrapper().user(user).password(password).teardownExpectedToFail(true);
		super.teardown(bootstrapper);
		String output = bootstrapper.getLastActionOutput();
	
		if(isExpectedToFail){			
			AssertUtils.assertTrue(userDescription + " succeeded to teardown", output.contains(TEARDOWN_ACCESS_DENIED_MESSAGE));
		}
		else{			
			AssertUtils.assertTrue(userDescription + " failed to teardown", output.contains(TEARDOWN_SUCCESSFULL_MESSAGE));
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
