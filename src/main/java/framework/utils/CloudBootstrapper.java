package framework.utils;

public class CloudBootstrapper extends Bootstrapper {

	private String provider;
	
	public CloudBootstrapper(final String provider) {
		this.provider = provider;
	}
	
	@Override
	public String getBootstrapCommand() {
		return "bootstrap-cloud " + provider;
	}

	@Override
	public String getOptions() {
		return null;
	}

	@Override
	public String getTeardownCommand() {
		return "teardown-cloud " + provider;
	}
	
	

}
