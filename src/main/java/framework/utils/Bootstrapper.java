package framework.utils;

import java.io.IOException;

import org.apache.commons.lang.StringUtils;

import test.cli.cloudify.CommandTestUtils;
import test.cli.cloudify.CommandTestUtils.ProcessResult;
import test.cli.cloudify.security.SecurityConstants;


public abstract class Bootstrapper {
	
	private int timeoutInMinutes;
	private String user = SecurityConstants.CLOUD_ADMIN_USER_PWD;
	private String password = SecurityConstants.CLOUD_ADMIN_USER_PWD;
	private String securityFilePath;
	private boolean force = true;
	
	public abstract String getBootstrapCommand();
		
	public abstract String getTeardownCommand();
	
	public abstract String getCustomOptions() throws Exception;
	
	public Bootstrapper(int timeoutInMinutes) {
		this.timeoutInMinutes = timeoutInMinutes;
	}
	
	public Bootstrapper setTimeoutInMinutes(int timeoutInMinutes) {
		this.timeoutInMinutes = timeoutInMinutes;
		return this;
	}
	
	public Bootstrapper setForce(boolean force) {
		this.force = force;
		return this;
	}
	
	public Bootstrapper setUser(String user) {
		this.user = user;
		return this;
	}

	public Bootstrapper setPassword(String password) {
		this.password = password;
		return this;
	}
	
	public Bootstrapper setSecurityFilePath(String securityFilePath) {
		this.securityFilePath = securityFilePath;
		return this;
	}

	public String getUser() {
		return user;
	}
	
	public String getPassword() {
		return password;
	}
	
	public String getSecurityFilePath() {
		return securityFilePath;
	}

	public ProcessResult bootstrap() throws Exception {
		StringBuilder builder = new StringBuilder();
		
		String[] bootstrapCommandParts = getBootstrapCommand().split(" ");
		String commandAndOptions = bootstrapCommandParts[0] + " " + getCustomOptions();
		
		builder
			.append(commandAndOptions).append(" ")
			.append("--verbose").append(" ")
			.append("-timeout").append(" ")
			.append(timeoutInMinutes).append(" ");
		
		if(StringUtils.isNotBlank(user)){
			builder.append("-user " + user + " ");
		}
		
		if(StringUtils.isNotBlank(password)){
			builder.append("-pwd " + password + " ");
		}
		
		if(StringUtils.isNotBlank(securityFilePath)){
			builder.append("-security " + securityFilePath + " ");
		}
		
		if (bootstrapCommandParts.length == 2) {
			// cloud bootstrap, append provider
			builder.append(bootstrapCommandParts[1]);
		} else {
			// localcloud bootstrap.
		}
		
		return CommandTestUtils.runCloudifyCommandAndWait(builder.toString());	
	}
	
	public ProcessResult teardown() throws IOException, InterruptedException {
		
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
		return CommandTestUtils.runCloudifyCommandAndWait(builder.toString());	
	}
}
