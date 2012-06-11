package test.cli.cloudify.cloud.services.terremark;

import java.io.IOException;

import test.cli.cloudify.cloud.services.AbstractCloudService;

public class TerremarkCloudService extends AbstractCloudService {

	private static final String TERREMARK = "terremark";
	private static TerremarkCloudService self = null;

	private TerremarkCloudService(String uniqueName) {
		super(uniqueName, TERREMARK);
	};

	public static TerremarkCloudService getService(String uniqueName) {
		if (self == null) {
			self = new TerremarkCloudService(uniqueName);
		}
		return self;	
	}
	
	@Override
	public String getCloudName() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getUser() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getApiKey() {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public void injectServiceAuthenticationDetails() throws IOException {
		// TODO Auto-generated method stub
		
	}
	
	
}
