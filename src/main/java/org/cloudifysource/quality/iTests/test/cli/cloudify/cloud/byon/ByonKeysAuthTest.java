package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.byon;

import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

public class ByonKeysAuthTest extends AbstractByonCloudTest {
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void testBootstrap() throws Exception {
		super.bootstrap();
		super.teardown();
	}
	
	@Override
	protected void customizeCloud() throws Exception {
		super.customizeCloud();
		getService().setSudo(false);
		getService().getProperties().put("keyFile", "testKey.pem");
	}
	
	@Override
	protected void parseBootstrapOutput(String bootstrapOutput) throws Exception {
		assertTrue("Bootstrap did not use the specified key file for authentication", 
				bootstrapOutput.contains("Authentication succeeded (publickey)"));
	}
	
	@AfterClass(alwaysRun = true)
	protected void teardown() throws Exception {
		super.teardown();
	}

}
