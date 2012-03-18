package test.cli.cloudify.cloud.rackspace;

import java.io.IOException;

import test.cli.cloudify.cloud.AbstractCloudService;

public class RackspaceCloudService extends AbstractCloudService {
	
	private static RackspaceCloudService self = null;
	private static final String cloudName = "rackspace";
	private static final String user = "gsrackspace";
	private static final String apiKey = "9a403ae461f8e5210d5bc435761aeb37";
	private static final String tenant = "658142";

	private RackspaceCloudService() {};

	public static RackspaceCloudService getService() {
		if (self == null) {
			self = new RackspaceCloudService();
		}
		return self;	
	}
	
	@Override
	public String getCloudName() {
		return cloudName;
	}

	public String getUser() {
		return user;
	}

	public String getApiKey() {
		return apiKey;
	}

	@Override
	public void injectAuthenticationDetails() throws IOException {
		// TODO Auto-generated method stub
		
	}
	
	

}
