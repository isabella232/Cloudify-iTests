package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.openstack;

import iTests.framework.utils.AssertUtils;

import org.cloudifysource.domain.Service;
import org.cloudifysource.domain.cloud.Cloud;
import org.cloudifysource.domain.cloud.network.NetworkConfiguration;
import org.cloudifysource.esc.driver.provisioning.MachineDetails;
import org.cloudifysource.esc.driver.provisioning.openstack.SecurityGroupNames;
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
    }

}
