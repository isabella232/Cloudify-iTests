package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.openstack.examples;

import iTests.framework.utils.LogUtils;

import java.net.URL;

import org.cloudifysource.dsl.utils.ServiceUtils;
import org.cloudifysource.esc.driver.provisioning.CloudProvisioningException;
import org.cloudifysource.esc.driver.provisioning.ComputeDriverConfiguration;
import org.cloudifysource.esc.driver.provisioning.openstack.OpenStackCloudifyDriver;
import org.cloudifysource.esc.driver.provisioning.openstack.OpenStackComputeClient;
import org.cloudifysource.esc.driver.provisioning.openstack.OpenstackException;
import org.cloudifysource.esc.driver.provisioning.openstack.rest.NovaServer;
import org.cloudifysource.esc.driver.provisioning.openstack.rest.NovaServerSecurityGroup;
import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
import org.cloudifysource.quality.iTests.test.cli.cloudify.CommandTestUtils;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.NewAbstractCloudTest;
import org.cloudifysource.restclient.GSRestClient;
import org.cloudifysource.restclient.RestException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.j_spaces.kernel.PlatformVersion;

/**
 * Check security group and machine names don't contain invalid chars.
 * 
 * @author adaml
 *
 */
public class OpenstackResourceNamesTest extends NewAbstractCloudTest {

	private static final String DEFAULT_APPLICATION_NAME = "default";
	private static final String REST_SERVICE_NAME = "rest";
	private static final String SERVICE_NAME = "simple";
	private OpenStackCloudifyDriver openstackCloudDriver;
	private static final String SERVICE_PATH = CommandTestUtils.getPath("/src/main/resources/apps/USM/usm/simple");
	private GSRestClient client;
	
	@BeforeClass(alwaysRun = true)
	protected void bootstrap() throws Exception {
		super.bootstrap();
        client = new GSRestClient("", "", new URL(getRestUrl()), PlatformVersion
                .getVersionNumber());
	}

	@Override
	protected String getCloudName() {
		return "hp-grizzly";
	}

	@Override
	protected boolean isReusableCloud() {
		return false;
	}
	
	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 4, enabled = true)
	public void testResourceNamesValidity() throws Exception {
		
		initCloudDriver();
		
		installServiceAndWait(SERVICE_PATH, SERVICE_NAME);
		
		String managementIp = getServiceIP(REST_SERVICE_NAME);
		String serviceIp = getServiceIP(ServiceUtils.getAbsolutePUName(DEFAULT_APPLICATION_NAME, SERVICE_NAME));
		
		assertResource(managementIp);
		assertResource(serviceIp);
		
		uninstallServiceAndWait(SERVICE_NAME);
	}
	
	private void assertResource(final String resourceIP) throws OpenstackException {
		
		final OpenStackComputeClient computeClient = (OpenStackComputeClient) openstackCloudDriver.getComputeContext();
		final NovaServer serverDetails = computeClient.getServerByIp(resourceIP);
		
		final NovaServerSecurityGroup[] securityGroups = serverDetails.getSecurityGroups();
		
		for (NovaServerSecurityGroup novaServerSecurityGroup : securityGroups) {
			assertResourceNameValidity(novaServerSecurityGroup.getName(), "Security Group");
		}
		
		assertResourceNameValidity(serverDetails.getName(), "Machine Name");
	}

	private String getServiceIP(String serviceName) throws RestException {
		LogUtils.log("getting private IP for service named '" + serviceName + "'");
		String privateIpUrl = "ProcessingUnits/Names/" + serviceName + "/Instances/0/JVMDetails/EnvironmentVariables/GIGASPACES_AGENT_ENV_PRIVATE_IP";
		String ipAddress = (String) client.getAdminData(privateIpUrl).get("GIGASPACES_AGENT_ENV_PRIVATE_IP");
		LogUtils.log("found service ip address: " + ipAddress);
        return ipAddress;
	}

	private void assertResourceNameValidity(
			final String resourceName, final String resourceType) {
		LogUtils.log("Asserting resource named '" + resourceName + "' does not contain invalid chars");
		assertTrue("resource of type '" + resourceType + "' contains invalid chars.", !resourceName.contains("."));
	}

	private void initCloudDriver() throws CloudProvisioningException {
		openstackCloudDriver = new OpenStackCloudifyDriver();
        ComputeDriverConfiguration conf = new ComputeDriverConfiguration();
        conf.setCloud(getService().getCloud());
        conf.setServiceName("default." + SERVICE_NAME);
        conf.setCloudTemplate("SMALL_LINUX");
        openstackCloudDriver.setConfig(conf);
	}
	
	@AfterClass(alwaysRun = true)
	protected void teardown() throws Exception {
		super.teardown();
	}
}
