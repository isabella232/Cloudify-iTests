package framework.utils;

import java.io.IOException;
import java.util.concurrent.TimeUnit;


import test.AbstractTest;
import test.cli.cloudify.CommandTestUtils;

public abstract class RecipeInstaller {
	
	private String restUrl;
	private String recipePath;
	private String overridesFilePath;
	private String cloudOverridesFilePath;
	private long timeoutInMinutes = TimeUnit.MILLISECONDS.toMinutes(AbstractTest.DEFAULT_TEST_TIMEOUT);
	private boolean waitForFinish = true;
	private boolean expectToFail = false;

	public RecipeInstaller(final String restUrl) {
		this.restUrl = restUrl;
	}
	
	public String getRestUrl() {
		return restUrl;
	}

	public void setRestUrl(String restUrl) {
		this.restUrl = restUrl;
	}

	public String getRecipePath() {
		return recipePath;
	}

	public void setRecipePath(String recipePath) {
		this.recipePath = recipePath;
	}

	public String getOverridesFilePath() {
		return overridesFilePath;
	}

	public void setOverridesFilePath(String overridesFilePath) {
		this.overridesFilePath = overridesFilePath;
	}

	public String getCloudOverridesFilePath() {
		return cloudOverridesFilePath;
	}

	public void setCloudOverridesFilePath(String cloudOverridesFilePath) {
		this.cloudOverridesFilePath = cloudOverridesFilePath;
	}

	public long getTimeoutInMinutes() {
		return timeoutInMinutes;
	}

	public void setTimeoutInMinutes(long timeoutInMinutes) {
		this.timeoutInMinutes = timeoutInMinutes;
	}

	public boolean isWaitForFinish() {
		return waitForFinish;
	}

	public void setWaitForFinish(boolean waitForFinish) {
		this.waitForFinish = waitForFinish;
	}

	public boolean isExpectToFail() {
		return expectToFail;
	}

	public void setExpectToFail(boolean expectToFail) {
		this.expectToFail = expectToFail;
	}
	
	public abstract String getInstallCommand();
	
	public abstract String getUninstallCommand();
	
	public abstract String getRecipeName();
	
	public abstract void assertInstall(String output);
	
	public abstract void assertUninstall(String output);
	
	public void install() throws IOException, InterruptedException {
		
		if (recipePath == null) {
			throw new IllegalStateException("recipe path cannot be null. please use setRecipePath before calling install");
		}
		
		final String connectCommand = "connect " + restUrl + ";";
		StringBuilder commandBuilder = new StringBuilder()
				.append(getInstallCommand()).append(" ")
				.append("--verbose").append(" ")
				.append("-timeout").append(" ")
				.append(timeoutInMinutes).append(" ");
		
		if (cloudOverridesFilePath != null && !cloudOverridesFilePath.isEmpty()) {
			commandBuilder.append("-cloud-overrides ").append(cloudOverridesFilePath).append(" ");
		}
		if (overridesFilePath != null && !overridesFilePath.isEmpty()) {
			commandBuilder.append("-overrides ").append(overridesFilePath).append(" ");
		}
		
		commandBuilder.append(recipePath.replace('\\', '/'));
		final String installCommand = commandBuilder.toString();
		if (expectToFail) {
			CommandTestUtils.runCommandExpectedFail(connectCommand + installCommand);
			return;
		}
		if (waitForFinish) {
			CommandTestUtils.runCommandAndWait(connectCommand + installCommand);
			return;			
		}
		String output = CommandTestUtils.runCommand(connectCommand + installCommand);
		assertInstall(output);
	}
	
	public void uninstall() throws IOException, InterruptedException {
		String url = null;
		try {
			url = restUrl + "/service/dump/machines/?fileSizeLimit=50000000";
			DumpUtils.dumpMachines(restUrl);
		} catch (final Exception e) {
			LogUtils.log("Failed to create dump for this url - " + url, e);
		}

		final String connectCommand = "connect " + restUrl + ";";
		final String installCommand = new StringBuilder()
				.append(getUninstallCommand()).append(" ")
				.append("--verbose").append(" ")
				.append("-timeout").append(" ")
				.append(timeoutInMinutes).append(" ")
				.append(getRecipeName())
				.toString();
		String output = CommandTestUtils.runCommandAndWait(connectCommand + installCommand);
		assertUninstall(output);
	}
}
