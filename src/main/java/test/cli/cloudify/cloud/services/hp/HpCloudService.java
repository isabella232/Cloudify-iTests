package test.cli.cloudify.cloud.services.hp;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;

import test.cli.cloudify.cloud.services.JcloudCloudService;
import framework.tools.SGTestHelper;
import framework.utils.IOUtils;

public class HpCloudService extends JcloudCloudService {

	private String tenant = "hpcloud@gigaspaces.com";
	private String user = "98173213380893";
	private String apiKey = "C5nobOW90bhnCmE5AQaLaJ0Ubd8UISPxGih";
	private String keyPair = "sgtest";

	private final String hardwareId = "az-2.region-a.geo-1/102";
	private final String linuxImageId = "az-2.region-a.geo-1/221";
	private final String securityGroup = "test";

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
		
		propsToReplace.put("numberOfManagementMachines 1", "numberOfManagementMachines "
				+ getNumberOfManagementMachines());

		IOUtils.replaceTextInFile(getPathToCloudGroovy(), propsToReplace);

		// add a pem file
		final String sshKeyPemName = this.keyPair + ".pem";
		final File fileToCopy =
				new File(SGTestHelper.getSGTestRootDir() + "/src/main/resources/apps/cloudify/cloud/" + getCloudName()
						+ "/"
						+ sshKeyPemName);

		final File targetLocation = new File(getPathToCloudFolder() + "/upload/");
		FileUtils.copyFileToDirectory(fileToCopy, targetLocation);
	}

}
