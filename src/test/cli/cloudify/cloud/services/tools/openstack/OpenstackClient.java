package test.cli.cloudify.cloud.services.tools.openstack;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import org.cloudifysource.esc.driver.provisioning.MachineDetails;
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
 * A custom cloud driver for OpenStack, using keystone authentication. In order to be able to define a floating IP for a
 * machine Changes will have to be made in the cloud driver. a floating ip should be allocated and attached to the
 * server in the newServer method, and detach and deleted upon machine shutdown.
 * 
 * 
 * @author barakme
 * @since 2.1
 * 
 */
public class OpenstackClient {

	public static final String MACHINE_STATUS_ACTIVE = "ACTIVE";
	private static final int HTTP_NOT_FOUND = 404;
	
	private static final int SERVER_POLLING_INTERVAL_MILLIS = 10 * 1000; // 10 seconds
	
	private static final String OPENSTACK_OPENSTACK_IDENTITY_ENDPOINT = "openstack.identity.endpoint";
	
	private static final String OPENSTACK_KEY_PAIR = "openstack.keyPair";
	private static final String OPENSTACK_SECURITYGROUP = "openstack.securityGroup";
	private static final String OPENSTACK_OPENSTACK_ENDPOINT = "openstack.endpoint";
	private static final String OPENSTACK_TENANT = "openstack.tenant";
	
	
	private final XPath xpath = XPathFactory.newInstance().newXPath();

	private final Client client;

	private final long throttlingTimeout = -1;
	private String serverNamePrefix;
	private String tenant;
	private String endpoint;
	private WebResource service;
	private String pathPrefix;
	private String identityEndpoint;
	private final DocumentBuilderFactory dbf;
	private final Object xmlFactoryMutex = new Object();
	private Cloud cloud;

	/************
	 * Constructor.
	 * 
	 * @throws ParserConfigurationException
	 */
	public OpenstackClient() {
		dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(false);

		final ClientConfig config = new DefaultClientConfig();
		this.client = Client.create(config);

	}

	public void close() {
		this.client.destroy();
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

	public void setConfig(final Cloud cloud) {

		this.cloud = cloud;
		this.tenant = (String) this.cloud.getCustom().get(OPENSTACK_TENANT);

		if (tenant == null) {
			throw new IllegalArgumentException("Custom field '" + OPENSTACK_TENANT + "' must be set");
		}

		this.pathPrefix = "v1.1/" + tenant + "/";

		this.endpoint = (String) this.cloud.getCustom().get(OPENSTACK_OPENSTACK_ENDPOINT);
		if (this.endpoint == null) {
			throw new IllegalArgumentException("Custom field '" + OPENSTACK_OPENSTACK_ENDPOINT + "' must be set");
		}
		this.service = client.resource(this.endpoint);

		this.identityEndpoint = (String) this.cloud.getCustom().get(OPENSTACK_OPENSTACK_IDENTITY_ENDPOINT);
		if (this.identityEndpoint == null) {
			throw new IllegalArgumentException("Custom field '" + OPENSTACK_OPENSTACK_IDENTITY_ENDPOINT
					+ "' must be set");
		}

		// final String wireLog = (String) this.cloud.getCustom().get(OPENSTACK_WIRE_LOG);
		// if (wireLog != null) {
		// if (Boolean.parseBoolean(wireLog)) {
		// this.client.addFilter(new LoggingFilter(logger));
		// }
		// }

	}

	private Node getNode(final String nodeId, final String token)
			throws OpenstackException {
		final String response =
				service.path(this.pathPrefix + "servers/" + nodeId).header("X-Auth-Token", token)
						.accept(MediaType.APPLICATION_XML).get(String.class);
		final Node node = new Node();
		try {
			final DocumentBuilder documentBuilder = createDocumentBuilder();
			final Document xmlDoc = documentBuilder.parse(new InputSource(new StringReader(response)));

			node.setId(xpath.evaluate("/server/@id", xmlDoc));
			node.setStatus(xpath.evaluate("/server/@status", xmlDoc));
			node.setName(xpath.evaluate("/server/@name", xmlDoc));

			// We expect to get 2 IP addresses, public and private. Currently we get them both in an xml
			// under a private node attribute. this is expected to change.
			final NodeList addresses =
					(NodeList) xpath.evaluate("/server/addresses/network/ip/@addr", xmlDoc, XPathConstants.NODESET);
			if (node.getStatus().equalsIgnoreCase(MACHINE_STATUS_ACTIVE)) {

				if (addresses.getLength() != 2) {
					throw new IllegalStateException("Expected 2 addresses, private and public, got "
							+ addresses.getLength() + " addresses");
				}

				node.setPrivateIp(addresses.item(0).getTextContent());
				node.setPublicIp(addresses.item(1).getTextContent());
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
			nodes.add(getNode(id, token));
		}

		return nodes;
	}

	public void listFlavors(final String token)
			throws OpenstackException {
		String response = null;

		response =
				service.path(this.pathPrefix + "flavors").header("X-Auth-Token", token)
						.accept(MediaType.APPLICATION_XML).get(String.class);
		System.out.println(response);

	}

	public void listImages(final String token)
			throws OpenstackException {
		String response = null;
		try {
			response =
					service.path(this.pathPrefix + "images").header("X-Auth-Token", token)
							.accept(MediaType.APPLICATION_XML).get(String.class);
			System.out.println(response);

			// final NodeList idNodes = (NodeList) xpath.evaluate("/servers/server/@id", xmlDoc,
			// XPathConstants.NODESET);
			// final int howmany = idNodes.getLength();
			// final List<String> ids = new ArrayList<String>(howmany);
			// for (int i = 0; i < howmany; i++) {
			// ids.add(idNodes.item(i).getTextContent());
			//
			// }
			// return ids;

		} catch (final UniformInterfaceException e) {
			final String responseEntity = e.getResponse().getEntity(String.class);
			throw new OpenstackException(e + " Response entity: " + responseEntity, e);

			// } catch (final SAXException e) {
			// throw new OpenstackException("Failed to parse XML Response from server. Response was: " + response
			// + ", Error was: " + e.getMessage(), e);
			// // } catch (final XPathException e) {
			// // throw new OpenstackException("Failed to parse XML Response from server. Response was: " + response
			// // + ", Error was: " + e.getMessage(), e);
			// } catch (final IOException e) {
			// throw new OpenstackException("Failed to send request to server. Response was: " + response
			// + ", Error was: " + e.getMessage(), e);
			//
			// }
		}
	}

	private List<String> listServerIds(final String token)
			throws OpenstackException {

		String response = null;
		try {
			response =
					service.path(this.pathPrefix + "servers").header("X-Auth-Token", token)
							.accept(MediaType.APPLICATION_XML).get(String.class);
			final DocumentBuilder documentBuilder = createDocumentBuilder();
			final Document xmlDoc = documentBuilder.parse(new InputSource(new StringReader(response)));

			final NodeList idNodes = (NodeList) xpath.evaluate("/servers/server/@id", xmlDoc, XPathConstants.NODESET);
			final int howmany = idNodes.getLength();
			final List<String> ids = new ArrayList<String>(howmany);
			for (int i = 0; i < howmany; i++) {
				ids.add(idNodes.item(i).getTextContent());

			}
			return ids;

		} catch (final UniformInterfaceException e) {
			final String responseEntity = e.getResponse().getEntity(String.class);
			throw new OpenstackException(e + " Response entity: " + responseEntity, e);

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
		final Node node = getNodeByIp(serverIp, token);
		if (node == null) {
			throw new IllegalArgumentException("Could not find a server with IP: " + serverIp);
		}
		terminateServer(node.getId(), token, endTime);
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
		terminateServers(Arrays.asList(serverId), token, endTime);
	}

	private void terminateServers(final List<String> serverIds, final String token, final long endTime)
			throws Exception {

		// detach public ip and delete the servers
		for (final String serverId : serverIds) {
			try {
				service.path(this.pathPrefix + "servers/" + serverId).header("X-Auth-Token", token)
						.accept(MediaType.APPLICATION_XML).delete();
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
					this.getNode(serverId, token);

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

		final String serverId = createServer(token, serverTemplate);

		try {
			final MachineDetails md = new MachineDetails();
			// wait until complete
			waitForServerToReachStatus(md, endTime, serverId, token, MACHINE_STATUS_ACTIVE);

			// if here, we have a node with a private and public ip.
			final Node node = this.getNode(serverId, token);

			md.setPublicAddress(node.getPublicIp());
			md.setMachineId(serverId);
			md.setAgentRunning(false);
			md.setCloudifyInstalled(false);
			md.setInstallationDirectory(serverTemplate.getRemoteDirectory());

			md.setRemoteUsername("root");

			return md;
		} catch (final Exception e) {
			LogUtils.log("server: " + serverId + " failed to start up correctly. "
					+ "Shutting it down. Error was: " + e.getMessage(), e);
			try {
				terminateServer(serverId, token, endTime);
			} catch (final Exception e2) {
				LogUtils.log(
						"Error while shutting down failed machine: " + serverId + ". Error was: " + e.getMessage()
								+ ".It may be leaking.", e);
			}
			throw e;
		}

	}

	private String createServer(final String token, final CloudTemplate serverTemplate)
			throws OpenstackException {
		final String serverName = this.serverNamePrefix + System.currentTimeMillis();
		final String securityGroup = getCustomTemplateValue(serverTemplate, OPENSTACK_SECURITYGROUP, null, false);
		final String keyPairName = getCustomTemplateValue(serverTemplate, OPENSTACK_KEY_PAIR, null, false);

		// Start the machine!
		final String json =
				"{\"server\":{ \"name\":\"" + serverName + "\",\"imageRef\":\"" + serverTemplate.getImageId()
						+ "\",\"flavorRef\":\"" + serverTemplate.getHardwareId() + "\",\"key_name\":\"" + keyPairName
						+ "\",\"security_groups\":[{\"name\":\"" + securityGroup + "\"}]}}";

		String serverBootResponse = null;
		try {
			serverBootResponse =
					service.path(this.pathPrefix + "servers").header("Content-Type", "application/json")
							.header("X-Auth-Token", token).accept(MediaType.APPLICATION_XML).post(String.class, json);
		} catch (final UniformInterfaceException e) {
			final String responseEntity = e.getResponse().getEntity(String.class);
			throw new OpenstackException(e + " Response entity: " + responseEntity, e);
		}

		try {
			// if we are here, the machine started!
			final DocumentBuilder documentBuilder = createDocumentBuilder();
			final Document doc = documentBuilder.parse(new InputSource(new StringReader(serverBootResponse)));

			final String status = xpath.evaluate("/server/@status", doc);
			if (!status.startsWith("BUILD")) {
				throw new IllegalStateException("Expected server status of BUILD(*), got: " + status);
			}

			final String serverId = xpath.evaluate("/server/@id", doc);
			return serverId;
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

	private String getCustomTemplateValue(final CloudTemplate serverTemplate, final String key,
			final String defaultValue, final boolean allowNull) {
		final String value = (String) serverTemplate.getOptions().get(key);
		if (value == null) {
			if (allowNull) {
				return defaultValue;
			} else {
				throw new IllegalArgumentException("Template option '" + key + "' must be set");
			}
		} else {
			return value;
		}

	}

	private void waitForServerToReachStatus(final MachineDetails md, final long endTime, final String serverId,
			final String token, final String status)
			throws OpenstackException, TimeoutException, InterruptedException {

		final String respone = null;
		while (true) {

			final Node node = this.getNode(serverId, token);

			final String currentStatus = node.getStatus().toLowerCase();

			if (currentStatus.equalsIgnoreCase(status)) {

				md.setPrivateAddress(node.getPrivateIp());
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

	@SuppressWarnings("rawtypes")
	List<FloatingIP> listFloatingIPs(final String token)
			throws SAXException, IOException {
		final String response =
				service.path(this.pathPrefix + "os-floating-ips").header("X-Auth-Token", token)
						.accept(MediaType.APPLICATION_JSON).get(String.class);

		final ObjectMapper mapper = new ObjectMapper();
		final Map map = mapper.readValue(new StringReader(response), Map.class);
		@SuppressWarnings("unchecked")
		final List<Map> list = (List<Map>) map.get("floating_ips");
		final List<FloatingIP> floatingIps = new ArrayList<FloatingIP>(map.size());

		for (final Map floatingIpMap : list) {
			final FloatingIP ip = new FloatingIP();

			final Object instanceId = floatingIpMap.get("instance_id");

			ip.setInstanceId(instanceId == null ? null : instanceId.toString());
			ip.setIp((String) floatingIpMap.get("ip"));
			ip.setFixedIp((String) floatingIpMap.get("fixed_ip"));
			ip.setId(floatingIpMap.get("id").toString());
			floatingIps.add(ip);
		}
		return floatingIps;

	}

	private FloatingIP getFloatingIpByIp(final String ip, final String token)
			throws SAXException, IOException {
		final List<FloatingIP> allips = listFloatingIPs(token);
		for (final FloatingIP floatingIP : allips) {
			if (ip.equals(floatingIP.getIp())) {
				return floatingIP;
			}
		}

		return null;
	}

	/*********************
	 * Deletes a floating IP.
	 * 
	 * @param ip .
	 * @param token .
	 * @throws SAXException .
	 * @throws IOException .
	 */
	public void deleteFloatingIP(final String ip, final String token)
			throws SAXException, IOException {

		final FloatingIP floatingIp = getFloatingIpByIp(ip, token);
		if (floatingIp == null) {
			LogUtils.log("Could not find floating IP " + ip + " in list. IP was not deleted.");
		} else {
			service.path(this.pathPrefix + "os-floating-ips/" + floatingIp.getId()).header("X-Auth-Token", token)
					.accept(MediaType.APPLICATION_JSON).delete();

		}

	}

	/**************
	 * Allocates a floating IP.
	 * 
	 * @param token .
	 * @return .
	 */
	public String allocateFloatingIP(final String token) {

		try {
			final String resp =
					service.path(this.pathPrefix + "os-floating-ips").header("Content-type", "application/json")
							.header("X-Auth-Token", token).accept(MediaType.APPLICATION_JSON).post(String.class, "");

			final Matcher m = Pattern.compile("\"ip\": \"([^\"]*)\"").matcher(resp);
			if (m.find()) {
				return m.group(1);
			} else {
				throw new IllegalStateException("Failed to allocate floating IP - IP not found in response");
			}
		} catch (final UniformInterfaceException e) {
			logRestError(e);
			throw new IllegalStateException("Failed to allocate floating IP", e);
		}

	}

	private void logRestError(final UniformInterfaceException e) {
		LogUtils.log("REST Error: " + e.getMessage());
		LogUtils.log("REST Status: " + e.getResponse().getStatus());
		LogUtils.log("REST Message: " + e.getResponse().getEntity(String.class));
	}

	/**
	 * Attaches a previously allocated floating ip to a server.
	 * 
	 * @param serverid .
	 * @param ip public ip to be assigned .
	 * @param token .
	 * @throws Exception .
	 */
	public void addFloatingIP(final String serverid, final String ip, final String token)
			throws Exception {

		service.path(this.pathPrefix + "servers/" + serverid + "/action")
				.header("Content-type", "application/json")
				.header("X-Auth-Token", token)
				.accept(MediaType.APPLICATION_JSON)
				.post(String.class,
						String.format("{\"addFloatingIp\":{\"server\":\"%s\",\"address\":\"%s\"}}", serverid, ip));

	}

	/**********
	 * Detaches a floating IP from a server.
	 * 
	 * @param serverId .
	 * @param ip .
	 * @param token .
	 */
	public void detachFloatingIP(final String serverId, final String ip, final String token) {

		service.path(this.pathPrefix + "servers/" + serverId + "/action")
				.header("Content-type", "application/json")
				.header("X-Auth-Token", token)
				.accept(MediaType.APPLICATION_JSON)
				.post(String.class,
						String.format("{\"removeFloatingIp\":{\"server\": \"%s\", \"address\": \"%s\"}}",
								serverId, ip));

	}

	/**********
	 * Creates an openstack keystone authentication token.
	 * 
	 * @return the authentication token.
	 */

	public String createAuthenticationToken() {

		final String json =
				"{\"auth\":{\"apiAccessKeyCredentials\":{\"accessKey\":\"" + this.cloud.getUser().getUser()
						+ "\",\"secretKey\":\"" + this.cloud.getUser().getApiKey() + "\"},\"tenantId\":\""
						+ this.tenant + "\"}}";

		final WebResource service = client.resource(this.identityEndpoint);

		final String resp =
				service.path("/v2.0/tokens").header("Content-Type", "application/json")
						.accept(MediaType.APPLICATION_XML).post(String.class, json);

		final Matcher m = Pattern.compile("token id=\"([^\"]*)\"").matcher(resp);
		if (m.find()) {
			final String token = m.group(1);
			return token;
		}

		throw new RuntimeException("error:" + resp);
	}

	/**
	 * Checks if throttling is now activated, to avoid overloading the cloud.
	 * 
	 * @return True if throttling is activate, false otherwise
	 */
	public boolean isThrottling() {
		boolean throttling = false;
		if (throttlingTimeout > 0 && throttlingTimeout - System.currentTimeMillis() > 0) {
			throttling = true;
		}

		return throttling;
	}
}
