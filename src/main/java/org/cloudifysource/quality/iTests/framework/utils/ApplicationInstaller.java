package org.cloudifysource.quality.iTests.framework.utils;

import iTests.framework.utils.LogUtils;
import junit.framework.Assert;
import org.cloudifysource.quality.iTests.test.cli.cloudify.CommandTestUtils;

public class ApplicationInstaller extends RecipeInstaller {

	private static final int DEFAULT_INSTALL_APPLICATION_TIMEOUT = 30;
	
	private String applicationName; 
	
	public ApplicationInstaller(String restUrl, String applicationName) {
		super(restUrl, DEFAULT_INSTALL_APPLICATION_TIMEOUT);
		this.applicationName = applicationName;
	}

	public ApplicationInstaller setApplicationName(String applicationName) {
		this.applicationName = applicationName;
		return this;
	}
	
	public void uninstallIfFound() {	
		if (getRestUrl() != null) {
			String command = connectCommand() + ";list-applications";
			String output;
			try {
				output = CommandTestUtils.runCommandAndWait(command);
				if (output.contains(applicationName + ".")) {
					uninstall();
				}
			} catch (final Exception e) {
				LogUtils.log(e.getMessage(), e);
				Assert.fail(e.getMessage());
			}
		}
	}
	
	public String getApplicationName() {
		return applicationName;
	}
}
