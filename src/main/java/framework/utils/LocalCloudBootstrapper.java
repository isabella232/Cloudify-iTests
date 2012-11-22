package framework.utils;

public class LocalCloudBootstrapper extends Bootstrapper {

	@Override
	public String getBootstrapCommand() {
		return "bootstrap-localcloud";
	}

	@Override
	public String getOptions() {
		// TODO Auto-generated method stub
		return null;
	}
	

	@Override
	public String getTeardownCommand() {
		return "teardown-localcloud";
	}
}
