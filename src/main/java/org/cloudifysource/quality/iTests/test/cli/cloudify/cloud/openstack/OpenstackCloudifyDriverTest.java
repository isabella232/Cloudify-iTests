package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.openstack;

import iTests.framework.utils.AssertUtils;
import junit.framework.Assert;

import org.cloudifysource.domain.ComputeDetails;
import org.cloudifysource.domain.Service;
import org.cloudifysource.domain.ServiceNetwork;
import org.cloudifysource.domain.cloud.Cloud;
import org.cloudifysource.domain.cloud.network.NetworkConfiguration;
import org.cloudifysource.esc.driver.provisioning.CloudProvisioningException;
import org.cloudifysource.esc.driver.provisioning.MachineDetails;
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

    private static final String TEMPLATE_APPLICATION_NET2 = "APPLICATION_NET2";
    private static final String COMPUTE_SOME_INTERNAL_NETWORK_1 = "SOME_INTERNAL_NETWORK_1";
    private static final String COMPUTE_SOME_INTERNAL_NETWORK_2 = "SOME_INTERNAL_NETWORK_2";

    private OpenstackCloudifyDriverLauncher launcher;

    /**
     * Create supposing existing networks.
     */
    public void createExpectedExistingNetworks(OpenStackNetworkClient networkApi) throws OpenstackException {
        createExistingNetwork(networkApi, COMPUTE_SOME_INTERNAL_NETWORK_1, 1);
        createExistingNetwork(networkApi, COMPUTE_SOME_INTERNAL_NETWORK_2, 2);
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
        String[] existingNetworkNames = { COMPUTE_SOME_INTERNAL_NETWORK_1, COMPUTE_SOME_INTERNAL_NETWORK_2 };
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

    // ***************************************************************
    // ******** START MANAGEMENT MACHINE TESTS
    // ***************************************************************

    @Test
    public void testStartManagementMachinesWithNetworkTemplate() throws Exception {
        Cloud cloud = launcher.createCloud("/cloudNetwork/cloudNetwork-cloud.groovy");
        MachineDetails[] mds = launcher.startManagementMachines(cloud);
        for (MachineDetails md : mds) {
            assertNotNull("Machine id is null", md.getMachineId());
            assertNotNull("Private ip is null", md.getPrivateAddress());
            assertNotNull("Public ip is null", md.getPublicAddress());
            launcher.assertFloatingIpBindToServer(md);
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
    public void testStartManagementMachinesWithNetworkTemplateAndMultipleSubnets() throws Exception {
        Cloud cloud = launcher.createCloud("/cloudNetwork/multipleSubnets-cloud.groovy");
        MachineDetails[] mds = launcher.startManagementMachines(cloud);

        String prefix = cloud.getProvider().getManagementGroup();

        for (MachineDetails md : mds) {
            assertNotNull("Machine id is null", md.getMachineId());
            assertNotNull("Private ip is null", md.getPrivateAddress());
            assertTrue("Private ip is not from subnet 177.70.0.0/24", md.getPrivateAddress().startsWith("177.70.0."));
            assertNotNull("Public ip is null", md.getPublicAddress());
            launcher.assertFloatingIpBindToServer(md);

            NetworkConfiguration networkConfiguration = cloud.getCloudNetwork().getManagement().getNetworkConfiguration();
            launcher.assertRouterExists(prefix);
            launcher.assertNetworkExists(prefix + networkConfiguration.getName());
            launcher.assertSubnetExists(prefix + networkConfiguration.getName(), networkConfiguration.getSubnets().get(0).getName());
            launcher.assertSubnetExists(prefix + networkConfiguration.getName(), networkConfiguration.getSubnets().get(1).getName());
            launcher.assertSubnetSize(prefix + networkConfiguration.getName(), 2);
            launcher.assertVMBoundToNetwork(md.getMachineId(), prefix + networkConfiguration.getName());
        }

        launcher.stopManagementMachines(cloud);
    }

    /**
     * Management Network template is defined in Management compute template and it does
     * exist already in the environment.
     * 
     * @throws Exception
     */
    @Test
    public void testStartManagementWithComputeNetwork() throws Exception {
        launcher.setSkipExternalNetworking(true);
        Cloud cloud = launcher.createCloud("/computeNetwork/computeNetwork-cloud.groovy");
        try {
            this.createExpectedExistingNetworks(launcher.getNetworkApi());

            MachineDetails[] mds = launcher.startManagementMachines(cloud);
            for (MachineDetails md : mds) {
                assertNotNull("Machine id is null", md.getMachineId());
                assertNotNull("Private ip is null", md.getPrivateAddress());
                assertTrue("Private ip is not from subnet 150.0.0.0/24", md.getPrivateAddress().startsWith("150.0.0."));
                launcher.assertNoFloatingIp(md.getMachineId());
                launcher.assertVMBoundToNetwork(md.getMachineId(), COMPUTE_SOME_INTERNAL_NETWORK_1);
                launcher.assertVMBoundToNetwork(md.getMachineId(), COMPUTE_SOME_INTERNAL_NETWORK_2);
                launcher.stopManagementMachines(cloud);
            }
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
    public void testStartManagementWithComputeTemplateButNetworksDoNotExist() throws Exception {
        launcher.setSkipExternalNetworking(true);
        Cloud cloud = launcher.createCloud("/computeNetwork/computeNetwork-cloud.groovy");
        launcher.startManagementMachines(cloud);
        Assert.fail("CloudProvisioningException should be thrown, the defined network doesn't exist in the environment");
    }

    @Test(expectedExceptions = CloudProvisioningException.class, enabled = false)
    // Should be JUnit test
    public void testStartManagementWithNoNetworksAtAll() throws Exception {
        launcher.setSkipExternalNetworking(true);
        Cloud cloud = launcher.createCloud("/noNetworkAtAll/noNetworkAtAll-cloud.groovy");
        launcher.startManagementMachines(cloud);
        Assert.fail("CloudProvisioningException should be thrown, the defined network doesn't exist in the environment");
    }

    // ***************************************************************
    // ******** START MACHINE TESTS
    // ***************************************************************

    @Test
    public void testStartMachineWithDefaultTemplate() throws Exception {
        Cloud cloud = launcher.createCloud("/cloudNetwork/cloudNetwork-cloud.groovy");
        Service service = launcher.getService("secgroups/securityGroup-service.groovy");

        MachineDetails md = launcher.startMachineWithManagement(service, cloud);

        assertNotNull("Machine id is null", md.getMachineId());
        assertNotNull("Private ip is null", md.getPrivateAddress());
        assertTrue("Private ip is not from subnet 177.70.0.0/24. Got " + md.getPrivateAddress(), md.getPrivateAddress().startsWith("177.70.0."));
        assertNotNull("Public ip is null", md.getPublicAddress());
        launcher.assertFloatingIpBindToServer(md);

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
    public void testStartMachineWithMultipleSubnets() throws Exception {
        Cloud cloud = launcher.createCloud("/cloudNetwork/multipleSubnets-cloud.groovy");
        Service service = launcher.getService("secgroups/securityGroup-service.groovy");
        ServiceNetwork serviceNetwork = new ServiceNetwork();
        serviceNetwork.setTemplate(TEMPLATE_APPLICATION_NET2);
        service.setNetwork(serviceNetwork);
        MachineDetails md = launcher.startMachineWithManagement(service, cloud);

        assertNotNull("Machine id is null", md.getMachineId());
        assertNotNull("Private ip is null", md.getPrivateAddress());
        assertTrue("Private ip is not from subnet 177.70.0.0/24. Got " + md.getPrivateAddress(), md.getPrivateAddress().startsWith("177.70.0."));
        assertNotNull("Public ip is null", md.getPublicAddress());
        launcher.assertFloatingIpBindToServer(md);

        String prefix = cloud.getProvider().getManagementGroup();
        SecurityGroupNames secgroupnames = new SecurityGroupNames(prefix, OpenstackCloudifyDriverLauncher.DEFAULT_APPLICATION_NAME, service.getName());
        launcher.assertSecurityGroupIncomingRulesNumber(secgroupnames.getServiceName(), 0);

        NetworkConfiguration mngNetConfig = cloud.getCloudNetwork().getManagement().getNetworkConfiguration();
        NetworkConfiguration netConfig = cloud.getCloudNetwork().getTemplates().get(TEMPLATE_APPLICATION_NET2);
        launcher.assertNetworkExists(prefix + netConfig.getName());
        launcher.assertSubnetExists(prefix + netConfig.getName(), netConfig.getSubnets().get(0).getName());
        launcher.assertSubnetExists(prefix + netConfig.getName(), netConfig.getSubnets().get(1).getName());
        launcher.assertSubnetSize(prefix + netConfig.getName(), 2);
        launcher.assertVMBoundToNetwork(md.getMachineId(), prefix + mngNetConfig.getName());
        launcher.assertVMBoundToNetwork(md.getMachineId(), prefix + netConfig.getName());

        launcher.stopMachineWithManagement(service, cloud, md.getPrivateAddress());
    }

    @Test
    public void testStartMachineWithComputeNetworkUsingManagerNetwork() throws Exception {

        launcher.setSkipExternalNetworking(true);
        Cloud cloud = launcher.createCloud("/computeNetwork/computeNetwork-cloud.groovy");
        Service service = launcher.getService("secgroups/securityGroup-service.groovy");

        try {
            this.createExpectedExistingNetworks(launcher.getNetworkApi());

            MachineDetails md = launcher.startMachineWithManagement(service, cloud);
            assertNotNull("Machine id is null", md.getMachineId());
            assertNotNull("Private ip is null", md.getPrivateAddress());
            assertTrue("Private ip is not from subnet 177.70.0.0/24. Got " + md.getPrivateAddress(), md.getPrivateAddress().startsWith("177.70.0."));
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

    @Test(enabled = false)
    // TODO review network configuration flow
    public void testStartMachineWithComputeNetwork() throws Exception {
        Cloud cloud = launcher.createCloud("/computeNetwork/managerAppli-cloud.groovy");
        Service service = launcher.getService("secgroups/securityGroup-service.groovy");
        ComputeDetails compute = new ComputeDetails();
        compute.setTemplate("APPLI");
        service.setCompute(compute);

        try {
            this.createExpectedExistingNetworks(launcher.getNetworkApi());

            MachineDetails md = launcher.startMachineWithManagement(service, cloud);
            assertNotNull("Machine id is null", md.getMachineId());
            assertNotNull("Private ip is null", md.getPrivateAddress());
            assertTrue("Private ip is not from subnet 150.0.0.0/24. Got " + md.getPrivateAddress(), md.getPrivateAddress().startsWith("150.0.0."));
        } finally {
            this.cleanExpectedExistingNetworks(launcher.getNetworkApi());
        }
    }

}
