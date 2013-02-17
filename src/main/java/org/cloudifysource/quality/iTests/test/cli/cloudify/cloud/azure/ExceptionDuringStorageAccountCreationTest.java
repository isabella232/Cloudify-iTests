package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.azure;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.cloudifysource.esc.driver.provisioning.azure.client.MicrosoftAzureException;
import org.cloudifysource.esc.driver.provisioning.azure.client.MicrosoftAzureRestClient;
import org.cloudifysource.esc.driver.provisioning.azure.model.AffinityGroups;
import org.cloudifysource.esc.driver.provisioning.azure.model.VirtualNetworkSites;
import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.NewAbstractCloudTest;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.services.azure.MicrosoftAzureCloudService;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.NewAbstractCloudTest;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.services.CloudServiceManager;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.services.azure.MicrosoftAzureCloudService;
import org.cloudifysource.quality.iTests.framework.utils.AssertUtils;

public class ExceptionDuringStorageAccountCreationTest extends NewAbstractCloudTest {
	
	private static final String FAILURE_MESSAGE = "The name is not a valid storage account name. Storage account names must be between 3 and 24 characters in length and use numbers and lower-case letters only.";
	
	private MicrosoftAzureRestClient azureClient ;
	
	private String leakingAffinityGroup;
	private String leakingNetwork;

	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 5, enabled = true)
	public void testExceptionInStorageAccount() throws Exception {
		MicrosoftAzureCloudService azureCloudService = (MicrosoftAzureCloudService) CloudServiceManager.getInstance().getCloudService(getCloudName());
		
		// it usually takes more than 2 minutes to create a storage account
		String className = this.getClass().getSimpleName().toLowerCase();
		
		String affinityGroup = className + "affinity";
		String network = className + "network";
		// this name is too long for a valid storage account name
		String storageAccountName = className + "storage";
		azureCloudService.setAffinityGroup(affinityGroup);
		azureCloudService.setVirtualNetworkSiteName(network);
		azureCloudService.setStorageAccountName(storageAccountName);
		azureCloudService.getBootstrapper().verbose(false).setBootstrapExpectedToFail(true);
		super.bootstrap(azureCloudService);
		
		azureClient = azureCloudService.getRestClient();
		
		String bootstrapOutput = azureCloudService.getBootstrapper().getLastActionOutput();
		AssertUtils.assertTrue("Bootstrap failed but did not contain the necessary output of " + FAILURE_MESSAGE , bootstrapOutput.toLowerCase().contains(FAILURE_MESSAGE.toLowerCase()));
		
		// check that the affinity group and network were deleted
		AffinityGroups affinityGroups = azureClient.listAffinityGroups();
		boolean affinityExists = affinityGroups.contains(affinityGroup);
		if (affinityExists) {
			leakingAffinityGroup = affinityGroup;
			AssertUtils.assertFail("Found an affinity group " + affinityGroup);
		}
		
		// listVirtualNetworks can return null if there are no networks present
		VirtualNetworkSites sites = azureClient.listVirtualNetworkSites();
		if (sites != null) {
			boolean networkExists = sites.contains(network);
			if (networkExists) {
				leakingNetwork = network;
				AssertUtils.assertFail("Found a virtual network " + network);
			}
		}
		
	}
	
	@AfterMethod(alwaysRun = true)
	public void cleanUp() throws IOException, InterruptedException, MicrosoftAzureException, TimeoutException {
		if (leakingNetwork != null) {
			azureClient.deleteVirtualNetworkSite(leakingNetwork, System.currentTimeMillis() + 60 * 1000);
		}
		if (leakingAffinityGroup != null) {
			azureClient.deleteAffinityGroup(leakingAffinityGroup, System.currentTimeMillis() + 60 * 1000);
		}
	}
	
	@Override
	protected String getCloudName() {
		return "azure";
	}

	@Override
	protected boolean isReusableCloud() {
		return false;
	}
}
