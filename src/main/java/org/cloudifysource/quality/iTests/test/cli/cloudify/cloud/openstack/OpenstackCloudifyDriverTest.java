package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.openstack;

import iTests.framework.utils.AssertUtils;

import java.util.concurrent.TimeUnit;

import junit.framework.Assert;

import org.cloudifysource.domain.Service;
import org.cloudifysource.domain.cloud.Cloud;
import org.cloudifysource.domain.cloud.network.NetworkConfiguration;
import org.cloudifysource.dsl.internal.ServiceReader;
import org.cloudifysource.esc.driver.provisioning.CloudProvisioningException;
import org.cloudifysource.esc.driver.provisioning.ComputeDriverConfiguration;
import org.cloudifysource.esc.driver.provisioning.MachineDetails;
import org.cloudifysource.esc.driver.provisioning.openstack.OpenStackCloudifyDriver;
import org.cloudifysource.esc.driver.provisioning.openstack.OpenStackNetworkClient;
import org.cloudifysource.esc.driver.provisioning.openstack.OpenstackException;
import org.cloudifysource.esc.driver.provisioning.openstack.SecurityGroupNames;
import org.cloudifysource.esc.driver.provisioning.openstack.rest.Network;
import org.cloudifysource.esc.driver.provisioning.openstack.rest.Subnet;
import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

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

    private static final String SOME_INTERNAL_NETWORK_1 = "SOME_INTERNAL_NETWORK_1";
    private static final String SOME_INTERNAL_NETWORK_2 = "SOME_INTERNAL_NETWORK_2";

    private OpenstackCloudifyDriverLauncher launcher;

    /**
     * Create supposing existing networks.
     */
    public void createExpectedExistingNetworks(OpenStackNetworkClient networkApi) throws OpenstackException {
        createExistingNetwork(networkApi, SOME_INTERNAL_NETWORK_1, 1);
        createExistingNetwork(networkApi, SOME_INTERNAL_NETWORK_2, 2);
    }

    private void createExistingNetwork(OpenStackNetworkClient networkApi, String networkName, int nbSubnet) throws OpenstackException {
        Network network = new Network();
        network.setName(networkName);
        String networkId = networkApi.createNetworkIfNotExists(network).getId();

        for (int i = 0; i < nbSubnet; i++) {
            Subnet requestSubnet = new Subnet();
            requestSubnet.setName(networkName + "_subnet_" + i);
            requestSubnet.setCidr("15" + i + ".0.0.0/24");
            requestSubnet.setIpVersion("4");
            requestSubnet.setNetworkId(networkId);
            networkApi.createSubnet(requestSubnet);
        }

    }

    /**
     * Clean supposing existing networks
     */
    public void cleanExpectedExistingNetworks(OpenStackNetworkClient networkApi) throws OpenstackException {
        String[] existingNetworkNames = { SOME_INTERNAL_NETWORK_1, SOME_INTERNAL_NETWORK_2 };
        for (String name : existingNetworkNames) {
            networkApi.deleteNetworkByName(name);
        }

    }

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
    public void testtest() throws Exception {

        Cloud cloud = ServiceReader.readCloudFromDirectory("C:/cloudify-deployment/gigaspaces-cloudify-2.7.0-m8-b5992-64/clouds/openstack");

        final ComputeDriverConfiguration configuration = new ComputeDriverConfiguration();
        configuration.setCloud(cloud);
        configuration.setManagement(true);

        OpenStackCloudifyDriver driver = new OpenStackCloudifyDriver();
        driver.setConfig(configuration);
        MachineDetails[] mds = driver.startManagementMachines(null, 60, TimeUnit.MINUTES);
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
        SecurityGroupNames secgroupnames = new SecurityGroupNames(prefix, OpenstackCloudifyDriverLauncher.DEFAULT_APPLICATION_NAME, service.getName());
        launcher.assertSecurityGroupIncomingRulesNumber(secgroupnames.getServiceName(), service.getNetwork().getAccessRules().getIncoming().size());

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

        this.createExpectedExistingNetworks(launcher.getNetworkApi());
        try {

            MachineDetails md = launcher.startMachineWithManagement(service, cloud);
            assertNotNull("Machine id is null", md.getMachineId());
            assertNotNull("Private ip is null", md.getPrivateAddress());
            AssertUtils.assertNull("Public ip should be null", md.getPublicAddress());
            launcher.assertNoFloatingIp(md.getMachineId());

            String prefix = cloud.getProvider().getManagementGroup();
            SecurityGroupNames secgroupnames = new SecurityGroupNames(prefix, OpenstackCloudifyDriverLauncher.DEFAULT_APPLICATION_NAME, service.getName());
            launcher.assertSecurityGroupIncomingRulesNumber(secgroupnames.getServiceName(), service.getNetwork().getAccessRules().getIncoming().size());
            launcher.assertNoRouter(prefix);

            launcher.stopMachineWithManagement(service, cloud, md.getPrivateAddress());
        } finally {
            this.cleanExpectedExistingNetworks(launcher.getNetworkApi());
        }
    }

    /**
     * Management Network template is defined in Management compute template, but its doesn't
     * exist in the environment.
     * 
     * @throws Exception
     */
    @Test(expectedExceptions = CloudProvisioningException.class)
    public void testStartManagementWithNotExistingNetworkDefinedInComputeTemplate() throws Exception {
        launcher.setSkipExternalNetworking(true);
        Cloud cloud = launcher.createCloud("/mng-network-template-in-compute/mng-network-template-in-compute-cloud.groovy");

        launcher.startManagementMachines(cloud);
        Assert.fail("CloudProvisioningException should be thrown, the defined network doesn't exist in the environment");
    }

    /**
     * Management Network template is defined in Management compute template and it does
     * exist already in the environment.
     * 
     * @throws Exception
     */
    @Test
    public void testStartManagementWithExistingNetworkDefinedInComputeTemplate() throws Exception {
        launcher.setSkipExternalNetworking(true);
        Cloud cloud = launcher.createCloud("/mng-network-template-in-compute/mng-network-template-in-compute-cloud.groovy");
        this.createExpectedExistingNetworks(launcher.getNetworkApi());
        try {

            // checks the management machine
            MachineDetails md = launcher.startManagementMachines(cloud)[0];
            Assert.assertNotNull(md.getMachineId());
            Assert.assertNotNull(md.getPrivateAddress());

            launcher.stopManagementMachines(cloud);
        } finally {
            this.cleanExpectedExistingNetworks(launcher.getNetworkApi());
        }
    }

    /**
     * Installation of a service which specify with a incorrect network template.
     * TODO Should be a unit test.
     * 
     * @throws Exception
     */
    @Test(expectedExceptions = CloudProvisioningException.class)
    public void testAgentWithNoNetworkTemplateDefined() throws Exception {
        Cloud cloud = launcher.createCloud("/default/openstack-cloud.groovy");
        Service service = launcher.getService("agt-network-template-not-exist/agt-network-template-not-exist-service.groovy");
        launcher.startMachineWithManagement(service, cloud);
        Assert.fail("CloudProvisioningException should be thrown, the defined template network doesn't exist in DSL");
    }

}
