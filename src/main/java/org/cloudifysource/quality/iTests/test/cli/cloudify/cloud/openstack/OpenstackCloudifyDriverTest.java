package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.openstack;

import iTests.framework.utils.AssertUtils;

import java.util.Collection;
import java.util.List;

import org.cloudifysource.domain.Service;
import org.cloudifysource.domain.cloud.Cloud;
import org.cloudifysource.domain.cloud.compute.ComputeTemplate;
import org.cloudifysource.domain.cloud.compute.ComputeTemplateNetwork;
import org.cloudifysource.domain.cloud.network.NetworkConfiguration;
import org.cloudifysource.esc.driver.provisioning.MachineDetails;
import org.cloudifysource.esc.driver.provisioning.openstack.OpenstackException;
import org.cloudifysource.esc.driver.provisioning.openstack.SecurityGroupNames;
import org.cloudifysource.esc.driver.provisioning.openstack.rest.Network;
import org.cloudifysource.esc.driver.provisioning.openstack.rest.Subnet;
import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.beust.jcommander.internal.Lists;

/**
 * To run this test, you will have to create the credential file here : ./src/main/resources/credentials/cloud/openstack/openstack.properties.<br />
 * An property example:
 * 
 * <pre>
 * user=_your_user_
 * tenant=_your_tenant_
 * apiKey=_your_apiKey_
 * keyPair=_your_keyPair_
 * 
 * endpoint=http://_openstack_endpoint_address_:5000/v2.0/
 * 
 * hardwareId=RegionOne/2
 * imageId=RegionOne/8b62f8c3-1275-47c3-a884-a295167d109a
 * </pre>
 * 
 * @author victor
 * 
 */
public class OpenstackCloudifyDriverTest extends AbstractTestSupport {

	private OpenstackCloudifyDriverLauncher launcher;

	@BeforeMethod
	public void init() throws Exception {
		launcher = new OpenstackCloudifyDriverLauncher();
		launcher.setNetworkServiceName("quantum");
	}
	
	    @AfterMethod
	    public void clean() throws Exception {
	        launcher.cleanOpenstackResources();
	    }

	@Test
	public void testStartManagementMachinesWithDefaultTemplate() throws Exception {
		Cloud cloud = launcher.createCloud("/default/openstack-cloud.groovy");
		MachineDetails[] mds = launcher.startManagementMachines(cloud);
		for (MachineDetails md : mds) {
			assertNotNull("Machine id is null", md.getMachineId());
			assertNotNull("Private ip is null", md.getPrivateAddress());
			assertNotNull("Public ip is null", md.getPublicAddress());
			launcher.assertFloatingIpBindToServer(md.getPublicAddress(), md.getMachineId());
		}

		String prefix = cloud.getProvider().getManagementGroup();
		launcher.assertRouterExists(prefix);
		NetworkConfiguration networkConfiguration = cloud.getCloudNetwork().getManagement().getNetworkConfiguration();
		launcher.assertNetworkExists(prefix + networkConfiguration.getName());
		launcher.assertSubnetExists(prefix + networkConfiguration.getName(), networkConfiguration.getSubnets().get(0).getName());
		launcher.assertSubnetSize(prefix + networkConfiguration.getName(), 1);

		launcher.stopManagementMachines(cloud);
	}

	@Test
	public void testStartMachineWithDefaultTemplateAndSecgroupService() throws Exception {
		Cloud cloud = launcher.createCloud("/default/openstack-cloud.groovy");
		Service service = launcher.getService("secgroups/securityGroup-service.groovy");

		MachineDetails md = launcher.startMachineWithManagement(service, cloud);
		assertNotNull("Machine id is null", md.getMachineId());
		assertNotNull("Private ip is null", md.getPrivateAddress());
		assertNotNull("Public ip is null", md.getPublicAddress());
		launcher.assertFloatingIpBindToServer(md.getPublicAddress(), md.getMachineId());

		String prefix = cloud.getProvider().getManagementGroup();
		NetworkConfiguration networkConfiguration = cloud.getCloudNetwork().getTemplates().get("APPLICATION_NET");
		launcher.assertNetworkExists(prefix + networkConfiguration.getName());
		launcher.assertSubnetExists(prefix + networkConfiguration.getName(), networkConfiguration.getSubnets().get(0).getName());
		launcher.assertSubnetSize(prefix + networkConfiguration.getName(), 1);

		launcher.stopMachineWithManagement(service, cloud, md.getPrivateAddress());
	}

	@Test
	public void testStartMachineWithComputeNetworkTemplateAndSecgroupService() throws Exception {
	
		launcher.setSkipExternalNetworking(true);
		Cloud cloud = launcher.createCloud("/computeNetwork/computeNetwork-cloud.groovy");
		Service service = launcher.getService("secgroups/securityGroup-service.groovy");

		Collection<ComputeTemplate> allComputeTemplates = cloud.getCloudCompute().getTemplates().values();

		List<String> createdNetworks = createNetworksFromComputeTemplates (allComputeTemplates);

		for (String networkName : createdNetworks) {    	
			launcher.assertNetworkExists(networkName);
		}

		MachineDetails md = launcher.startMachineWithManagement(service, cloud);
		assertNotNull("Machine id is null", md.getMachineId());
		assertNotNull("Private ip is null", md.getPrivateAddress());
		AssertUtils.assertNull("Public ip should be null", md.getPublicAddress());
		launcher.assertNoFloatingIp(md.getMachineId());

		String prefix = cloud.getProvider().getManagementGroup();
		SecurityGroupNames secgroupnames = new SecurityGroupNames(prefix, OpenstackCloudifyDriverLauncher.DEFAULT_APPLICATION_NAME, service.getName());
		launcher.assertSecurityGroupExists(secgroupnames.getApplicationName());
		launcher.assertSecurityGroupExists(secgroupnames.getServiceName());
		launcher.assertSecurityGroupIncomingRulesNumber(secgroupnames.getServiceName(), service.getNetwork().getAccessRules().getIncoming().size());
		launcher.assertNoRouter(prefix);

		launcher.stopMachineWithManagement(service, cloud, md.getPrivateAddress());
		cleanCreatedTestNetworks(createdNetworks);
		
		for (String networkName : createdNetworks) {    	
			launcher.assertNetworkNotExists(networkName);
		}

	}

	/**
	 * Creates networks that were specified in service DSL for test purpose. 
	 * @param allComputeTemplates
	 * @return
	 * @throws OpenstackException
	 */
	private List<String> createNetworksFromComputeTemplates(Collection<ComputeTemplate> allComputeTemplates) throws OpenstackException {
		List<String> createdNetworks = Lists.newArrayList(); 
				
		Subnet subnet = new Subnet();
		subnet.setName("subnet");
		subnet.setCidr("192.168.0.0/24");
		subnet.setIpVersion("4");
		
		for (ComputeTemplate ct : allComputeTemplates) {
			ComputeTemplateNetwork computeNetwork = ct.getComputeNetwork();

			if (computeNetwork != null) {

				List<String> networks = computeNetwork.getNetworks();  			
				for (String networkName : networks) {
					Network network = new Network(); 
					network.setName(networkName);
					String networkId = launcher.getNetworkApi().createNetworkIfNotExists(network).getId();				
					subnet.setNetworkId(networkId);
					createdNetworks.add(networkName);
					launcher.getNetworkApi().createSubnet(subnet);
				}		
			}
		}
		return createdNetworks;
	}
	
	/**
	 * Removes networks with the gives names from openstack environment 
	 * @param createdNetworks
	 * @throws OpenstackException
	 */
	private void  cleanCreatedTestNetworks(List<String> createdNetworks) throws OpenstackException {
		
		for (String networkName : createdNetworks) {    	
			launcher.getNetworkApi().deleteNetworkByName(networkName);
		}	
	}
	
}
