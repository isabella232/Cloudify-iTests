package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.services.hpfolsom;

import iTests.framework.utils.IOUtils;
import org.apache.commons.io.FileUtils;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.services.JCloudsCloudService;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class HpFolsomCloudService extends JCloudsCloudService {
    private static final String HP_CERT_PROPERTIES = CREDENTIALS_FOLDER + "/cloud/hp-folsom/hp-folsom-cred.properties";

    private Properties certProperties = getCloudProperties(HP_CERT_PROPERTIES);

	private String tenant = certProperties.getProperty("tenant");
	private String user = certProperties.getProperty("user");
	private String apiKey = certProperties.getProperty("apiKey");
	private String keyPair = certProperties.getProperty("keyPair");

	private final String securityGroup = certProperties.getProperty("securityGroup");

	public HpFolsomCloudService() {
		super("hp-folsom");
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

    @Override
    public String getRegion() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
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

		getProperties().put(USER_PROP, this.user);
		getProperties().put(API_KEY_PROP, this.apiKey);
		getProperties().put(KEYFILE_PROP, this.keyPair + ".pem");
		getProperties().put(KEYPAIR_PROP, this.keyPair);
		getProperties().put("tenant", this.tenant);
		getProperties().put(SECURITY_GROUP_PROP, this.securityGroup);

		final Map<String, String> propsToReplace = new HashMap<String, String>();
		
		propsToReplace.put("cloudify-agent-", getMachinePrefix() + "cloudify-agent");
		propsToReplace.put("cloudify-manager", getMachinePrefix() + "cloudify-manager");
		propsToReplace.put("numberOfManagementMachines 1", "numberOfManagementMachines "
				+ getNumberOfManagementMachines());
		propsToReplace.put("fileTransfer org.cloudifysource.domain.cloud.FileTransferModes.SFTP",
				"fileTransfer org.cloudifysource.domain.cloud.FileTransferModes.SCP");
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

	@Override
	public boolean supportsComputeApi() {
		return true;
	}

	@Override
	public boolean supportsStorageApi() {
		return false;
	}

	@Override
	public boolean supportNetworkApi() {
		return false;
	}

}
