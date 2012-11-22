package framework.utils;

public class LocalCloudBootstrapper extends Bootstrapper {

	private static final int DEFAULT_BOOTSTRAP_LOCAL_CLOUD_TIMEOUT = 15;
	
	public LocalCloudBootstrapper() {
		super(DEFAULT_BOOTSTRAP_LOCAL_CLOUD_TIMEOUT);
	}

	@Override
	public String getBootstrapCommand() {
		return "bootstrap-localcloud";
	}

	@Override
	public String getOptions() {
		return null;
	}
	

	@Override
	public String getTeardownCommand() {
		return "teardown-localcloud";
	}
}
