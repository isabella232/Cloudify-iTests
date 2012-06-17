package test.cli.cloudify.cloud.services.hp;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import test.cli.cloudify.cloud.services.AbstractCloudService;
import framework.tools.SGTestHelper;
import framework.utils.IOUtils;

public class HpCloudService extends AbstractCloudService {
	
	private static final String CLOUD_NAME = "openstack";
	private String tenant = "24912589714038";
	private String user = "98173213380893";
	private String apiKey = "C5nobOW90bhnCmE5AQaLaJ0Ubd8UISPxGih";
	private String pemFileName = "sgtest-hp";

	public HpCloudService(String uniqueName) {
		super(uniqueName, CLOUD_NAME);
	}
	
	public String getTenant() {
		return tenant;
	}

	public void setTenant(String tenant) {
		this.tenant = tenant;
	}
	
	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getApiKey() {
		return apiKey;
	}

	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

	public String getPemFileName() {
		return pemFileName;
	}

	public void setPemFileName(String pemFileName) {
		this.pemFileName = pemFileName;
	}



	@Override
	public void injectServiceAuthenticationDetails() throws IOException {
		
		Map<String, String> propsToReplace = new HashMap<String,String>();
		propsToReplace.put("ENTER_USER", user);
		propsToReplace.put("ENTER_API_KEY", apiKey);
		propsToReplace.put("cloudify_agent_", this.machinePrefix + "cloudify-agent");
		propsToReplace.put("cloudify_manager", this.machinePrefix + "cloudify-manager");
		propsToReplace.put("ENTER_KEY_FILE", pemFileName + ".pem");
		propsToReplace.put("ENTER_TENANT", tenant);
        propsToReplace.put("ENTER_KEY_PAIR_NAME", "sgtest");
		propsToReplace.put("numberOfManagementMachines 1", "numberOfManagementMachines "  + numberOfManagementMachines);
		propsToReplace.put("\"openstack.wireLog\": \"false\"", "\"openstack.wireLog\": \"true\"");
		
		IOUtils.replaceTextInFile(getPathToCloudGroovy(), propsToReplace);

		// add a pem file
		String sshKeyPemName = pemFileName + ".pem";
		File FileToCopy = new File(SGTestHelper.getSGTestRootDir() + "/apps/cloudify/cloud/" + getCloudName() + "/" + sshKeyPemName);
		File targetLocation = new File(getPathToCloudFolder() + "/upload/" + sshKeyPemName);
		Map<File, File> filesToReplace = new HashMap<File, File>();
		filesToReplace.put(targetLocation, FileToCopy);
		addFilesToReplace(filesToReplace);
	}
}
