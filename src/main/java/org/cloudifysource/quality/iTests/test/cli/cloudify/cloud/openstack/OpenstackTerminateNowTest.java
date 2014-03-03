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
package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.openstack;

import java.util.List;
import java.util.Map;

import org.cloudifysource.domain.cloud.Cloud;
import org.cloudifysource.domain.cloud.compute.ComputeTemplate;
import org.cloudifysource.esc.driver.provisioning.openstack.OpenStackComputeClient;
import org.cloudifysource.esc.driver.provisioning.openstack.OpenStackNetworkClient;
import org.cloudifysource.esc.driver.provisioning.openstack.OpenstackException;
import org.cloudifysource.esc.driver.provisioning.openstack.OpenstackJsonSerializationException;
import org.cloudifysource.esc.driver.provisioning.openstack.rest.Router;
import org.cloudifysource.esc.driver.provisioning.openstack.rest.SecurityGroup;
import org.cloudifysource.quality.iTests.test.cli.cloudify.CommandTestUtils;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.NewAbstractCloudTest;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.services.hpgrizzly.HpGrizzlyCloudService;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

public class OpenstackTerminateNowTest extends NewAbstractCloudTest {
	
	private static final String PROPERTY_OPENSTACK_URL = "openstackUrl";
	private static final String PROPERTY_API_KEY = "apiKey";
	private static final String PROPERTY_USER = "user";
	private static final String PROPERTY_TENANT = "tenant";
	private static final String USM_SERVICES_PATH = "src/main/resources/apps/USM/usm/";
	private static final String SERVICE_NAME = "simple-with-network";
	private static final String NON_CLOUDIFY_SECURITY_GROUP = "TestTerminateNowSecurityGroup";
	private static final String SHARED_TESTING_ROUTER = "hpclouddev-router";
	private HpGrizzlyCloudService service;
	private Cloud cloud;
	private OpenStackComputeClient computeClient;
	private OpenStackNetworkClient networkClient;
	private boolean teardownRequired = true;
	
	
	@BeforeTest(alwaysRun = true)
	public void init() throws Exception {
		service = new HpGrizzlyCloudService();
		super.bootstrap(service);
		cloud = service.getCloud();
		initClients();
        
        // install service with network
		String simpleServicePath = CommandTestUtils.getPath(USM_SERVICES_PATH + SERVICE_NAME);
        super.installServiceAndWait(simpleServicePath, SERVICE_NAME);
	}
	
	
	@AfterClass(alwaysRun = true)
	public void clean() throws Exception {
		if (teardownRequired) {
			super.teardown();
		}
		
		// delete the custom security group
		if (networkClient != null) {
			List<SecurityGroup> customSecurityGroups = 
					networkClient.getSecurityGroupsByPrefix(NON_CLOUDIFY_SECURITY_GROUP);
			for (SecurityGroup group : customSecurityGroups) {
				deleteSecurityGroup(group.getId());
			} 
		}
		
	}
	

	@Override
	protected String getCloudName() {
		return "hp-grizzly";
	}
	

	@Override
	protected boolean isReusableCloud() {
		// TODO Auto-generated method stub
		return false;
	}
	
	
	@Test
	public void testTerminateNow() throws Exception {
		
		String managementPrefix = cloud.getProvider().getManagementGroup();
        String agentPrefix = cloud.getProvider().getMachineNamePrefix();
        
        // create a security group without the prefix, named : "TestTerminateNowSecurityGroup"
        SecurityGroup customSecurityGroup = createSecurityGroup(NON_CLOUDIFY_SECURITY_GROUP);

        // verify the expected resources are found
        assertServersCount(1, managementPrefix);
        assertServersCount(1, agentPrefix);
        assertRoutersCount(1, SHARED_TESTING_ROUTER);
        assertNetworksCount(2, managementPrefix);
        assertSecurityGroupsCount(5, managementPrefix);
        assertSecurityGroupsCount(1, NON_CLOUDIFY_SECURITY_GROUP);
        
        // run terminate-now
        this.cloudService.getBootstrapper().terminateNow(true);
        super.teardown();
        
        // verify all resourced were removed, and only the custom security group is left
        assertServersCount(0, managementPrefix);
        teardownRequired = false;	// if we got here the management server is down, cancel the teardown on @AfterClass
        assertServersCount(0, agentPrefix);
        assertRoutersCount(1, SHARED_TESTING_ROUTER);
        assertNetworksCount(0, managementPrefix);
        assertSecurityGroupsCount(0, managementPrefix);
        assertSecurityGroupsCount(1, NON_CLOUDIFY_SECURITY_GROUP);
        
        deleteSecurityGroup(customSecurityGroup.getId());
        assertSecurityGroupsCount(0, NON_CLOUDIFY_SECURITY_GROUP);
        
	}
	
	
	private void initClients() {
		try {
			
			Map<String, Object> properties = service.getProperties();
			String tenant = properties.get(PROPERTY_TENANT).toString();
			String username = properties.get(PROPERTY_USER).toString();
			String password = properties.get(PROPERTY_API_KEY).toString();
			String endpoint = properties.get(PROPERTY_OPENSTACK_URL).toString();

			// calculating the region by the image id of the management template
			String managementTemplateName = cloud.getConfiguration().getManagementMachineTemplate();
			ComputeTemplate managementTemplate = cloud.getCloudCompute().getTemplates().get(managementTemplateName);
			String region = managementTemplate.getImageId().split("/")[0];
			
			computeClient = new OpenStackComputeClient(endpoint, username, password, tenant, region);
			networkClient = new OpenStackNetworkClient(endpoint, username, password, tenant, region);
		} catch (OpenstackJsonSerializationException e) {
			throw new IllegalStateException(e);
		}		
	}
	
	
	private SecurityGroup createSecurityGroup(final String securityGroupName) throws OpenstackException {
		final SecurityGroup request = new SecurityGroup();
		request.setName(securityGroupName);
		request.setDescription("Cloudify generated security group " + securityGroupName);
		return networkClient.createSecurityGroupsIfNotExist(request);
	}
	
	
	private void deleteSecurityGroup(final String securityGroupId) throws OpenstackException {
		networkClient.deleteSecurityGroup(securityGroupId);
	}
	
	
	private void assertServersCount(final int expectedServersCount, final String prefix) throws OpenstackException {
		int serversCount = computeClient.getServersByPrefix(prefix).size();
		
        assertTrue("expected " + expectedServersCount + " server(s) with prefix " + prefix + " but found " 
        		+ serversCount, serversCount == expectedServersCount);
	}

	
	private void assertRoutersCount(final int expetcedRoutersCount, final String prefix) throws OpenstackException {
		List<Router> routers = networkClient.getRouters();
        int routersCount = 0;
        if (routers != null) {
        	for (Router router : routers) {
        		if (router.getName().startsWith(prefix)) {
        			routersCount++;
        		}
        	}
        }
        
        assertTrue("expected " + expetcedRoutersCount + " router(s) with prefix " + prefix + " but found " 
        		+ routersCount, routersCount == expetcedRoutersCount);
	}
	
	
	private void assertNetworksCount(final int expectedNetworksCount, final String prefix) throws OpenstackException {
		int networksCount = networkClient.getNetworkByPrefix(prefix).size();
        assertTrue("expected " + expectedNetworksCount + " network(s) with prefix " + prefix + " but found " 
        		+ networksCount, networksCount == expectedNetworksCount);
	}
	
	
	private void assertSecurityGroupsCount(final int expectedGroupsCount, final String prefix) throws OpenstackException {
		int groupsCount = networkClient.getSecurityGroupsByPrefix(prefix).size();
        assertTrue("expected " + expectedGroupsCount + " security groups with prefix " + prefix + " but found " 
        		+ groupsCount, groupsCount == expectedGroupsCount);
	}
	
}
