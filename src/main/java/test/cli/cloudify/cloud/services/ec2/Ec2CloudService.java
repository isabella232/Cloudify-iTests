package test.cli.cloudify.cloud.services.ec2;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;

import test.cli.cloudify.cloud.services.JcloudCloudService;
import framework.tools.SGTestHelper;
import framework.utils.IOUtils;
import framework.utils.LogUtils;

public class Ec2CloudService extends JcloudCloudService {

	public Ec2CloudService() {
		super("ec2");
	}

	public Ec2CloudService(final String cloudName) {
		super(cloudName);
	}

	public static final String DEFAULT_US_EAST_LINUX_AMI = "us-east-1/ami-76f0061f";
	public static final String DEFAULT_US_EAST_UBUNTU_AMI = "us-east-1/ami-82fa58eb";
	public static final String DEFAULT_EU_WEST_LINUX_AMI = "eu-west-1/ami-c37474b7";
	public static final String DEFAULT_EU_WEST_UBUNTU_AMI = "eu-west-1/ami-c1aaabb5";

	public static final String DEFAULT_EC2_LINUX_AMI_USERNAME = "ec2-user";
	public static final String DEFAULT_EC2_UBUNTU_AMI_USERNAME = "ubuntu";

	private String user = "AKIAI4OVPQZZQT53O6SQ";
	private String apiKey = "xI/BDTPh0LE9PcC0aHhn5GEUh+/hjOiRcKwCNVP5";
	private String keyPair = "ec2-sgtest";

	public void setUser(final String user) {
		this.user = user;
	}

	@Override
	public String getUser() {
		return user;
	}

	public void setApiKey(final String apiKey) {
		this.apiKey = apiKey;
	}

	@Override
	public String getApiKey() {
		return apiKey;
	}

	public String getRegion() {
		return System.getProperty("ec2.region", "us-east-1");
	}

	public void setRegion(final String region) {
		System.setProperty("ec2.region", region);
	}

	public void setKeyPair(final String keyPair) {
		this.keyPair = keyPair;
	}

	public String getKeyPair() {
		return keyPair;
	}

	@Override
	public void injectCloudAuthenticationDetails()
			throws IOException {

		getProperties().put("user", this.user);
		getProperties().put("apiKey", this.apiKey);

		final Map<String, String> propsToReplace = new HashMap<String, String>();

		if (getRegion().contains("eu")) {
			LogUtils.log("Working in eu region");
			getProperties().put("locationId", "eu-west-1");
			setKeyPair("sgtest-eu");
			if (!getCloudName().contains("win")) {
				getProperties().put("linuxImageId", DEFAULT_EU_WEST_LINUX_AMI);
				getProperties().put("ubuntuImageId", DEFAULT_EU_WEST_UBUNTU_AMI);
				getProperties().put("hardwareId", "m1.small");
			}
		} else {
			getProperties().put("locationId", "us-east-1");
			if (!getCloudName().contains("win")) {
				getProperties().put("linuxImageId", DEFAULT_US_EAST_LINUX_AMI);
				getProperties().put("ubuntuImageId", DEFAULT_US_EAST_UBUNTU_AMI);
				getProperties().put("hardwareId", "m1.small");
			}
		}
		getProperties().put("keyPair", this.keyPair);
		getProperties().put("keyFile", this.keyPair + ".pem");

		propsToReplace.put("cloudify_agent_", getMachinePrefix() + "cloudify-agent");
		propsToReplace.put("cloudify_manager", getMachinePrefix() + "cloudify-manager");
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
