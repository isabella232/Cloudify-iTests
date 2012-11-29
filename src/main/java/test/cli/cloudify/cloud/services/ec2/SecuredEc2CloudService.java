package test.cli.cloudify.cloud.services.ec2;

import framework.utils.CloudBootstrapper;
import test.cli.cloudify.security.SecuredCloudService;

public class SecuredEc2CloudService extends Ec2CloudService implements SecuredCloudService {
	
	private String cloudifyUsername;
	private String cloudifyPassword;
	private String securityConfigFile;

	public SecuredEc2CloudService(String username, String password, String securityConfigFile) {
		this.cloudifyUsername = username;
		this.cloudifyPassword = password;
		this.securityConfigFile = securityConfigFile;
		
		CloudBootstrapper bootstrapper = new CloudBootstrapper();
		bootstrapper.user(username).password(password).securityFilePath(securityConfigFile);
		setBootstrapper(bootstrapper);
	}

	public SecuredEc2CloudService() {
		cloudifyUsername = getBootstrapper().getUser();
		cloudifyPassword = getBootstrapper().getPassword();
	}

	public String getCloudifyUsername() {
		return cloudifyUsername;
	}

	public String getCloudifyPassword() {
		return cloudifyPassword;
	}

	public String getSecurityConfigFile() {
		return securityConfigFile;
	}
}
