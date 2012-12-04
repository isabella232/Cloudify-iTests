package framework.utils;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import test.cli.cloudify.CommandTestUtils;
import test.cli.cloudify.security.SecurityConstants;

public abstract class RecipeInstaller {
	
	private String restUrl;
	private String recipePath;
	private boolean disableSelfHealing = false;
	private Map<String, Object> overrideProperties;
	private Map<String, Object> cloudOverrideProperties;
	private long timeoutInMinutes;
	private boolean waitForFinish = true;
	private boolean expectToFail = false;
	private String cloudifyUsername;
	private String cloudifyPassword;
	private String authGroups;
	

	public RecipeInstaller(final String restUrl, int timeout) {
		this.restUrl = restUrl;
		this.timeoutInMinutes = timeout;
	}
	
	public boolean isDisableSelfHealing() {
		return disableSelfHealing;
	}

	public RecipeInstaller setDisableSelfHealing(boolean disableSelfHealing) {
		this.disableSelfHealing = disableSelfHealing;
		return this;
	}
	
	public String getRestUrl() {
		return restUrl;
	}

	public RecipeInstaller restUrl(String restUrl) {
		this.restUrl = restUrl;
		return this;
	}

	public String getRecipePath() {
		return recipePath;
	}

	public RecipeInstaller recipePath(String recipePath) {
		this.recipePath = recipePath;
		return this;
	}

	public Map<String, Object> getOverrides() {
		return overrideProperties;
	}

	public RecipeInstaller overrides(Map<String, Object> overridesFilePath) {
		this.overrideProperties = overridesFilePath;
		return this;
	}

	public Map<String, Object> getCloudOverrides() {
		return cloudOverrideProperties;
	}

	public RecipeInstaller cloudOverrides(Map<String, Object> cloudOverridesFilePath) {
		this.cloudOverrideProperties = cloudOverridesFilePath;
		return this;
	}

	public long getTimeoutInMinutes() {
		return timeoutInMinutes;
	}

	public RecipeInstaller timeoutInMinutes(long timeoutInMinutes) {
		this.timeoutInMinutes = timeoutInMinutes;
		return this;
	}

	public boolean isWaitForFinish() {
		return waitForFinish;
	}

	public RecipeInstaller waitForFinish(boolean waitForFinish) {
		this.waitForFinish = waitForFinish;
		return this;
	}

	public boolean isExpectToFail() {
		return expectToFail;
	}

	public RecipeInstaller expectToFail(boolean expectToFail) {
		this.expectToFail = expectToFail;
		return this;
	}
	
	public RecipeInstaller cloudifyUsername(String cloudifyUsername) {
		this.cloudifyUsername = cloudifyUsername;
		return this;
	}

	public RecipeInstaller cloudifyPassword(String cloudifyPassword) {
		this.cloudifyPassword = cloudifyPassword;
		return this;
	}
	
	public String getCloudifyUsername() {
		return cloudifyUsername;
	}

	public String getCloudifyPassword() {
		return cloudifyPassword;
	}
	
	public String getAuthGroups() {
		return authGroups;
	}

	public RecipeInstaller authGroups(String authGroups) {
		this.authGroups = authGroups;
		return this;
	}

	public abstract String getInstallCommand();
	
	public abstract String getUninstallCommand();
	
	public abstract String getRecipeName();
	
	public abstract void assertInstall(String output);
	
	public abstract void assertUninstall(String output);
	
	public String install() throws IOException, InterruptedException {
		
		if (recipePath == null) {
			throw new IllegalStateException("recipe path cannot be null. please use setRecipePath before calling install");
		}
		
		StringBuilder connectCommandBuilder = new StringBuilder()
		.append("connect").append(" ");
		if (StringUtils.isNotBlank(cloudifyUsername) && StringUtils.isNotBlank(cloudifyPassword)){
			//TODO : More validations
			connectCommandBuilder.append("-user").append(" ")
			.append(cloudifyUsername).append(" ")
			.append("-password").append(" ")
			.append(cloudifyPassword).append(" ");
		}
		connectCommandBuilder.append(restUrl).append(";");
		
		StringBuilder commandBuilder = new StringBuilder()
				.append(getInstallCommand()).append(" ")
				.append("--verbose").append(" ")
				.append("-timeout").append(" ")
				.append(timeoutInMinutes).append(" ");
		
		if (disableSelfHealing) {
			commandBuilder.append("-disableSelfHealing").append(" ");
		}

		if (cloudOverrideProperties != null && !cloudOverrideProperties.isEmpty()) {	
			File cloudOverridesFile = IOUtils.createTempOverridesFile(cloudOverrideProperties);
			commandBuilder.append("-cloud-overrides").append(" ").append(cloudOverridesFile.getAbsolutePath().replace("\\", "/")).append(" ");
		}
		if (overrideProperties != null && !overrideProperties.isEmpty()) {
			File serviceOverridesFile = IOUtils.createTempOverridesFile(overrideProperties);
			commandBuilder.append("-overrides").append(" ").append(serviceOverridesFile.getAbsolutePath().replace("\\", "/")).append(" ");
		}
		if (authGroups != null && !authGroups.isEmpty()) {
			commandBuilder.append("-authGroups").append(" ").append(authGroups).append(" ");
		}
		
		commandBuilder.append(recipePath.replace('\\', '/'));
		final String installCommand = commandBuilder.toString();
		final String connectCommand = connectCommandBuilder.toString();
		if (expectToFail) {
			return CommandTestUtils.runCommandExpectedFail(connectCommand + installCommand);
		}
		if (waitForFinish) {
			String output = CommandTestUtils.runCommandAndWait(connectCommand + installCommand);
			assertInstall(output);
			return output;			
		}
		return CommandTestUtils.runCommand(connectCommand + installCommand);
	}
	
	public String uninstall() throws IOException, InterruptedException {
		String url = null;
		try {
			url = restUrl + "/service/dump/machines/?fileSizeLimit=50000000";
			DumpUtils.dumpMachines(restUrl, SecurityConstants.ALL_ROLES_USER_PWD, SecurityConstants.ALL_ROLES_USER_PWD);
		} catch (final Exception e) {
			LogUtils.log("Failed to create dump for this url - " + url, e);
		}

		StringBuilder connectCommandBuilder = new StringBuilder()
		.append("connect").append(" ");
				
		if (StringUtils.isNotBlank(cloudifyUsername) && StringUtils.isNotBlank(cloudifyPassword)){
			//TODO : More validations
			connectCommandBuilder.append("-user").append(" ")
			.append(cloudifyUsername).append(" ")
			.append("-password").append(" ")
			.append(cloudifyPassword).append(" ");
		}
		connectCommandBuilder.append(restUrl).append(";");
		
		final String uninstallCommand = new StringBuilder()
				.append(getUninstallCommand()).append(" ")
				.append("--verbose").append(" ")
				.append("-timeout").append(" ")
				.append(timeoutInMinutes).append(" ")
				.append(getRecipeName())
				.toString();
		
		final String connectCommand = connectCommandBuilder.toString();
		
		if (expectToFail) {
			return CommandTestUtils.runCommandExpectedFail(connectCommand + uninstallCommand);
		}
		if (waitForFinish) {
			String output = CommandTestUtils.runCommandAndWait(connectCommand + uninstallCommand);
			assertUninstall(output);
			return output;			
		}
		return CommandTestUtils.runCommand(connectCommand + uninstallCommand);
				
	}

}
