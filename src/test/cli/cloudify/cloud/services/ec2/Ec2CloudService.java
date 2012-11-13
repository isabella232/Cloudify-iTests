package test.cli.cloudify.cloud.services.ec2;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.cloudifysource.esc.jclouds.WindowsServerEC2ReviseParsedImage;
import org.jclouds.aws.ec2.compute.strategy.AWSEC2ReviseParsedImage;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.ComputeServiceContextFactory;
import org.jclouds.compute.domain.ComputeMetadata;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.NodeState;
import org.jclouds.logging.jdk.config.JDKLoggingModule;

import test.cli.cloudify.cloud.services.AbstractCloudService;

import com.google.inject.AbstractModule;
import com.google.inject.Module;

import framework.tools.SGTestHelper;
import framework.utils.IOUtils;
import framework.utils.LogUtils;

public class Ec2CloudService extends AbstractCloudService {
	
	public static final String DEFAULT_US_EAST_LINUX_AMI = "us-east-1/ami-76f0061f";
	public static final String DEFAULT_US_EAST_UBUNTU_AMI = "us-east-1/ami-82fa58eb";
	public static final String DEFAULT_EU_WEST_LINUX_AMI = "eu-west-1/ami-c37474b7";
	public static final String DEFAULT_EU_WEST_UBUNTU_AMI = "eu-west-1/ami-c1aaabb5";
	public static final String LINUX_AMI_VARIABLE = "LINUX_AMI";
	public static final String UBUNTU_AMI_VARIABLE = "UBUNTU_AMI";

	private static final String DEFAULT_EC2_CLOUD_NAME = "ec2";
	
	public static final String DEFAULT_EC2_LINUX_AMI_USERNAME = "ec2-user";
	public static final String DEFAULT_EC2_UBUNTU_AMI_USERNAME = "ubuntu";
	
	private String user = "AKIAI4OVPQZZQT53O6SQ";
	private String apiKey = "xI/BDTPh0LE9PcC0aHhn5GEUh+/hjOiRcKwCNVP5";
	private String keyPair = "ec2-sgtest";
	private ComputeServiceContext context;

	public Ec2CloudService(final String uniqueName) {
		super(uniqueName, DEFAULT_EC2_CLOUD_NAME);
	}
	
	public Ec2CloudService(final String uniqueName, final String cloudName) {
		super(uniqueName, cloudName);
	}
	
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
		return System.getProperty("ec2.region" , "eu");
	}

	public void setKeyPair(final String keyPair) {
		this.keyPair = keyPair;
	}

	public String getKeyPair() {
		return keyPair;
	}
	
	@Override
	public void beforeBootstrap() {

		final Set<Module> wiring = new HashSet<Module>();
		wiring.add(new AbstractModule() {

			@Override
			protected void configure() {
				bind(
						AWSEC2ReviseParsedImage.class).to(
						WindowsServerEC2ReviseParsedImage.class);
			}

		});

		// Enable logging using gs_logging
		wiring.add(new JDKLoggingModule());

		Properties overrides = new Properties();
		overrides.put("jclouds.ec2.ami-query", "");
		overrides.put("jclouds.ec2.cc-ami-query", "");

		final String provider = this.cloudConfiguration.getProvider().getProvider();
		final String user = this.cloudConfiguration.getUser().getUser();
		final String key = this.cloudConfiguration.getUser().getApiKey();
		LogUtils.log("Creating jclouds context, this may take a few seconds");
		this.context = new ComputeServiceContextFactory().createContext(
				provider, user, key, wiring, overrides);
		LogUtils.log("Jclouds context created successfully, scanning for machines leaked from previous tests");
		final Set<? extends ComputeMetadata> aloNodes = this.context.getComputeService().listNodes();
		final String agentPrefix = this.cloudConfiguration.getProvider().getMachineNamePrefix();
		final String managerPrefix = this.cloudConfiguration.getProvider().getManagementGroup();

		for (final ComputeMetadata computeMetadata : aloNodes) {
			final String name = computeMetadata.getName();
			final NodeState state = ((NodeMetadata) computeMetadata).getState();
			switch (state) {
			case TERMINATED:
				// ignore
				break;
			case ERROR:
			case PENDING:
			case RUNNING:
			case UNRECOGNIZED:
			case SUSPENDED:
			default:
				if (name != null) {
					if (name.startsWith(agentPrefix)) {
						throw new IllegalStateException(
								"Before bootstrap, found a non-terminated node in the cloud " 
										+ "with the agent prefix of the current cloud. Node details: "
										+ computeMetadata);
					} else if (name.startsWith(managerPrefix)) {
						throw new IllegalStateException(
								"Before bootstrap, found a non-terminated node in the cloud " 
										+ "with the management prefix of the current cloud. Node details: "
										+ computeMetadata);
					}
				}
				break;
			}
		}
	}

	@Override
	public boolean scanLeakedAgentAndManagementNodes() {


		if (this.context == null) {
			return true;
		}

		final String agentPrefix = this.cloudConfiguration.getProvider().getMachineNamePrefix();
		final String managerPrefix = this.cloudConfiguration.getProvider().getManagementGroup();

		final List<ComputeMetadata> leakedNodes = checkForLeakedNodesWithPrefix(agentPrefix, managerPrefix);

		LogUtils.log("Closing jclouds context for this EC2 cloud service");
		this.context.close();

		return leakedNodes.isEmpty();

	}
	
	@Override
	public void injectServiceAuthenticationDetails()
			throws IOException {
		
		getProperties().put("user", '"' + this.user +  '"');
		getProperties().put("apiKey", '"' + this.apiKey +  '"');
		getProperties().put("keyFile", '"' + this.keyPair + ".pem" +  '"');
		getProperties().put("keyPair", '"' + this.keyPair +  '"');
		

		final Map<String, String> propsToReplace = new HashMap<String, String>();
		
		if (getRegion().contains("eu")) {
			LogUtils.log("Working in eu region");
			propsToReplace.put("locationId \"us-east-1\"", "locationId \"eu-west-1\"");
			setKeyPair("sgtest-eu");
			if (!getCloudName().contains("win")) {
				propsToReplace.put('"' + DEFAULT_US_EAST_LINUX_AMI + '"', LINUX_AMI_VARIABLE);
				propsToReplace.put('"' + DEFAULT_US_EAST_UBUNTU_AMI + '"', UBUNTU_AMI_VARIABLE);
			}
		}
		
		propsToReplace.put("cloudify_agent_", this.machinePrefix + "cloudify-agent");
		propsToReplace.put("cloudify_manager", this.machinePrefix + "cloudify-manager");
		propsToReplace.put("numberOfManagementMachines 1", "numberOfManagementMachines " + numberOfManagementMachines);

		IOUtils.replaceTextInFile(getPathToCloudGroovy(), propsToReplace);

		// add a pem file
		final String sshKeyPemName = this.keyPair + ".pem";
		final File fileToCopy =
				new File(SGTestHelper.getSGTestRootDir() + "/apps/cloudify/cloud/" + getCloudName() + "/"
						+ sshKeyPemName);
		final File targetLocation = new File(getPathToCloudFolder() + "/upload/" + sshKeyPemName);
		final Map<File, File> filesToReplace = new HashMap<File, File>();
		filesToReplace.put(targetLocation, fileToCopy);
		
		if (getRegion().contains("eu") && !getCloudName().contains("win")) {
			getProperties().setProperty(LINUX_AMI_VARIABLE, '"' + DEFAULT_EU_WEST_LINUX_AMI + '"');
			getProperties().setProperty(UBUNTU_AMI_VARIABLE, '"' + DEFAULT_EU_WEST_UBUNTU_AMI + '"');
		}
		
		addFilesToReplace(filesToReplace);
	}

	@Override
	public boolean scanLeakedAgentNodes() {
		final String agentPrefix = this.cloudConfiguration.getProvider().getMachineNamePrefix();

		final List<ComputeMetadata> leakedNodes = checkForLeakedNodesWithPrefix(agentPrefix);

		return leakedNodes.isEmpty();

	}

	private List<ComputeMetadata> checkForLeakedNodesWithPrefix(final String... prefixes) {
		List<ComputeMetadata> leakedNodes = null;
		int iteration = 0;
		while (iteration < 3) {
			leakedNodes = new LinkedList<ComputeMetadata>();
			LogUtils.log("Checking for leaked nodes with prefix: " + Arrays.toString(prefixes));
			final Set<? extends ComputeMetadata> allNodes = this.context.getComputeService().listNodes();
			
			boolean foundPendingNodes = scanNodes(leakedNodes, allNodes,
					prefixes);

			if (!foundPendingNodes) {
				break; // no need to re-run the scan
			}
			try {
				LogUtils.log("Sleeping for several seconds to give pending nodes time to reach their new state");
				Thread.sleep(10000);
			} catch (InterruptedException e) {
				// ignore.
			}
			++iteration;
		}

		if (leakedNodes.size() > 0) {
			LogUtils.log("Killing leaked EC2 nodes");
			for (final ComputeMetadata leakedNode : leakedNodes) {

				LogUtils.log("Killing node: " + leakedNode.getName() + ": " + leakedNode);
				try {
					this.context.getComputeService().destroyNode(leakedNode.getId());
				} catch (final Exception e) {
					LogUtils.log("Failed to kill a leaked node. This machine may be leaking!", e);
				}
			}

		}
		return leakedNodes;
	}

	private boolean scanNodes(final List<ComputeMetadata> leakedNodes,
			final Set<? extends ComputeMetadata> allNodes, final String... prefixes) {
		boolean foundPendingNodes = false;
		for (final ComputeMetadata computeMetadata : allNodes) {
			final String name = computeMetadata.getName();
			final NodeState state = ((NodeMetadata) computeMetadata).getState();
			switch (state) {
			case TERMINATED:
				// ignore - node is shut down
				break;
			case PENDING:
			case ERROR:
			case RUNNING:
			case UNRECOGNIZED:
			case SUSPENDED:
			default:
				if (name == null || name.length() == 0) {						
					LogUtils.log("WARNING! Found a non-terminated node with an empty name. " 
							+ "The test can't tell if this is a leaked node or not! Node details: "
							+ computeMetadata);						
				} else {
					for (String prefix : prefixes) {
						if (name.startsWith(prefix)) {
							if (state.equals(NodeState.PENDING)) {
								// node is transitioning from one state to another 
								// 		- might be shutting down, might be starting up!
								// Must try again later.
								foundPendingNodes = true;
								LogUtils.log("While scanning for leaked nodes, found a node in state PENDING. " 
										+ "Leak scan will be retried. Node in state pending was: "
										+ computeMetadata);
							} else {
								LogUtils.log("ERROR: Found a leaking node: " + computeMetadata);
								leakedNodes.add(computeMetadata);
							}
						}
					}

				}

				break;
			}
		}
		return foundPendingNodes;
	}

}
