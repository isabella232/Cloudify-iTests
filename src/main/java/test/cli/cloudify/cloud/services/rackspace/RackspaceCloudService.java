package test.cli.cloudify.cloud.services.rackspace;

import framework.utils.IOUtils;
import framework.utils.LogUtils;
import org.cloudifysource.esc.driver.provisioning.openstack.Node;
import org.cloudifysource.esc.driver.provisioning.openstack.OpenstackException;
import test.cli.cloudify.cloud.services.AbstractCloudService;
import test.cli.cloudify.cloud.services.tools.openstack.OpenstackClient;
import test.cli.cloudify.cloud.services.tools.openstack.RackspaceClient;

import java.io.IOException;
import java.util.*;

public class RackspaceCloudService extends AbstractCloudService {
	private static final int DEFAULT_SERVER_SHUTDOWN_TIMEOUT = 5 * 60000;
    private static final String RACKSPACE_CERT_PROPERTIES = CREDENTIALS_FOLDER + "/cloud/rackspace/rackspace-cred.properties";

    private Properties certProperties = getCloudProperties(RACKSPACE_CERT_PROPERTIES);
	private String user = certProperties.getProperty("user");
	private String apiKey = certProperties.getProperty("apiKey");
	private String tenant = certProperties.getProperty("tenant");

	private RackspaceClient rackspaceClient;

	public RackspaceCloudService() {
		super("rsopenstack");

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
	public void injectCloudAuthenticationDetails()
			throws IOException {

		getProperties().put("user", this.user);
		getProperties().put("apiKey", this.apiKey);
		getProperties().put("tenant", this.tenant);
		
		Map<String, String> propsToReplace = new HashMap<String, String>();
		propsToReplace.put("machineNamePrefix " + "\"agent\"", "machineNamePrefix " + '"' + getMachinePrefix()
				+ "cloudify-agent" + '"');
		propsToReplace.put("managementGroup " + "\"management\"", "managementGroup " + '"' + getMachinePrefix()
				+ "cloudify-manager" + '"');
		propsToReplace.put("numberOfManagementMachines 1", "numberOfManagementMachines " + getNumberOfManagementMachines());
		propsToReplace.put("\"openstack.wireLog\": \"false\"", "\"openstack.wireLog\": \"true\"");

		IOUtils.replaceTextInFile(getPathToCloudGroovy(), propsToReplace);
	}

	private RackspaceClient createClient() {
		RackspaceClient client = new RackspaceClient();
		client.setConfig(getCloud());
		return client;
	}

	@Override
	public boolean scanLeakedAgentNodes() {
		
		if (rackspaceClient == null) {
			this.rackspaceClient = createClient();
		}
		
		String token = rackspaceClient.createAuthenticationToken();

		final String agentPrefix = getCloud().getProvider().getMachineNamePrefix();

		return checkForLeakedNode(token, agentPrefix);
	

	}
	@Override
	public boolean scanLeakedAgentAndManagementNodes() {
		if(rackspaceClient == null) {
			rackspaceClient = createClient();
		}
		String token = rackspaceClient.createAuthenticationToken();

		final String agentPrefix = getCloud().getProvider().getMachineNamePrefix();
		final String mgmtPrefix = getCloud().getProvider().getManagementGroup();
		
		final boolean result = checkForLeakedNode(token, agentPrefix, mgmtPrefix);
		this.rackspaceClient.close();
		return result;

	}

	private boolean checkForLeakedNode(String token, final String... prefixes) {
		List<Node> nodes;
		try {
			nodes = rackspaceClient.listServers(token);
		} catch (OpenstackException e) {
			throw new IllegalStateException("Failed to query openstack cloud for current servers", e);
		}

		List<Node> leakedNodes = new LinkedList<Node>();
		for (Node node : nodes) {
			if (node.getStatus().equals(OpenstackClient.MACHINE_STATUS_ACTIVE)) {
				for (String prefix : prefixes) {
					if (node.getName().startsWith(prefix)) {
						LogUtils.log("Found leaking node with name " + node.getName());
						leakedNodes.add(node);
					}
				}

			}
		}

		if (leakedNodes.size() > 0) {
			for (Node node : leakedNodes) {
				LogUtils.log("Shutting down: " + node);
				try {
					rackspaceClient.terminateServer(node.getId(), token, System.currentTimeMillis() + DEFAULT_SERVER_SHUTDOWN_TIMEOUT);
				} catch (Exception e) {
					LogUtils.log("Failed to terminate Rackspace openstack node: " + node.getId()
							+ ". This node may be leaking. Node details: " + node + ", Error was: " + e.getMessage(), e);
				}
			}
			return false;
		}
		
		return true;
	}
}
