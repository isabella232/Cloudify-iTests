package framework.utils;

import java.io.IOException;

import test.cli.cloudify.CommandTestUtils;


public abstract class Bootstrapper {
	
	private int timeoutInMinutes;
	private boolean force = true;
	
	public abstract String getBootstrapCommand();
	
	public abstract String getOptions();
	
	public abstract String getTeardownCommand();
	
	public void setTimeoutInMinutes(int timeoutInMinutes) {
		this.timeoutInMinutes = timeoutInMinutes;
	}
	
	public void setForce(boolean force) {
		this.force = force;
	}

	public void bootstrap() throws IOException, InterruptedException {
		StringBuilder builder = new StringBuilder();
		
		String[] bootstrapCommandParts = getBootstrapCommand().split(" ");
		String commandAndOptions = bootstrapCommandParts[0] + " " + getOptions();
		
		builder
			.append(commandAndOptions).append(" ")
			.append("--verbose").append(" ")
			.append("-timeout").append(" ")
			.append(timeoutInMinutes).append(" ");
		if (bootstrapCommandParts.length == 2) {
			// cloud bootstrap, append provider
			builder.append(bootstrapCommandParts[1]);
		} else {
			// localcloud bootstrap.
		}
		CommandTestUtils.runCommandAndWait(builder.toString());	
	}
	
	public void teardown() throws IOException, InterruptedException {
		
		String[] teardownCommandParts = getTeardownCommand().split(" ");
		String command = teardownCommandParts[0];

		
		StringBuilder builder = new StringBuilder();
		builder
			.append(command).append(" ");
		if (force) {
			builder.append("-force").append(" ");
		}
		if (teardownCommandParts.length == 2) {
			// cloud teardown, append provider
			builder.append(teardownCommandParts[1]);
		} else {
			// localcloud teardown.
		}
		CommandTestUtils.runCommandAndWait(builder.toString());	
	}
}
