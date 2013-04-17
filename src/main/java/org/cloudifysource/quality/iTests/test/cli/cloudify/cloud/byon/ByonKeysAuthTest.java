package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.byon;

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

}
