package test.cli.cloudify.cloud.services.hp;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import test.cli.cloudify.cloud.services.AbstractCloudService;
import test.cli.cloudify.cloud.services.tools.openstack.Node;
import test.cli.cloudify.cloud.services.tools.openstack.OpenstackClient;
import test.cli.cloudify.cloud.services.tools.openstack.OpenstackException;
import framework.tools.SGTestHelper;
import framework.utils.IOUtils;
import framework.utils.LogUtils;

public class HpCloudService extends AbstractCloudService {

	private String tenant = "24912589714038";
	private String user = "98173213380893";
	private String apiKey = "C5nobOW90bhnCmE5AQaLaJ0Ubd8UISPxGih";
	private String pemFileName = "sgtest-hp";
	private OpenstackClient openstackClient;

	public HpCloudService(final String uniqueName) {
		super(uniqueName, "openstack");
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

	public String getPemFileName() {
		return pemFileName;
	}

	public void setPemFileName(final String pemFileName) {
		this.pemFileName = pemFileName;
	}

	@Override
	public void injectServiceAuthenticationDetails()
			throws IOException {

		final Map<String, String> propsToReplace = new HashMap<String, String>();
		propsToReplace.put("ENTER_USER", user);
		propsToReplace.put("ENTER_API_KEY", apiKey);
		propsToReplace.put("cloudify_agent_", this.machinePrefix + "cloudify-agent");
		propsToReplace.put("cloudify_manager", this.machinePrefix + "cloudify-manager");
		propsToReplace.put("ENTER_KEY_FILE", pemFileName + ".pem");
		propsToReplace.put("ENTER_TENANT", tenant);
		propsToReplace.put("ENTER_KEY_PAIR_NAME", "sgtest");
		propsToReplace.put("numberOfManagementMachines 1", "numberOfManagementMachines " + numberOfManagementMachines);
		propsToReplace.put("\"openstack.wireLog\": \"false\"", "\"openstack.wireLog\": \"true\"");

		IOUtils.replaceTextInFile(getPathToCloudGroovy(), propsToReplace);

		// add a pem file
		final String sshKeyPemName = pemFileName + ".pem";
		final File FileToCopy =
				new File(SGTestHelper.getSGTestRootDir() + "/apps/cloudify/cloud/" + getCloudName() + "/"
						+ sshKeyPemName);
		final File targetLocation = new File(getPathToCloudFolder() + "/upload/" + sshKeyPemName);
		final Map<File, File> filesToReplace = new HashMap<File, File>();
		filesToReplace.put(targetLocation, FileToCopy);
		addFilesToReplace(filesToReplace);
	}

	@Override
	public void beforeBootstrap() {
		this.openstackClient = new OpenstackClient();
		openstackClient.setConfig(this.cloudConfiguration);

		String token = openstackClient.createAuthenticationToken();

		final String agentPrefix = this.cloudConfiguration.getProvider().getMachineNamePrefix();
		final String mgmtPrefix = this.cloudConfiguration.getProvider().getManagementGroup();

		List<Node> nodes;
		try {
			nodes = openstackClient.listServers(token);
		} catch (OpenstackException e) {
			throw new IllegalStateException("Failed to query openstack cloud for current servers", e);
		}

		for (Node node : nodes) {
			if (node.getStatus().equals(OpenstackClient.MACHINE_STATUS_ACTIVE)) {
				if (node.getName().startsWith(mgmtPrefix) || node.getName().startsWith(agentPrefix)) {
					throw new IllegalStateException("Before bootstrap, found an active node with name: "
							+ node.getName() + ". Details: " + node);
				}
			}
		}

	}

	@Override
	public boolean scanLeakedAgentNodes() {
		if(openstackClient == null) {
			LogUtils.log("Openstack client was not initialized, therefore a bootstrap never took place, and no scan is needed.");
			return true;
		}
		String token = openstackClient.createAuthenticationToken();

		final String agentPrefix = this.cloudConfiguration.getProvider().getMachineNamePrefix();

		return checkForLeakedNode(token, agentPrefix);

	}
	@Override
	public boolean scanLeakedAgentAndManagementNodes() {
		if(openstackClient == null) {
			LogUtils.log("Openstack client was not initialized, so no test was performed after teardown");
			return true;
		}
		String token = openstackClient.createAuthenticationToken();

		final String agentPrefix = this.cloudConfiguration.getProvider().getMachineNamePrefix();
		final String mgmtPrefix = this.cloudConfiguration.getProvider().getManagementGroup();
		
		final boolean result = checkForLeakedNode(token, agentPrefix, mgmtPrefix);
		this.openstackClient.close();
		return result;

	}

	private boolean checkForLeakedNode(String token, final String... prefixes) {
		List<Node> nodes;
		try {
			nodes = openstackClient.listServers(token);
		} catch (OpenstackException e) {
			throw new IllegalStateException("Failed to query openstack cloud for current servers", e);
		}

		List<Node> leakedNodes = new LinkedList<Node>();
		for (Node node : nodes) {
			if (node.getStatus().equals(OpenstackClient.MACHINE_STATUS_ACTIVE)) {
				for (String prefix : prefixes) {
					if (node.getName().startsWith(prefix)) {
						leakedNodes.add(node);
					}
				}

			}
		}

		if (leakedNodes.size() > 0) {
			LogUtils.log("Found leaking nodes in HP cloud after teardown");
			for (Node node : leakedNodes) {
				LogUtils.log("Shutting down: " + node);
				try {
					openstackClient.terminateServer(node.getId(), token, System.currentTimeMillis() + 60000);
				} catch (Exception e) {
					LogUtils.log("Failed to terminate HP openstack node: " + node.getId()
							+ ". This node may be leaking. Node details: " + node + ", Error was: " + e.getMessage(), e);
				}
			}
			return false;
		}
		
		return true;
	}
}
