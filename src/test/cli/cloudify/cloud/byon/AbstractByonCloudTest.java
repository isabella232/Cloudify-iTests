package test.cli.cloudify.cloud.byon;

import test.cli.cloudify.cloud.NewAbstractCloudTest;
import test.cli.cloudify.cloud.services.byon.ByonCloudService;

public class AbstractByonCloudTest extends NewAbstractCloudTest {

	public ByonCloudService getService() {
		return (ByonCloudService) super.getService();
	}
	
	@Override
	protected void customizeCloud() throws Exception {
	}

	@Override
	protected String getCloudName() {
		return "byon";
	}

	@Override
	protected boolean isReusableCloud() {
		return false;
	}
}
