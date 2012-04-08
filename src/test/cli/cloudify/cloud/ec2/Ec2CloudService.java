package test.cli.cloudify.cloud.ec2;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;

import test.cli.cloudify.cloud.AbstractCloudService;
import framework.tools.SGTestHelper;
import framework.utils.AssertUtils;
import framework.utils.IOUtils;
import framework.utils.ScriptUtils;

public class Ec2CloudService extends AbstractCloudService {

	private String cloudName = "ec2";
	private String user = "0VCFNJS3FXHYC7M6Y782";
	private String apiKey = "fPdu7rYBF0mtdJs1nmzcdA8yA/3kbV20NgInn4NO";
	private String pemFileName = "cloud-demo";
	
	@Override
	public void injectAuthenticationDetails() throws IOException {

		String cloudTestPath = (SGTestHelper.getSGTestRootDir() + "/apps/cloudify/cloud/" + cloudName).replace('\\', '/');
		String sshKeyPemName = pemFileName + ".pem";

		// cloud plugin should include recipe that includes secret key 
		File cloudPluginDir = new File(ScriptUtils.getBuildPath() , "tools/cli/plugins/esc/" + cloudName + "/");
		File originalCloudDslFile = new File(cloudPluginDir, cloudName + "-cloud.groovy");
		File backupCloudDslFile = new File(cloudPluginDir, cloudName + "-cloud.backup");

		
		// first make a backup of the original file
		FileUtils.copyFile(originalCloudDslFile, backupCloudDslFile);
		
		Map<String, String> propsToReplace = new HashMap<String,String>();
		propsToReplace.put("ENTER_USER", user);
		propsToReplace.put("ENTER_API_KEY", apiKey);
		propsToReplace.put("cloudify_agent_", this.machinePrefix + "cloudify_agent");
		propsToReplace.put("cloudify_manager", this.machinePrefix + "cloudify_manager");
		propsToReplace.put("ENTER_KEY_FILE", pemFileName + ".pem");
		propsToReplace.put("numberOfManagementMachines 1", "numberOfManagementMachines "  + numberOfManagementMachines);
		
		IOUtils.replaceTextInFile(originalCloudDslFile.getAbsolutePath(), propsToReplace);

		// upload dir needs to contain the sshKeyPem 
		File targetPem = new File(ScriptUtils.getBuildPath(), "tools/cli/plugins/esc/" + cloudName + "/upload/" + sshKeyPemName);
		FileUtils.copyFile(new File(cloudTestPath, sshKeyPemName), targetPem);
		AssertUtils.assertTrue("File not found", targetPem.isFile());
	}

	public void setCloudName(String cloudName) {
		this.cloudName = cloudName;
	}
	
	@Override
	public String getCloudName() {
		return cloudName;
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
		return "ec2-cloud-demo";
	}
}
