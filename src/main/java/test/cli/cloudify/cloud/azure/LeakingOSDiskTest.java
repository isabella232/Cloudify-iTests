package test.cli.cloudify.cloud.azure;

import java.net.InetAddress;
import java.net.URL;

import org.cloudifysource.esc.driver.provisioning.azure.client.MicrosoftAzureRestClient;
import org.cloudifysource.esc.driver.provisioning.azure.model.Deployment;
import org.cloudifysource.esc.driver.provisioning.azure.model.Disk;
import org.cloudifysource.esc.driver.provisioning.azure.model.Disks;
import org.cloudifysource.esc.driver.provisioning.azure.model.HostedServices;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import test.cli.cloudify.cloud.NewAbstractCloudTest;
import test.cli.cloudify.cloud.services.azure.MicrosoftAzureCloudService;
import framework.utils.AssertUtils;
import framework.utils.LogUtils;

/**
 * CLOUDIFY-1432
 * @author elip
 *
 */
public class LeakingOSDiskTest extends NewAbstractCloudTest {

	private MicrosoftAzureRestClient azureClient;
	private String leakingOsDiskName;

	@BeforeClass(alwaysRun = true)
	protected void bootstrap() throws Exception {
		super.bootstrap();
		azureClient = ((MicrosoftAzureCloudService)getService()).getRestClient();
	}


	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 5, enabled = true)
	public void testLeakingOSDisk() throws Exception {		

		String managementIp = InetAddress.getByName(new URL(getService().getRestUrls()[0]).getHost()).getHostAddress();

		// manually kill the virtual machine.
		Deployment deploymentByIp = azureClient.getDeploymentByIp(managementIp, false);
		azureClient.deleteDeployment(deploymentByIp.getHostedServiceName(), deploymentByIp.getName(), System.currentTimeMillis() + OPERATION_TIMEOUT);
		azureClient.deleteCloudService(deploymentByIp.getHostedServiceName(), System.currentTimeMillis() + OPERATION_TIMEOUT);

		getService().getBootstrapper().force(true).setRestUrl(null);
		
		// this was the bug. teardown should clean everything
		super.teardown();

		// verify there is no zombi OS disk
		Disks osDisks = azureClient.listOSDisks();
		for (Disk disk : osDisks) {
			if (disk.getName().contains(getService().getMachinePrefix())) {
				// disk belongs to current bootstrap.
				String cloudServiceName = disk.getAttachedTo().getHostedServiceName();
				HostedServices cloudServices = azureClient.listHostedServices();
				if (!cloudServices.contains(cloudServiceName)) {
					// zombi os disk. its attached cloud service is gone.
					leakingOsDiskName = disk.getName();
					AssertUtils.assertFail("Found a leaking OS disk : " + disk.getName());
				}
			}

		}
	}

	@AfterClass(alwaysRun = true)
	protected void teardown() throws Exception {
		if (leakingOsDiskName != null) {
			LogUtils.log("Deleting leaking OS Disk : " + leakingOsDiskName);
			azureClient.deleteOSDisk(leakingOsDiskName, System.currentTimeMillis() + OPERATION_TIMEOUT);
		}
	}


	@Override
	protected String getCloudName() {
		return "azure";
	}

	@Override
	protected boolean isReusableCloud() {
		// TODO Auto-generated method stub
		return false;
	}



}
