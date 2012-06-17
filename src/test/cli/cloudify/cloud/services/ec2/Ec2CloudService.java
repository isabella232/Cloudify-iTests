package test.cli.cloudify.cloud.services.ec2;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import test.cli.cloudify.cloud.services.AbstractCloudService;
import framework.tools.SGTestHelper;
import framework.utils.IOUtils;

public class Ec2CloudService extends AbstractCloudService {

	private static final String EC2_CLOUD_NAME = "ec2";
	private String user = "0VCFNJS3FXHYC7M6Y782";
	private String apiKey = "fPdu7rYBF0mtdJs1nmzcdA8yA/3kbV20NgInn4NO";
	private String pemFileName = "cloud-demo";
	
	public Ec2CloudService(String uniqueName) {
		super(uniqueName, EC2_CLOUD_NAME);
	}
	
	@Override
	public void injectServiceAuthenticationDetails() throws IOException {

		Map<String, String> propsToReplace = new HashMap<String,String>();
		propsToReplace.put("ENTER_USER", user);
		propsToReplace.put("ENTER_API_KEY", apiKey);
		propsToReplace.put("cloudify_agent_", this.machinePrefix + "cloudify-agent");
		propsToReplace.put("cloudify_manager", this.machinePrefix + "cloudify-manager" 
											+ Long.toString(System.currentTimeMillis()));
		propsToReplace.put("ENTER_KEY_FILE", getPemFileName() + ".pem");
        propsToReplace.put("ENTER_KEY_PAIR_NAME", getPemFileName());
		propsToReplace.put("numberOfManagementMachines 1", "numberOfManagementMachines "  + numberOfManagementMachines);
		
		IOUtils.replaceTextInFile(getPathToCloudGroovy(), propsToReplace);
		
		// add a pem file
		String sshKeyPemName = pemFileName + ".pem";
		File FileToCopy = new File(SGTestHelper.getSGTestRootDir() + "/apps/cloudify/cloud/" + getCloudName() + "/" + sshKeyPemName);
		File targetLocation = new File(getPathToCloudFolder() + "/upload/" + sshKeyPemName);
		Map<File, File> filesToReplace = new HashMap<File, File>();
		filesToReplace.put(targetLocation, FileToCopy);
		addFilesToReplace(filesToReplace);
	}
	
	public void setUser(String user) {
		this.user = user;
	}

	public String getUser() {
		return user;
	}
	
	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

	public String getApiKey() {
		return apiKey;
	}

	public void setPemFileName(String pemFileName) {
		this.pemFileName = pemFileName;
	}
	
	public String getPemFileName() {
		return pemFileName;
	}
}
