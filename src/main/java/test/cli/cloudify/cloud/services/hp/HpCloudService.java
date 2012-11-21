package test.cli.cloudify.cloud.services.hp;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.cloudifysource.esc.driver.provisioning.openstack.Node;
import org.cloudifysource.esc.driver.provisioning.openstack.OpenstackException;

import test.cli.cloudify.cloud.services.AbstractCloudService;
import test.cli.cloudify.cloud.services.tools.openstack.OpenstackClient;
import framework.tools.SGTestHelper;
import framework.utils.IOUtils;
import framework.utils.LogUtils;

public class HpCloudService extends AbstractCloudService {

	private String tenant = "24912589714038";
	private String user = "98173213380893";
	private String apiKey = "C5nobOW90bhnCmE5AQaLaJ0Ubd8UISPxGih";
	private String keyPair = "sgtest";
	private OpenstackClient openstackClient;

	public HpCloudService() {
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

		final Map<String, String> propsToReplace = new HashMap<String, String>();
		
		propsToReplace.put("cloudify_agent_", getMachinePrefix() + "cloudify-agent");
		propsToReplace.put("cloudify_manager", getMachinePrefix() + "cloudify-manager");
		propsToReplace.put("numberOfManagementMachines 1", "numberOfManagementMachines " + getNumberOfManagementMachines());
		propsToReplace.put("\"openstack.wireLog\": \"false\"", "\"openstack.wireLog\": \"true\"");

		IOUtils.replaceTextInFile(getPathToCloudGroovy(), propsToReplace);

		// add a pem file
		final String sshKeyPemName = this.keyPair + ".pem";
		final File fileToCopy =
				new File(SGTestHelper.getSGTestRootDir() + "/src/main/resources/apps/cloudify/cloud/" + getCloudName() + "/"
						+ sshKeyPemName);
		final File targetLocation = new File(getPathToCloudFolder() + "/upload/");
		FileUtils.copyFileToDirectory(fileToCopy, targetLocation);
	}

	@Override
	public boolean scanLeakedAgentNodes() {
		if (openstackClient == null) {
			this.openstackClient = createClient();
		}
		String token = openstackClient.createAuthenticationToken();

		final String agentPrefix = getCloud().getProvider().getMachineNamePrefix();

		return checkForLeakedNode(token, agentPrefix);

	}
	@Override
	public boolean scanLeakedAgentAndManagementNodes() {
		if(openstackClient == null) {
			openstackClient = createClient();
		}
		String token = openstackClient.createAuthenticationToken();

		final String agentPrefix = getCloud().getProvider().getMachineNamePrefix();
		final String mgmtPrefix = getCloud().getProvider().getManagementGroup();
		
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
	
	private OpenstackClient createClient() {
		OpenstackClient client = new OpenstackClient();
		client.setConfig(getCloud());
		return client;
		
	}
}
