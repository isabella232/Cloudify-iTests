/*******************************************************************************
 * Copyright (c) 2013 GigaSpaces Technologies Ltd. All rights reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.cloudifysource.quality.iTests.framework.utils.compute;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.cloudifysource.domain.cloud.Cloud;
import org.cloudifysource.domain.cloud.compute.ComputeTemplate;
import org.cloudifysource.esc.driver.provisioning.CloudProvisioningException;
import org.cloudifysource.esc.driver.provisioning.MachineDetails;
import org.cloudifysource.esc.driver.provisioning.openstack.OpenStackComputeClient;
import org.cloudifysource.esc.driver.provisioning.openstack.OpenstackException;
import org.cloudifysource.esc.driver.provisioning.openstack.rest.NovaServer;

public class OpenstackComputeApiHelper implements ComputeApiHelper {
	
	public static final String OPENSTACK_ENDPOINT = "openstack.endpoint";		// example: openstack.endpoint="https://<IP>:5000/v2.0/"
	public static final String OPT_COMPUTE_SERVICE_NAME = "computeServiceName";	// default="nova"
	

	private OpenStackComputeClient computeClient;
	
	/**
	 * Ctor.
	 * @param cloud The cloud the client connects to
	 * @param templateName The compute template used (set in the cloud groovy file)
	 */
	public OpenstackComputeApiHelper(final Cloud cloud, final String templateName) {

		ComputeTemplate computeTemplate = cloud.getCloudCompute().getTemplates().get(templateName);
		if (computeTemplate == null) {
			throw new IllegalStateException("Template with name \"" + templateName + "\" could not be found.");
		}

		String endpoint = null;
		final Map<String, Object> overrides = computeTemplate.getOverrides();
		if (overrides != null && !overrides.isEmpty()) {
			endpoint = (String) overrides.get(OPENSTACK_ENDPOINT);
		}

		final String region = computeTemplate.getImageId().split("/")[0];
		final String cloudUser = cloud.getUser().getUser();
		final String password = cloud.getUser().getApiKey();
		final String computeServiceName = (String) computeTemplate.getOptions().get(OPT_COMPUTE_SERVICE_NAME);

		if (cloudUser == null || password == null) {
			throw new IllegalStateException("Cloud user or password not found");
		}

		final StringTokenizer st = new StringTokenizer(cloudUser, ":");
		final String tenant = st.hasMoreElements() ? (String) st.nextToken() : null;
		final String username = st.hasMoreElements() ? (String) st.nextToken() : null;

		try {
			this.computeClient = new OpenStackComputeClient(endpoint, username, password, tenant, region,
					computeServiceName);
		} catch (final Exception e) {
			throw new RuntimeException("Failed to initialize compute helper : " + e.getMessage(), e);
		}

	}

	@Override
	public Set<MachineDetails> getServersContaining(final String partialName) throws CloudProvisioningException {
		Set<MachineDetails> machineDetailsSet = new HashSet<MachineDetails>();
		
		List<NovaServer> servers;
		
		try {
			servers = computeClient.getServers();
		} catch (OpenstackException e) {
			throw new CloudProvisioningException("Failed to get servers from Openstack, reported error: " 
					+ e.getMessage(), e);
		}
		
		for (NovaServer server : servers) {
			if (server.getName().contains(partialName)) {
				MachineDetails md = new MachineDetails();
				md.setMachineId(server.getId());
				md.setLocationId(server.getAvailabilityZone());
				machineDetailsSet.add(md);
			}
		}

		return machineDetailsSet;
	}

	
	@Override
	public MachineDetails getServerById(final String serverId) throws CloudProvisioningException {
		
		MachineDetails md = new MachineDetails();
		NovaServer server;
		
		try {
			server = computeClient.getServerDetails(serverId);
		} catch (OpenstackException e) {
			throw new CloudProvisioningException("Failed to get server details from Openstack, server id: " 
					+ serverId + ", reported error: " + e.getMessage(), e);
		}
		
		if (server == null) {
			throw new CloudProvisioningException("Failed to get server details from Openstack, server id: " 
					+ serverId);
		}
		
		md.setMachineId(serverId);
		md.setLocationId(server.getAvailabilityZone());
		
		return md;
	}

	@Override
	public void shutdownServerById(String serverId) throws CloudProvisioningException {
		try {
			computeClient.deleteServer(serverId);
		} catch (OpenstackException e) {
			throw new CloudProvisioningException("Failed to shutdown a server on Openstack, server id: " 
					+ serverId + " , reported error: " + e.getMessage(), e);
		}
	}

}
