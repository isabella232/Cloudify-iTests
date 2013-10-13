package org.cloudifysource.quality.iTests.test.cli.cloudify;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
import org.openspaces.admin.pu.ProcessingUnit;
import org.testng.Assert;
import org.testng.annotations.Test;

public class ManagementServiceCloudNameTest extends AbstractLocalCloudTest {

	public static final String SERVICE_NAME = "simple";
	public static final String SERVICE_FOLDER_NAME = "simple";

	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
	public void testCloudNameInManagementService() throws IOException, InterruptedException {
		validateCloudName(CloudifyConstants.MANAGEMENT_WEBUI_SERVICE_NAME);
		validateCloudName(CloudifyConstants.MANAGEMENT_REST_SERVICE_NAME);
		validateCloudName(CloudifyConstants.MANAGEMENT_SPACE_NAME);
		
	}

	private void validateCloudName(String puName) {
		final ProcessingUnit pu =
				AbstractLocalCloudTest.admin.getProcessingUnits().waitFor(
						puName, 5, TimeUnit.SECONDS);
		Assert.assertNotNull(pu);
		final String cloudName =
				pu.getBeanLevelProperties().getContextProperties()
						.getProperty(CloudifyConstants.CONTEXT_PROPERTY_CLOUD_NAME);
		Assert.assertEquals(cloudName, CloudifyConstants.LOCAL_CLOUD_NAME);
	}

}
