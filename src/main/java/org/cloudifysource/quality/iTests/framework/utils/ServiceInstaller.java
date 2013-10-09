package org.cloudifysource.quality.iTests.framework.utils;

import java.io.IOException;

import iTests.framework.utils.LogUtils;
import junit.framework.Assert;
import org.cloudifysource.quality.iTests.test.cli.cloudify.CommandTestUtils;

public class ServiceInstaller extends RecipeInstaller {

	private static final int DEFAULT_INSTALL_SERVICE_TIMEOUT = 15;
	
	private String serviceName;
	
	public ServiceInstaller(String restUrl, String serviceName) {
		super(restUrl, DEFAULT_INSTALL_SERVICE_TIMEOUT);
		this.serviceName = serviceName;
	}

    public ServiceInstaller(String restUrl, String serviceName, String applicationName) {
        super(restUrl, DEFAULT_INSTALL_SERVICE_TIMEOUT, applicationName);
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
	
	public String invoke(final String command, final String... params) throws IOException, InterruptedException {
		String text = connectCommand() + ";invoke " + serviceName + " " + command;
		for (String param : params) {
			text = text + " " + param;
		}
		return CommandTestUtils.runCommandAndWait(text);
	}
	
	public String invoke(final String command, int instanceId) throws IOException, InterruptedException {
		return CommandTestUtils.runCommandAndWait(connectCommand() + ";invoke -instanceid " + instanceId + " " + serviceName + " " + command);
	}
}
