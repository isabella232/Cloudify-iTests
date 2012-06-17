package test.cli.cloudify.cloud.services.rackspace;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import test.cli.cloudify.cloud.services.AbstractCloudService;
import framework.utils.IOUtils;

public class RackspaceCloudService extends AbstractCloudService {
	
	private static final String CLOUD_NAME = "rsopenstack";
	private String user = "gsrackspace";
	private String apiKey = "1ee2495897b53409f4643926f1968c0c";
	private String tenant = "658142";

	public RackspaceCloudService(String uniqueName) {
		super(uniqueName, CLOUD_NAME);
	

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

	public String getTenant() {
		return tenant;
	}

	public void setTenant(String tenant) {
		this.tenant = tenant;
	}

	@Override
	public void injectServiceAuthenticationDetails() throws IOException {
	

		// cloud plugin should include recipe that includes secret key 
		/*File cloudPluginDir = new File(ScriptUtils.getBuildPath() , "tools/cli/plugins/esc/" + getCloudName() + "/");
		File originalCloudDslFile = new File(cloudPluginDir, getCloudName() + "-cloud.groovy");
		File backupCloudDslFile = new File(cloudPluginDir, getCloudName() + "-cloud.backup");

		// first make a backup of the original file
		FileUtils.copyFile(originalCloudDslFile, backupCloudDslFile);*/
		
		Map<String, String> propsToReplace = new HashMap<String,String>();
		propsToReplace.put("USER_NAME", user);
		propsToReplace.put("API_KEY", apiKey);
		propsToReplace.put("machineNamePrefix " + "\"agent\"", "machineNamePrefix " + '"' + this.machinePrefix + "cloudify-agent" + '"');
		propsToReplace.put("managementGroup " + "\"management\"", "managementGroup " + '"' + this.machinePrefix + "cloudify-manager" + '"');
		propsToReplace.put("ENTER_TENANT", tenant);
		propsToReplace.put("numberOfManagementMachines 1", "numberOfManagementMachines "  + numberOfManagementMachines);
		propsToReplace.put("\"openstack.wireLog\": \"false\"", "\"openstack.wireLog\": \"true\"");
		
		IOUtils.replaceTextInFile(getPathToCloudGroovy(), propsToReplace);
	}
	
	

}
