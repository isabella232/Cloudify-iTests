package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.openstack;

import iTests.framework.utils.AssertUtils;

import java.io.File;
import java.util.List;

import org.cloudifysource.domain.Service;
import org.cloudifysource.dsl.internal.ServiceReader;
import org.cloudifysource.esc.driver.provisioning.openstack.OpenStackQuantumClient;
import org.cloudifysource.esc.driver.provisioning.openstack.OpenstackException;
import org.cloudifysource.esc.driver.provisioning.openstack.OpenstackJsonSerializationException;
import org.cloudifysource.esc.driver.provisioning.openstack.rest.SecurityGroup;
import org.cloudifysource.quality.iTests.framework.utils.ServiceInstaller;
import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
import org.cloudifysource.quality.iTests.test.cli.cloudify.CommandTestUtils;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.NewAbstractCloudTest;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.services.openstack.OpenstackService;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class OpenstackTest extends NewAbstractCloudTest {

    private OpenstackService service;

    @Override
    protected String getCloudName() {
        return "openstack";
    }

    @Override
    protected boolean isReusableCloud() {
        return false;
    }

    @Override
    @BeforeClass(alwaysRun = true)
    protected void bootstrap() throws Exception {
        service = new OpenstackService();
        super.bootstrap(service, null);
    }

    @Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 4, enabled = true)
    public void testSampleSecurityGroupApplication() throws Exception {

        final String relativePath = "src/main/resources/apps/USM/usm/networks/openstack/secgroups";
        final String servicePath = CommandTestUtils.getPath(relativePath);

        final String restUrl = getRestUrl();

        final ServiceInstaller installer = new ServiceInstaller(restUrl, "securityGroups", "openstack");
        installer.recipePath(servicePath);
        installer.waitForFinish(true);
        installer.install();

        // Assert that the service is installed
        String command = "connect " + restUrl + ";list-services";
        String output = CommandTestUtils.runCommandAndWait(command);
        final Service service = ServiceReader.getServiceFromFile(new File(relativePath, "securityGroup-service.groovy"));
        AssertUtils.assertTrue("the service " + service.getName() + " is not running", output.contains(service.getName()));

        this.assertSecurityGroups();

        installer.uninstall();
    }

    private void assertSecurityGroups() throws OpenstackException {

        final OpenStackQuantumClient quantum = this.createQuantumClient();

        final List<SecurityGroup> securityGroups = quantum.getSecurityGroupsByPrefix(this.service.getMachinePrefix());
        AssertUtils.assertFalse("No security groups found", securityGroups.isEmpty());
        // Expect 6 secgroups: management, agent, cluster, application, service and another one for service's public rules.
        AssertUtils.assertEquals(6, securityGroups.size());

        final SecurityGroup management = this.retrieveSecgroup(securityGroups, "management");
        AssertUtils.assertNotNull("No management security group found", management);
        AssertUtils.assertNotNull("No rules found in management security group", management.getSecurityGroupRules());
        // > 2 because there is 2 default egress rules
        AssertUtils.assertTrue("There should be rules in management security group", management.getSecurityGroupRules().length > 2);

        final SecurityGroup agent = this.retrieveSecgroup(securityGroups, "agent");
        AssertUtils.assertNotNull("No agent security group found", agent);
        AssertUtils.assertNotNull("No rules found in agent security group", agent.getSecurityGroupRules());
        // > 2 because there is 2 default egress rules
        AssertUtils.assertTrue("There should be rules in agent security group", agent.getSecurityGroupRules().length > 2);

        final SecurityGroup cluster = this.retrieveSecgroup(securityGroups, "cluster");
        AssertUtils.assertNotNull("No cluster security group found", cluster);
        AssertUtils.assertNotNull("No rules found in cluster security group", cluster.getSecurityGroupRules());
        // There is 2 egress rules ipv4 and ipv6 open to all
        AssertUtils.assertTrue("There should be 2 rules in cluster security group (got " + cluster.getSecurityGroupRules().length + ")"
                , cluster.getSecurityGroupRules() == null || cluster.getSecurityGroupRules().length == 2);

        final SecurityGroup appli = this.retrieveSecgroup(securityGroups, "default");
        AssertUtils.assertNotNull("No application security group found", appli);
        AssertUtils.assertNotNull("No rules found in application security group", appli.getSecurityGroupRules());
        // There is 2 egress rules ipv4 and ipv6 open to all
        AssertUtils.assertEquals("There should 2 rules in application security group (got " + appli.getSecurityGroupRules().length + ")"
                , 2, appli.getSecurityGroupRules().length);

        final SecurityGroup serviceSecgroup = this.retrieveSecgroup(securityGroups, "securityGroups");
        AssertUtils.assertNotNull("No service security group found", serviceSecgroup);
        AssertUtils.assertNotNull("No rules found in service security group", serviceSecgroup.getSecurityGroupRules());
        AssertUtils.assertEquals("There should 7 rules in application security group (got " + serviceSecgroup.getSecurityGroupRules().length + ")"
                , 7, serviceSecgroup.getSecurityGroupRules().length);

        final SecurityGroup servicePublic = this.retrieveSecgroup(securityGroups, "securityGroups-public");
        AssertUtils.assertNotNull("No service security group found", servicePublic);
        AssertUtils.assertNotNull("No rules found in service security group", servicePublic.getSecurityGroupRules());
        AssertUtils.assertEquals("There should 3 rules in application security group (got " + servicePublic.getSecurityGroupRules().length + ")"
                , 3, servicePublic.getSecurityGroupRules().length);
    }

    private SecurityGroup retrieveSecgroup(List<SecurityGroup> securityGroups, String suffix) {
        for (SecurityGroup securityGroup : securityGroups) {
            if (securityGroup.getName().endsWith(suffix)) {
                return securityGroup;
            }
        }
        return null;
    }

    private OpenStackQuantumClient createQuantumClient() throws OpenstackJsonSerializationException {
        final String imageId = this.getCloudProperty(OpenstackService.IMAGE_PROP);
        final String region = imageId.split("/")[0];
        final OpenStackQuantumClient client = new OpenStackQuantumClient(
                this.getCloudProperty(OpenstackService.ENDPOINT_PROP),
                this.getCloudProperty(OpenstackService.USER_PROP),
                this.getCloudProperty(OpenstackService.API_KEY_PROP),
                this.getCloudProperty(OpenstackService.TENANT_PROP),
                region,
                "v2.0");
        return client;
    }

    public String getCloudProperty(String key) {
        return service.getCloudProperty(key);
    }

    @Override
    @AfterClass(alwaysRun = true)
    protected void teardown() throws Exception {
        super.teardown();
    }
}
