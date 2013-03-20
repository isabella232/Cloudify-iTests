package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.services.openstack;

import org.cloudifysource.quality.iTests.framework.utils.IOUtils;
import org.apache.commons.io.FileUtils;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.services.JCloudsCloudService;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class OpenstackCloudService extends JCloudsCloudService {
    private static final String OPENSTACK_CERT_PROPERTIES = CREDENTIALS_FOLDER + "/cloud/openstack/openstack-cred.properties";

    private Properties certProperties = getCloudProperties(OPENSTACK_CERT_PROPERTIES);

	private String tenant = certProperties.getProperty("tenant");
	private String user = certProperties.getProperty("user");
	private String apiKey = certProperties.getProperty("apiKey");
	private String keyPair = certProperties.getProperty("keyPair");
	private String securityGroup = certProperties.getProperty("securityGroup");

	private final String hardwareId = "RegionOne/2";
	private final String linuxImageId = "RegionOne/5dc4c72d-5d80-4572-ba46-24c16be72f34";
	private final String openstackUrl = "http://192.168.9.70:5000/v2.0";

	public OpenstackCloudService() {
		super("openstack");
	}

	public String getTenant() {
		return tenant;
	}

	public void setTenant(final String tenant) {
		this.tenant = tenant;
	}

	@Override
	public String getUser() {
		return user;
	}

	public void setUser(final String user) {
		this.user = user;
	}

	@Override
	public String getApiKey() {
		return apiKey;
	}

	public void setApiKey(final String apiKey) {
		this.apiKey = apiKey;
	}

	public String getKeyPair() {
		return keyPair;
	}

	public void setKeyPair(final String keyPair) {
		this.keyPair = keyPair;
	}

	@Override
	public void injectCloudAuthenticationDetails()
			throws IOException {

		getProperties().put("user", this.user);
		getProperties().put("apiKey", this.apiKey);
		getProperties().put("keyFile", this.keyPair + ".pem");
		getProperties().put("keyPair", this.keyPair);
		getProperties().put("tenant", this.tenant);
		getProperties().put("hardwareId", this.hardwareId);
		getProperties().put("linuxImageId", this.linuxImageId);
		getProperties().put("securityGroup", this.securityGroup);
		getProperties().put("openstackUrl", this.openstackUrl);

		final Map<String, String> propsToReplace = new HashMap<String, String>();
		
		propsToReplace.put("cloudify-agent-", getMachinePrefix() + "cloudify-agent");
		propsToReplace.put("cloudify-manager", getMachinePrefix() + "cloudify-manager");
		propsToReplace.put("numberOfManagementMachines 1", "numberOfManagementMachines "
				+ getNumberOfManagementMachines());
		propsToReplace.put("fileTransfer org.cloudifysource.dsl.cloud.FileTransferModes.SFTP",
				"fileTransfer org.cloudifysource.dsl.cloud.FileTransferModes.SCP");
		IOUtils.replaceTextInFile(getPathToCloudGroovy(), propsToReplace);

		// add a pem file
		final String sshKeyPemName = this.keyPair + ".pem";
		final File fileToCopy = new File(CREDENTIALS_FOLDER + "/cloud/" + getCloudName() + "/" + sshKeyPemName);
		final File targetLocation = new File(getPathToCloudFolder() + "/upload/");
		FileUtils.copyFileToDirectory(fileToCopy, targetLocation);
	}

	@Override
	public void addOverrides(Properties overridesProps) {
		// TODO Auto-generated method stub
		
	}

}
