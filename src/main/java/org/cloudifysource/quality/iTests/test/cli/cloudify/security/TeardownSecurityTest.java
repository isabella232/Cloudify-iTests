package org.cloudifysource.quality.iTests.test.cli.cloudify.security;

import java.io.IOException;

import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
import org.cloudifysource.quality.iTests.test.cli.cloudify.AbstractSecuredLocalCloudTest;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.cloudifysource.quality.iTests.test.cli.cloudify.AbstractSecuredLocalCloudTest;
import org.cloudifysource.quality.iTests.framework.utils.AssertUtils;
import org.cloudifysource.quality.iTests.framework.utils.LocalCloudBootstrapper;

public class TeardownSecurityTest extends AbstractSecuredLocalCloudTest {
	
	private LocalCloudBootstrapper bootstrapper;
	
	@BeforeClass
	public void bootstrap() throws Exception {
		bootstrapper = new LocalCloudBootstrapper();
		bootstrapper.secured(true).securityFilePath(SecurityConstants.BUILD_SECURITY_FILE_PATH);
		bootstrapper.keystoreFilePath(SecurityConstants.DEFAULT_KEYSTORE_FILE_PATH).keystorePassword(SecurityConstants.DEFAULT_KEYSTORE_PASSWORD);
		super.bootstrap(bootstrapper);	
	}
	
	@AfterClass(alwaysRun = true)
	public void teardown() throws IOException, InterruptedException {
		super.teardown();
	}
	
	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT, enabled = true)
	public void testTeardown() throws Exception{
				
		teardownAndVerify(SecurityConstants.USER_PWD_CLOUD_ADMIN, SecurityConstants.USER_PWD_CLOUD_ADMIN, SecurityConstants.CLOUD_ADMIN_DESCRIPTIN, false);
		
		super.bootstrap(bootstrapper);		
		teardownAndVerify(SecurityConstants.USER_PWD_CLOUD_ADMIN_AND_APP_MANAGER, SecurityConstants.USER_PWD_CLOUD_ADMIN_AND_APP_MANAGER, SecurityConstants.CLOUD_ADMIN_AND_APP_MANAGER_DESCRIPTION, false);
		
		super.bootstrap(bootstrapper);
		teardownAndVerify(SecurityConstants.USER_PWD_APP_MANAGER, SecurityConstants.USER_PWD_APP_MANAGER, SecurityConstants.APP_MANAGER_DESCRIPTIN, true);

		teardownAndVerify(SecurityConstants.USER_PWD_APP_MANAGER_AND_VIEWER, SecurityConstants.USER_PWD_APP_MANAGER_AND_VIEWER, SecurityConstants.APP_MANAGER_AND_VIEWER_DESCRIPTIN, true);
		
		teardownAndVerify(SecurityConstants.USER_PWD_VIEWER, SecurityConstants.USER_PWD_VIEWER, SecurityConstants.VIEWER_DESCRIPTIN, true);
		
		teardownAndVerify(SecurityConstants.USER_PWD_NO_ROLE, SecurityConstants.USER_PWD_NO_ROLE, SecurityConstants.NO_ROLE_DESCRIPTIN, true);

	}

	private void teardownAndVerify(String user, String password, String userDescription ,boolean isExpectedToFail) throws Exception{
		bootstrapper.user(user).password(password).teardownExpectedToFail(isExpectedToFail);
		String output = super.teardown(bootstrapper);
		
		if(isExpectedToFail){			
			AssertUtils.assertTrue(userDescription + " succeeded to teardown", output.contains(TEARDOWN_ACCESS_DENIED_MESSAGE));
		}
		else{			
			AssertUtils.assertTrue(userDescription + " failed to teardown", output.contains(TEARDOWN_SUCCESSFULL_MESSAGE));
		}
	}
	
	

}
