package framework.utils;

import junit.framework.Assert;
import test.cli.cloudify.CommandTestUtils;

public class ServiceInstaller extends RecipeInstaller {

	private static final int DEFAULT_INSTALL_SERVICE_TIMEOUT = 15;
	
	private String serviceName;
	
	public ServiceInstaller(String restUrl, String serviceName) {
		super(restUrl, DEFAULT_INSTALL_SERVICE_TIMEOUT);
		this.serviceName = serviceName;
	}
	
	public void uninstallIfFound() {	
		if (getRestUrl() != null) {
			String command = connectCommand() + ";list-services";
			String output;
			try {
				output = CommandTestUtils.runCommandAndWait(command);
				if (output.contains("." + serviceName)) {
					uninstall();
				}
			} catch (final Exception e) {
				LogUtils.log(e.getMessage(), e);
				Assert.fail(e.getMessage());
			}
		}
	}
	
	public void setInstances(int numberOfInstances) {
		
		if (getRestUrl() != null) {
			String command = connectCommand() + ";set-instances " + serviceName + " " + numberOfInstances;
			try {
				CommandTestUtils.runCommandAndWait(command);
			} catch (final Exception e) {
				LogUtils.log(e.getMessage(), e);
				Assert.fail(e.getMessage());
			}
		}
		
	}

	public String getServiceName() {
		return serviceName;
	}
}
