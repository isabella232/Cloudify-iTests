package test.cli.cloudify.cloud.services.hp;

import framework.utils.IOUtils;
import org.apache.commons.io.FileUtils;
import test.cli.cloudify.cloud.services.JCloudsCloudService;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class HpCloudService extends JCloudsCloudService {
    private static final String HP_CERT_PROPERTIES = CREDENTIALS_FOLDER + "/cloud/hp/hp-cert.properties";

    private Properties certProperties = getCloudProperties(HP_CERT_PROPERTIES);

	private String tenant = certProperties.getProperty("tenant");
	private String user = certProperties.getProperty("user");
	private String apiKey = certProperties.getProperty("apiKey");
	private String keyPair = certProperties.getProperty("keyPair");

	private final String hardwareId = "az-2.region-a.geo-1/102";
	private final String linuxImageId = "az-2.region-a.geo-1/221";
	private final String securityGroup = certProperties.getProperty("securityGroup");

	public HpCloudService() {
		super("hp");
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
