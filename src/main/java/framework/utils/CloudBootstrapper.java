package framework.utils;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class CloudBootstrapper extends Bootstrapper {

	private String provider;
	private boolean noWebServices = false;
	private Map<String, Object> cloudOverrides;
	private static final int DEFAULT_BOOTSTRAP_CLOUD_TIMEOUT = 30;
	
	public CloudBootstrapper(final String provider) {
		super(DEFAULT_BOOTSTRAP_CLOUD_TIMEOUT);
		this.provider = provider;
	}
	
	public void setNoWebServices(boolean noWebServices) {
		this.noWebServices = noWebServices;
	}
	
	@Override
	public String getBootstrapCommand() {
		return "bootstrap-cloud " + provider;
	}

	@Override
	public String getOptions() throws IOException {
				
		StringBuilder builder = new StringBuilder();
		if (noWebServices) {
			builder.append("-no-web-services");
		}
		if (cloudOverrides != null && !cloudOverrides.isEmpty()) {
			File cloudOverridesFile = IOUtils.createTempOverridesFile(cloudOverrides);
			builder
				.append("--cloud-overrides").append(" ")
				.append(cloudOverridesFile.getAbsolutePath().replace("\\", "/")).append(" ");
		}
		return builder.toString();
	}

	@Override
	public String getTeardownCommand() {
		return "teardown-cloud " + provider;
	}
}
