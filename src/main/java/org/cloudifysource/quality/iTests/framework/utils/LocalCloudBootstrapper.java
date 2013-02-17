package org.cloudifysource.quality.iTests.framework.utils;

public class LocalCloudBootstrapper extends Bootstrapper {

	private static final int DEFAULT_BOOTSTRAP_LOCAL_CLOUD_TIMEOUT = 15;
	
	public LocalCloudBootstrapper() {
		super(DEFAULT_BOOTSTRAP_LOCAL_CLOUD_TIMEOUT);
	}

	@Override
	public String getCustomOptions() throws Exception {
		return "";
	}
}
