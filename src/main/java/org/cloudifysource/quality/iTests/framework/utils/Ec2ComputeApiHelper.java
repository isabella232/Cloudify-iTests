package org.cloudifysource.quality.iTests.framework.utils;

import java.io.IOException;
import java.util.Properties;

import org.cloudifysource.domain.cloud.Cloud;
import org.cloudifysource.esc.jclouds.JCloudsDeployer;
import org.jclouds.compute.domain.NodeMetadata;

public class Ec2ComputeApiHelper {
	
	private JCloudsDeployer deployer;
	
	public Ec2ComputeApiHelper(final Cloud cloud) throws IOException {
		deployer = new JCloudsDeployer(cloud.getProvider().getProvider(), cloud.getUser().getUser(),
				cloud.getUser().getApiKey(), new Properties());
	}
	
	public NodeMetadata getServerWithIp(String ip) {
		return deployer.getServerWithIP(ip);
	}
	
	public NodeMetadata getServerByName(String name) {
		return deployer.getServerByName(name);
	}

}
