package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.openstack;

import static org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.openstack.OpenstackCloudifyDriverLauncher.COMPUTE_SOME_INTERNAL_NETWORK_1;
import static org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.openstack.OpenstackCloudifyDriverLauncher.COMPUTE_SOME_INTERNAL_NETWORK_2;
import static org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.openstack.OpenstackCloudifyDriverLauncher.TEMPLATE_APPLICATION_NET;
import static org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.openstack.OpenstackCloudifyDriverLauncher.TEMPLATE_APPLICATION_NET2;
import iTests.framework.utils.AssertUtils;

import org.cloudifysource.domain.ComputeDetails;
import org.cloudifysource.domain.Service;
import org.cloudifysource.domain.cloud.Cloud;
import org.cloudifysource.domain.cloud.network.NetworkConfiguration;
import org.cloudifysource.esc.driver.provisioning.CloudProvisioningException;
import org.cloudifysource.esc.driver.provisioning.MachineDetails;
import org.cloudifysource.esc.driver.provisioning.openstack.OpenStackResourcePrefixes;
import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
import org.junit.Assert;
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

    private OpenstackCloudifyDriverLauncher launcher;

    @BeforeMethod
    public void init() throws Exception {
        launcher = new OpenstackCloudifyDriverLauncher();
    }

    @AfterMethod
    public void clean() throws Exception {
        launcher.cleanOpenstackResources();
    }

    // ***************************************************************
    // ******** startManagementMachines tests
    // ***************************************************************

    /**
     * <p>
     * Start management machine only.
     * </p>
     * <ul>
     * <li>Use network template.</li>
     * </ul>
     * 
     */
    @Test
    public void testStartManagementMachinesWithNetworkTemplate() throws Exception {
        Cloud cloud = launcher.createCloud("/cloudNetwork/cloudNetwork-cloud.groovy");

        MachineDetails[] mds = launcher.startManagementMachines(cloud);

        String prefix = cloud.getProvider().getManagementGroup();
        NetworkConfiguration networkConfiguration = cloud.getCloudNetwork().getManagement().getNetworkConfiguration();

        launcher.assertRouterExists(prefix);
        launcher.assertNetworkExists(prefix + networkConfiguration.getName());
        launcher.assertSubnetExists(prefix + networkConfiguration.getName(), networkConfiguration.getSubnets().get(0).getName());
        launcher.assertSubnetSize(prefix + networkConfiguration.getName(), 1);

        for (MachineDetails md : mds) {
            assertNotNull("Machine id is null", md.getMachineId());
            assertNotNull("Private ip is null", md.getPrivateAddress());
            assertTrue("Private ip is not from subnet 177.70.0.0/24", md.getPrivateAddress().startsWith("177.70.0."));
            assertNotNull("Public ip is null", md.getPublicAddress());
            launcher.assertFloatingIpBindToServer(md);
            launcher.assertVMBoundToNetwork(md.getMachineId(), prefix + networkConfiguration.getName());
        }

        launcher.stopManagementMachines(cloud);
    }

    /**
     * <p>
     * Start management machine only.
     * </p>
     * <ul>
     * <li>Use network template with multiple subnets.</li>
     * </ul>
     */
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
     * <p>
     * Start management machine only.
     * </p>
     * <ul>
     * <li>Use computeNetworks.</li>
     * </ul>
     */
    @Test
    public void testStartManagementMachinesWithComputeNetwork() throws Exception {
        launcher.setSkipExternalNetworking(true);
        Cloud cloud = launcher.createCloud("/computeNetwork/computeNetwork-cloud.groovy");
        try {
            launcher.createExpectedExistingNetworks();

            MachineDetails[] mds = launcher.startManagementMachines(cloud);
            for (MachineDetails md : mds) {
                assertNotNull("Machine id is null", md.getMachineId());
                assertNotNull("Private ip is null", md.getPrivateAddress());
                assertTrue("Private ip is not from subnet 151.0.0.0/24", md.getPrivateAddress().startsWith("151.0.0."));
                launcher.assertNoFloatingIp(md.getMachineId());
                launcher.assertVMBoundToNetwork(md.getMachineId(), COMPUTE_SOME_INTERNAL_NETWORK_1);
                launcher.assertVMBoundToNetwork(md.getMachineId(), COMPUTE_SOME_INTERNAL_NETWORK_2);
            }
            launcher.stopManagementMachines(cloud);
        } finally {
            launcher.cleanExpectedExistingNetworks();
        }
    }

    /**
     * <p>
     * Start multiple management machines.
     * </p>
     * <ul>
     * <li>Use network templates</li>
     * </ul>
     */
    @Test
    public void testStartMultipleManagementMachinesWithComputeNetwork() throws Exception {
        int nbManagementMachines = 5;
        launcher.setSkipExternalNetworking(true);
        Cloud cloud = launcher.createCloud("/computeNetwork/computeNetwork-cloud.groovy");
        cloud.getProvider().setNumberOfManagementMachines(nbManagementMachines);
        try {
            launcher.createExpectedExistingNetworks();
            MachineDetails[] mds = launcher.startManagementMachines(cloud);
            for (MachineDetails md : mds) {
                assertNotNull("Machine id is null", md.getMachineId());
                assertNotNull("Private ip is null", md.getPrivateAddress());
                assertTrue("Private ip is not from subnet 151.0.0.0/24", md.getPrivateAddress().startsWith("151.0.0."));
                launcher.assertNoFloatingIp(md.getMachineId());
                launcher.assertVMBoundToNetwork(md.getMachineId(), COMPUTE_SOME_INTERNAL_NETWORK_1);
                launcher.assertVMBoundToNetwork(md.getMachineId(), COMPUTE_SOME_INTERNAL_NETWORK_2);
            }
            launcher.stopManagementMachines(cloud);
        } finally {
            launcher.cleanExpectedExistingNetworks();
        }
    }

    /**
     * <p>
     * Start management machine only.
     * </p>
     * <ul>
     * <li>Use computeNetworks but networks does not exists.</li>
     * </ul>
     */
    @Test(expectedExceptions = CloudProvisioningException.class)
    public void testStartManagementWithComputeTemplateButNetworksDoNotExist() throws Exception {
        launcher.setSkipExternalNetworking(true);
        Cloud cloud = launcher.createCloud("/computeNetwork/computeNetwork-cloud.groovy");
        launcher.startManagementMachines(cloud);
    }

    // ***************************************************************
    // ******** startMachine Tests
    // ***************************************************************

    /**
     * <ul>
     * <li>Start management machine.</li>
     * <li>Start agent.</li>
     * <li>Use network template.</li>
     * </ul>
     */
    @Test
    public void testStartMachineWithNetworkTemplate() throws Exception {
        Cloud cloud = launcher.createCloud("/cloudNetwork/cloudNetwork-cloud.groovy");
        Service service = launcher.getService("secgroups/securityGroup-service.groovy");
        MachineDetails md = launcher.startMachineWithManagement(service, cloud);

        assertNotNull("Machine id is null", md.getMachineId());
        assertNotNull("Private ip is null", md.getPrivateAddress());
        assertTrue("Private ip is not from subnet 177.70.0.0/24. Got " + md.getPrivateAddress(), md.getPrivateAddress().startsWith("177.70.0."));
        assertNotNull("Public ip is null", md.getPublicAddress());
        launcher.assertFloatingIpBindToServer(md);

        String prefix = cloud.getProvider().getManagementGroup();
        OpenStackResourcePrefixes prefixes = new OpenStackResourcePrefixes(prefix, OpenstackCloudifyDriverLauncher.DEFAULT_APPLICATION_NAME, service.getName());
        launcher.assertSecurityGroupIncomingRulesNumber(prefixes.getServiceName(), service.getNetwork().getAccessRules().getIncoming().size());

        NetworkConfiguration networkConfiguration = cloud.getCloudNetwork().getTemplates().get(TEMPLATE_APPLICATION_NET);
        String appliNetworkName = prefixes.getApplicationName() + "-" + networkConfiguration.getName();
        launcher.assertNetworkExists(appliNetworkName);
        launcher.assertSubnetExists(appliNetworkName, networkConfiguration.getSubnets().get(0).getName());
        launcher.assertSubnetSize(appliNetworkName, 1);

        launcher.stopMachineWithManagement(service, cloud, md.getPrivateAddress());
    }

    /**
     * <ul>
     * <li>Start management machine.</li>
     * <li>Start agent.</li>
     * <li>Use network template with multiple subnets.</li>
     * </ul>
     */
    @Test
    public void testStartMachineWithMultipleSubnets() throws Exception {
        Cloud cloud = launcher.createCloud("/cloudNetwork/multipleSubnets-cloud.groovy");
        Service service = launcher.getService("secgroups/securityGroup-service.groovy");
        service.getNetwork().setTemplate(TEMPLATE_APPLICATION_NET2);
        MachineDetails md = launcher.startMachineWithManagement(service, cloud);

        assertNotNull("Machine id is null", md.getMachineId());
        assertNotNull("Private ip is null", md.getPrivateAddress());
        assertTrue("Private ip is not from subnet 177.70.0.0/24. Got " + md.getPrivateAddress(), md.getPrivateAddress().startsWith("177.70.0."));
        assertNotNull("Public ip is null", md.getPublicAddress());
        launcher.assertFloatingIpBindToServer(md);

        String prefix = cloud.getProvider().getManagementGroup();
        OpenStackResourcePrefixes prefixes = new OpenStackResourcePrefixes(prefix, OpenstackCloudifyDriverLauncher.DEFAULT_APPLICATION_NAME, service.getName());
        launcher.assertSecurityGroupIncomingRulesNumber(prefixes.getServiceName(), service.getNetwork().getAccessRules().getIncoming().size());

        NetworkConfiguration mngNetConfig = cloud.getCloudNetwork().getManagement().getNetworkConfiguration();
        NetworkConfiguration netConfig = cloud.getCloudNetwork().getTemplates().get(TEMPLATE_APPLICATION_NET2);
        String appliNetworkName = prefixes.getApplicationName() + "-" + netConfig.getName();
        launcher.assertNetworkExists(appliNetworkName);
        launcher.assertSubnetExists(appliNetworkName, netConfig.getSubnets().get(0).getName());
        launcher.assertSubnetExists(appliNetworkName, netConfig.getSubnets().get(1).getName());
        launcher.assertSubnetSize(appliNetworkName, 2);
        launcher.assertVMBoundToNetwork(md.getMachineId(), prefix + mngNetConfig.getName());
        launcher.assertVMBoundToNetwork(md.getMachineId(), appliNetworkName);

        launcher.stopMachineWithManagement(service, cloud, md.getPrivateAddress());
    }

    /**
     * <ul>
     * <li>Start management machine.</li>
     * <li>Start agent.</li>
     * <li>Use management network</li>
     * <li>Use computeNetwork for application network.</li>
     * </ul>
     */
    @Test
    public void testStartMachineWithComputeNetworkUsingManagerNetwork() throws Exception {

        launcher.setSkipExternalNetworking(true);
        Cloud cloud = launcher.createCloud("/computeNetwork/managerAppli-cloud.groovy");
        Service service = launcher.getService("secgroups/securityGroup-service.groovy");
        ComputeDetails computeDetails = new ComputeDetails();
        computeDetails.setTemplate("APPLI");
        service.setCompute(computeDetails);
        service.getNetwork().setTemplate(null);

        try {
            launcher.createExpectedExistingNetworks();

            MachineDetails md = launcher.startMachineWithManagement(service, cloud);
            assertNotNull("Machine id is null", md.getMachineId());
            assertNotNull("Private ip is null", md.getPrivateAddress());
            // Private IP network should be "SOME_INTERNAL_NETWORK_2"
            assertTrue("Private ip is not from subnet 152.0.0.0/24. Got " + md.getPrivateAddress(), md.getPrivateAddress().startsWith("152.0.0."));
            AssertUtils.assertNull("Public ip should be null", md.getPublicAddress());
            launcher.assertNoFloatingIp(md.getMachineId());

            String prefix = cloud.getProvider().getManagementGroup();
            OpenStackResourcePrefixes prefixes = new OpenStackResourcePrefixes(prefix, OpenstackCloudifyDriverLauncher.DEFAULT_APPLICATION_NAME,
                    service.getName());
            launcher.assertSecurityGroupIncomingRulesNumber(prefixes.getServiceName(), service.getNetwork().getAccessRules().getIncoming().size());
            launcher.assertNoRouter(prefix);

            launcher.stopMachineWithManagement(service, cloud, md.getPrivateAddress());
        } finally {
            launcher.cleanExpectedExistingNetworks();
        }
    }

    @Test
    public void testStartMachineWithComputeNetwork() throws Exception {
        Cloud cloud = launcher.createCloud("/computeNetwork/managerAppli-cloud.groovy");
        Service service = launcher.getService("secgroups/securityGroup-service.groovy");

        // Remove networkTemplate
        service.getNetwork().setTemplate(null);

        // Modify computeTemplate
        ComputeDetails compute = new ComputeDetails();
        compute.setTemplate("APPLI");
        service.setCompute(compute);

        try {
            launcher.createExpectedExistingNetworks();

            MachineDetails md = launcher.startMachineWithManagement(service, cloud);
            assertNotNull("Machine id is null", md.getMachineId());
            assertNotNull("Private ip is null", md.getPrivateAddress());
            assertTrue("Private ip is not from subnet 152.0.0.0/24. Got " + md.getPrivateAddress(), md.getPrivateAddress().startsWith("152.0.0."));
            Assert.assertNull("Public ip should be null", md.getPublicAddress());
            launcher.assertVMBoundToNetwork(md.getMachineId(), COMPUTE_SOME_INTERNAL_NETWORK_2);

        } finally {
            launcher.cleanExpectedExistingNetworks();
        }
    }
}
