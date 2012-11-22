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
	
	@Override
	public String getInstallCommand() {
		return "install-service";
	}

	@Override
	public void assertInstall(String output) {
		final String excpectedResult = "Service \"" + serviceName + "\" successfully installed";
		AssertUtils.assertTrue("output " + output + " Does not contain " + excpectedResult,
				output.toLowerCase().contains(excpectedResult.toLowerCase()));
	}

	@Override
	public String getUninstallCommand() {
		return "uninstall-service";
	}

	@Override
	public String getRecipeName() {
		return serviceName;
	}

	@Override
	public void assertUninstall(String output) {
		final String excpectedResult = "Service \"" + serviceName + "\" uninstalled successfully";
		AssertUtils.assertTrue(output.toLowerCase().contains(excpectedResult.toLowerCase()));
	}
	
	public void uninstallIfFound() {	
		if (getRestUrl() != null) {
			String command = "connect " + getRestUrl() + ";list-services";
			String output;
			try {
				output = CommandTestUtils.runCommandAndWait(command);
				if (output.contains(serviceName)) {
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
			String command = "connect " + getRestUrl() + ";set-instances " + serviceName + " " + numberOfInstances;
			try {
				CommandTestUtils.runCommandAndWait(command);
			} catch (final Exception e) {
				LogUtils.log(e.getMessage(), e);
				Assert.fail(e.getMessage());
			}
		}
		
	}
}
