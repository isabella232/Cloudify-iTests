package framework.utils;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class CloudBootstrapper extends Bootstrapper {

	private static final int DEFAULT_BOOTSTRAP_CLOUD_TIMEOUT = 30;

	private String provider;
	private boolean noWebServices = false;
	private Map<String, Object> cloudOverrides;
	
	public CloudBootstrapper() {
		super(DEFAULT_BOOTSTRAP_CLOUD_TIMEOUT);
	}
	
	public CloudBootstrapper provider(String provider) {
		this.provider = provider;
		return this;
	}
	
	public String getProvider() {
		return provider;
	}
	
	public CloudBootstrapper noWebServices(boolean noWebServices) {
		this.noWebServices = noWebServices;
		return this;
	}
	
	public boolean isNoWebServices() {
		return noWebServices;
	}
	
	public CloudBootstrapper cloudOverrides(Map<String, Object> overrides) {
		this.cloudOverrides = overrides;
		return this;
	}

	@Override
	public String getCustomOptions() throws IOException{
		
		StringBuilder builder = new StringBuilder();
		if (noWebServices) {
			builder.append("-no-web-services ");
		}
		if (cloudOverrides != null && !cloudOverrides.isEmpty()) {
			File cloudOverridesFile = IOUtils.createTempOverridesFile(cloudOverrides);
			builder
				.append("-cloud-overrides").append(" ")
				.append(cloudOverridesFile.getAbsolutePath().replace("\\", "/"));
		}
		
		return builder.toString();
	}
}
