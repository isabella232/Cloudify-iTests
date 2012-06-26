package test.cli.cloudify.cloud.services.tools.openstack;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.ws.rs.core.MediaType;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.cloudifysource.dsl.cloud.Cloud;
import org.cloudifysource.dsl.cloud.CloudTemplate;
import org.cloudifysource.esc.driver.provisioning.CloudProvisioningException;
import org.cloudifysource.esc.driver.provisioning.MachineDetails;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;

import framework.utils.LogUtils;

/**************
 * A custom cloud driver for RackStack OpenStack, using keystone authentication.
 * 
 * @author yoramw
 * @since 2.1
 * 
 */
public class RackspaceClient {

	private static final int HTTP_NOT_FOUND = 404;
	private static final int SERVER_POLLING_INTERVAL_MILLIS = 10 * 1000; // 10 seconds
	private static final int DEFAULT_SHUTDOWN_TIMEOUT_MILLIS = 5 * 60 * 1000; // 5 minutes
	private static final String OPENSTACK_OPENSTACK_IDENTITY_ENDPOINT = "openstack.identity.endpoint";
	private static final String OPENSTACK_WIRE_LOG = "openstack.wireLog";
	private static final String RS_OPENSTACK_TENANT = "openstack.tenant";
	private static final String OPENSTACK_OPENSTACK_ENDPOINT = "openstack.endpoint";

	private final XPath xpath = XPathFactory.newInstance().newXPath();

	private final Client client;

	private String tenant;
	private String serverNamePrefix;
	private String endpoint;
	private WebResource service;
	private String pathPrefix;
	private String identityEndpoint;

	private final Object xmlFactoryMutex = new Object();
	private final DocumentBuilderFactory dbf;
	private Cloud cloud;

	/************
	 * Constructor.
	 * 
	 * @throws ParserConfigurationException
	 */
	public RackspaceClient() {
		dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(false);

		final ClientConfig config = new DefaultClientConfig();
		this.client = Client.create(config);

	}

	private DocumentBuilder createDocumentBuilder() {
		synchronized (xmlFactoryMutex) {
			// Document builder is not guaranteed to be thread sage
			try {
				// Document builders are not thread safe
				return dbf.newDocumentBuilder();
			} catch (final ParserConfigurationException e) {
				throw new IllegalStateException("Failed to set up XML Parser", e);
			}
		}

	}

	public void close() {
		this.client.destroy();
	}

	public void setConfig(final Cloud cloud) {
		this.cloud = cloud;
		this.tenant = (String) this.cloud.getCustom().get(RS_OPENSTACK_TENANT);
		if (tenant == null) {
			throw new IllegalArgumentException("Custom field '" + RS_OPENSTACK_TENANT + "' must be set");
		}

		this.pathPrefix = "/v1.0/" + tenant + "/";

		this.endpoint = (String) this.cloud.getCustom().get(OPENSTACK_OPENSTACK_ENDPOINT);
		if (this.endpoint == null) {
			throw new IllegalArgumentException("Custom field '" + OPENSTACK_OPENSTACK_ENDPOINT + "' must be set");
		}
		this.service = client.resource(this.endpoint);

		this.identityEndpoint = (String) this.cloud.getCustom().get(
				OPENSTACK_OPENSTACK_IDENTITY_ENDPOINT);
		if (this.identityEndpoint == null) {
			throw new IllegalArgumentException("Custom field '" + OPENSTACK_OPENSTACK_IDENTITY_ENDPOINT
					+ "' must be set");
		}

		final String wireLog = (String) this.cloud.getCustom().get(
				OPENSTACK_WIRE_LOG);
		// if (wireLog != null) {
		// if (Boolean.parseBoolean(wireLog)) {
		// this.client.addFilter(new LoggingFilter(logger));
		// }
		// }

	}

	private long calcEndTimeInMillis(final long duration, final TimeUnit unit) {
		return System.currentTimeMillis() + unit.toMillis(duration);
	}

	public boolean stopMachine(final String ip, final long duration, final TimeUnit unit)
			throws InterruptedException, TimeoutException, CloudProvisioningException {
		final long endTime = calcEndTimeInMillis(
				duration, unit);

		final String token = createAuthenticationToken();

		try {
			terminateServerByIp(
					ip, token, endTime);
			return true;
		} catch (final Exception e) {
			throw new CloudProvisioningException(e);
		}
	}

	public Node getNode(final String nodeId, final String token)
			throws OpenstackException {
		final String response = service.path(
				this.pathPrefix + "servers/" + nodeId).header(
				"X-Auth-Token", token).accept(
				MediaType.APPLICATION_XML).get(
				String.class);
		final Node node = new Node();
		try {
			final DocumentBuilder documentBuilder = createDocumentBuilder();
			final Document xmlDoc = documentBuilder.parse(new InputSource(new StringReader(response)));

			node.setId(xpath.evaluate(
					"/server/@id", xmlDoc));
			node.setStatus(xpath.evaluate(
					"/server/@status", xmlDoc));
			node.setName(xpath.evaluate(
					"/server/@name", xmlDoc));

			NodeList addresses = (NodeList) xpath.evaluate(
					"/server/addresses/private/ip/@addr", xmlDoc, XPathConstants.NODESET);

			if (addresses.getLength() > 0) {
				node.setPrivateIp(addresses.item(
						0).getTextContent());

			}

			addresses = (NodeList) xpath.evaluate(
					"/server/addresses/public/ip/@addr", xmlDoc, XPathConstants.NODESET);
			if (addresses.getLength() > 0) {
				node.setPublicIp(addresses.item(
						0).getTextContent());
			}
		} catch (final XPathExpressionException e) {
			throw new OpenstackException("Failed to parse XML Response from server. Response was: " + response
					+ ", Error was: " + e.getMessage(), e);
		} catch (final SAXException e) {
			throw new OpenstackException("Failed to parse XML Response from server. Response was: " + response
					+ ", Error was: " + e.getMessage(), e);
		} catch (final IOException e) {
			throw new OpenstackException("Failed to send request to server. Response was: " + response
					+ ", Error was: " + e.getMessage(), e);
		}

		return node;

	}

	public List<Node> listServers(final String token)
			throws OpenstackException {
		final List<String> ids = listServerIds(token);
		final List<Node> nodes = new ArrayList<Node>(ids.size());

		for (final String id : ids) {
			nodes.add(getNode(
					id, token));
		}

		return nodes;
	}

	private List<String> listServerIds(final String token)
			throws OpenstackException {

		String response = null;
		try {
			response = service.path(
					this.pathPrefix + "servers")
					.queryParam("dummyReq", Long.toString(System.currentTimeMillis()))
					.header(
							"X-Auth-Token", token).accept(
							MediaType.APPLICATION_XML).get(
							String.class);

			final DocumentBuilder documentBuilder = createDocumentBuilder();
			final Document xmlDoc = documentBuilder.parse(new InputSource(new StringReader(response)));

			final NodeList idNodes = (NodeList) xpath.evaluate(
					"/servers/server/@id", xmlDoc, XPathConstants.NODESET);
			final int howmany = idNodes.getLength();
			final List<String> ids = new ArrayList<String>(howmany);
			for (int i = 0; i < howmany; i++) {
				ids.add(idNodes.item(
						i).getTextContent());

			}
			return ids;

		} catch (final UniformInterfaceException e) {
			final String responseEntity = e.getResponse().getEntity(String.class);
			throw new OpenstackException(e + " Response entity: " + responseEntity);

		} catch (final SAXException e) {
			throw new OpenstackException("Failed to parse XML Response from server. Response was: " + response
					+ ", Error was: " + e.getMessage(), e);
		} catch (final XPathException e) {
			throw new OpenstackException("Failed to parse XML Response from server. Response was: " + response
					+ ", Error was: " + e.getMessage(), e);
		} catch (final IOException e) {
			throw new OpenstackException("Failed to send request to server. Response was: " + response
					+ ", Error was: " + e.getMessage(), e);

		}
	}

	private void terminateServerByIp(final String serverIp, final String token, final long endTime)
			throws Exception {
		final Node node = getNodeByIp(
				serverIp, token);
		if (node == null) {
			throw new IllegalArgumentException("Could not find a server with IP: " + serverIp);
		}
		terminateServer(
				node.getId(), token, endTime);
	}

	private Node getNodeByIp(final String serverIp, final String token)
			throws OpenstackException {
		final List<Node> nodes = listServers(token);
		for (final Node node : nodes) {
			if (node.getPrivateIp() != null && node.getPrivateIp().equalsIgnoreCase(serverIp)
					|| node.getPublicIp() != null && node.getPublicIp().equalsIgnoreCase(serverIp)) {
				return node;
			}
		}

		return null;
	}

	public void terminateServer(final String serverId, final String token, final long endTime)
			throws Exception {
		terminateServers(
				Arrays.asList(serverId), token, endTime);
	}

	private void terminateServers(final List<String> serverIds, final String token, final long endTime)
			throws Exception {

		for (final String serverId : serverIds) {
			try {
				service.path(
						this.pathPrefix + "servers/" + serverId).header(
						"X-Auth-Token", token).accept(
						MediaType.APPLICATION_XML).delete();
			} catch (final UniformInterfaceException e) {
				final String responseEntity = e.getResponse().getEntity(String.class);
				throw new IllegalArgumentException(e + " Response entity: " + responseEntity);
			}

		}

		int successCounter = 0;

		// wait for all servers to die
		for (final String serverId : serverIds) {
			while (System.currentTimeMillis() < endTime) {
				try {
					this.getNode(
							serverId, token);

				} catch (final UniformInterfaceException e) {
					if (e.getResponse().getStatus() == HTTP_NOT_FOUND) {
						++successCounter;
						break;
					}
					throw e;
				}
				Thread.sleep(SERVER_POLLING_INTERVAL_MILLIS);
			}

		}

		if (successCounter == serverIds.size()) {
			return;
		}

		throw new TimeoutException("Nodes " + serverIds + " did not shut down in the required time");

	}

	/**
	 * Creates server. Block until complete. Returns id
	 * 
	 * @param name the server name
	 * @param timeout the timeout in seconds
	 * @param serverTemplate the cloud template to use for this server
	 * @return the server id
	 */
	private MachineDetails newServer(final String token, final long endTime, final CloudTemplate serverTemplate)
			throws Exception {

		final MachineDetails md = createServer(
				token, serverTemplate);

		try {
			// wait until complete
			waitForServerToReachStatus(
					md, endTime, md.getMachineId(), token, "ACTIVE");

			md.setAgentRunning(false);
			md.setCloudifyInstalled(false);
			md.setInstallationDirectory(serverTemplate.getRemoteDirectory());

			return md;
		} catch (final Exception e) {
			LogUtils.log(
					"server: " + md.getMachineId() + " failed to start up correctly. "
							+ "Shutting it down. Error was: " + e.getMessage(), e);
			try {
				terminateServer(
						md.getMachineId(), token, endTime);
			} catch (final Exception e2) {
				LogUtils.log(
						"Error while shutting down failed machine: " + md.getMachineId()
								+ ". Error was: " + e.getMessage()
								+ ".It may be leaking.", e);
			}
			throw e;
		}

	}

	private MachineDetails createServer(final String token, final CloudTemplate serverTemplate)
			throws OpenstackException {
		final String serverName = this.serverNamePrefix + System.currentTimeMillis();
		// Start the machine!
		final String json =
				"{\"server\":{ \"name\":\"" + serverName + "\",\"imageId\":" + serverTemplate.getImageId()
						+ ",\"flavorId\":" + serverTemplate.getHardwareId() + "}}";

		String serverBootResponse = null;
		try {
			serverBootResponse = service.path(
					this.pathPrefix + "servers")
					.header(
							"Content-Type", "application/json").header(
							"X-Auth-Token", token).accept(
							MediaType.APPLICATION_XML).post(
							String.class, json);
		} catch (final UniformInterfaceException e) {
			final String responseEntity = e.getResponse().getEntity(String.class);
			throw new OpenstackException(e + " Response entity: " + responseEntity);
		}

		try {
			// if we are here, the machine started!
			final DocumentBuilder documentBuilder = createDocumentBuilder();
			final Document doc = documentBuilder.parse(new InputSource(new StringReader(serverBootResponse)));

			final String status = xpath.evaluate(
					"/server/@status", doc);
			if (!status.startsWith("BUILD")) {
				throw new IllegalStateException("Expected server status of BUILD(*), got: " + status);
			}

			final String serverId = xpath.evaluate(
					"/server/@id", doc);
			final String rootPassword = xpath.evaluate(
					"/server/@adminPass", doc);
			final MachineDetails md = new MachineDetails();
			md.setMachineId(serverId);
			md.setRemoteUsername("root");
			md.setRemotePassword(rootPassword);
			return md;
		} catch (final XPathExpressionException e) {
			throw new OpenstackException("Failed to parse XML Response from server. Response was: "
					+ serverBootResponse + ", Error was: " + e.getMessage(), e);
		} catch (final SAXException e) {
			throw new OpenstackException("Failed to parse XML Response from server. Response was: "
					+ serverBootResponse + ", Error was: " + e.getMessage(), e);
		} catch (final IOException e) {
			throw new OpenstackException("Failed to send request to server. Response was: " + serverBootResponse
					+ ", Error was: " + e.getMessage(), e);
		}
	}

	private void waitForServerToReachStatus(final MachineDetails md, final long endTime, final String serverId,
			final String token, final String status)
			throws OpenstackException, TimeoutException, InterruptedException {

		final String respone = null;
		while (true) {

			final Node node = this.getNode(
					serverId, token);

			final String currentStatus = node.getStatus().toLowerCase();

			if (currentStatus.equalsIgnoreCase(status)) {

				md.setPrivateAddress(node.getPrivateIp());
				md.setPublicAddress(node.getPublicIp());
				break;
			} else {
				if (currentStatus.contains("error")) {
					throw new OpenstackException("Server provisioning failed. Node ID: " + node.getId() + ", status: "
							+ node.getStatus());
				}

			}

			if (System.currentTimeMillis() > endTime) {
				throw new TimeoutException("timeout creating server. last status:" + respone);
			}

			Thread.sleep(SERVER_POLLING_INTERVAL_MILLIS);

		}

	}

	/**********
	 * Creates an openstack keystone authentication token.
	 * 
	 * @return the authentication token.
	 */

	public String createAuthenticationToken() {
		final String json =
				"{\"auth\":{\"RAX-KSKEY:apiKeyCredentials\":{\"username\":\"" + this.cloud.getUser().getUser()
						+ "\",\"apiKey\":\"" + this.cloud.getUser().getApiKey() + "\"}}}";
		final WebResource service = client.resource(this.identityEndpoint);
		final String resp = service.path(
				"/v2.0/tokens").header(
				"Content-Type", "application/json").post(
				String.class, json);

		final String autenticationTokenId = getAutenticationTokenIdFromResponse(resp);
		return autenticationTokenId;
	}

	@SuppressWarnings("unchecked")
	private String getAutenticationTokenIdFromResponse(final String resp) {
		final ObjectMapper mapper = new ObjectMapper();
		try {
			final Map<String, Object> readValue = mapper.readValue(new StringReader(resp), Map.class);
			final Map<String, Object> accessMap = (Map<String, Object>) readValue.get("access");
			final Map<String, String> tokenMap = (Map<String, String>) accessMap.get("token");
			final String tokenId = tokenMap.get("id");
			return tokenId;
		} catch (final JsonParseException e) {
			throw new RuntimeException(e);
		} catch (final JsonMappingException e) {
			throw new RuntimeException(e);
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}
}
