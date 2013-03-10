package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.azure;

import java.net.InetAddress;
import java.net.URL;
import java.util.concurrent.TimeoutException;

import org.cloudifysource.esc.driver.provisioning.azure.client.MicrosoftAzureException;
import org.cloudifysource.esc.driver.provisioning.azure.client.MicrosoftAzureRestClient;
import org.cloudifysource.esc.driver.provisioning.azure.model.Deployment;
import org.cloudifysource.esc.driver.provisioning.azure.model.Disk;
import org.cloudifysource.esc.driver.provisioning.azure.model.Disks;
import org.cloudifysource.quality.iTests.framework.utils.AssertUtils;
import org.cloudifysource.quality.iTests.framework.utils.AssertUtils.RepetitiveConditionProvider;
import org.cloudifysource.quality.iTests.framework.utils.LogUtils;
import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.NewAbstractCloudTest;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.services.azure.MicrosoftAzureCloudService;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

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


	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 5, enabled = true)
	public void testLeakingOSDisk() throws Exception {		

		String managementIp = InetAddress.getByName(new URL(getService().getRestUrls()[0]).getHost()).getHostAddress();
		LogUtils.log("Management machine ip is " + managementIp);

		// manually kill the virtual machine.
		Deployment deploymentByIp = azureClient.getDeploymentByIp(managementIp, false);
		
		LogUtils.log("Management machine deployment is " + deploymentByIp.getHostedServiceName());
		
		LogUtils.log("Manually shutting the management machine down...");
		azureClient.deleteDeployment(deploymentByIp.getHostedServiceName(), deploymentByIp.getName(), System.currentTimeMillis() + AbstractTestSupport.OPERATION_TIMEOUT);
		LogUtils.log("Manually deleting the cloud service...");
		azureClient.deleteCloudService(deploymentByIp.getHostedServiceName(), System.currentTimeMillis() + AbstractTestSupport.OPERATION_TIMEOUT);

		getService().getBootstrapper().force(true).setRestUrl(null);
		
		// wait for the remaining disk to detach
		
		LogUtils.log("Waiting for the disk to detach...");
		RepetitiveConditionProvider condition = new AssertUtils.RepetitiveConditionProvider() {
			
			@Override
			public boolean getCondition() {
				try {
					Disk disk = getRemainingDisk();
					return (disk.getAttachedTo() == null);
				} catch (final Exception e) {
					LogUtils.log("Caught an exception while retrieving disk", e);
					return false;
				} 
			}
		};
		AssertUtils.repetitiveAssertTrue("Timed out waiting for disk to detach", condition, AbstractTestSupport.OPERATION_TIMEOUT);
		
		// this was the bug. teardown should delete the zombie disk
		super.teardown();

		Disk remainingDIsk = getRemainingDisk();
		if (remainingDIsk != null) {
			// leaking disk, teardown did not delete it.
			leakingOsDiskName = remainingDIsk.getName();
			AssertUtils.assertFail("Found a leaking os disk with name " + remainingDIsk.getName() + " after teardown"); 
		}

	}

	private Disk getRemainingDisk() throws MicrosoftAzureException, TimeoutException {
		Disks osDisks = azureClient.listOSDisks();
		for (Disk disk : osDisks) {
			if (disk.getName().contains(getService().getMachinePrefix())) {
				return disk;
			}
		}
		return null;
	}


	@AfterClass(alwaysRun = true)
	protected void teardown() throws Exception {
		if (leakingOsDiskName != null) {
			LogUtils.log("Deleting leaking OS Disk : " + leakingOsDiskName);
			azureClient.deleteOSDisk(leakingOsDiskName, System.currentTimeMillis() + AbstractTestSupport.OPERATION_TIMEOUT);
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
