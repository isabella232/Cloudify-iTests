package test.cli.cloudify.cloud;

import java.io.IOException;

import org.cloudifysource.dsl.internal.CloudifyConstants;

import org.testng.annotations.AfterMethod;

import org.testng.annotations.Test;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.DefaultClientConfig;

import test.cli.cloudify.CommandTestUtils;
import framework.utils.LogUtils;
import framework.utils.ScriptUtils;

public class ExamplesTest extends AbstractCloudTest {

	private static final String CLOUD_SERVICE_UNIQUE_NAME = "ExamplesTest";
	private String applicationName;

	public ExamplesTest() {
		LogUtils.log("Instansiated " + ExamplesTest.class.getName());
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 4, enabled = true, dataProvider = "supportedClouds")
	public void testTravel(String cloudName)
			throws IOException, InterruptedException {
		doTest(cloudName, "travel", "travel");
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 4, enabled = true, dataProvider = "supportedCloudsWithoutByon")
	public void testPetclinic(String cloudName)
			throws IOException, InterruptedException {
		doTest(cloudName, "petclinic", "petclinic");
	}

	// petclinic-simple is covered by {@link ScalingRulesCloudTest}

	protected void doTest(String cloudName, String applicationFolderName, String applicationName)
			throws IOException, InterruptedException {
		LogUtils.log("installing application " + applicationName + " on " + cloudName);
		setCloudService(cloudName, CLOUD_SERVICE_UNIQUE_NAME, true);
		this.applicationName = applicationName;
		String applicationPath = ScriptUtils.getBuildPath() + "/recipes/apps/" + applicationFolderName;
		try {
			installApplicationAndWait(applicationPath, applicationName);
			if (applicationName.equals("travel") && cloudName.equals("ec2")) {
				// verify image and hardware ID only for travel on EC2
				Client client = Client.create(new DefaultClientConfig());
				final WebResource service = client.resource(this.getRestUrl());
				String hardwareIDResult =
						service.path(
								"/admin/ProcessingUnits/Names/travel.tomcat/Instances/0/Statistics/Monitors/USM/Details/Attributes/"
										+ CloudifyConstants.USM_DETAILS_HARDWARE_ID).get(String.class);
				assertTrue("error reading hardware id", !hardwareIDResult.contains("error"));
				assertTrue("Could not find expected hardware ID: m1.small in response",
						hardwareIDResult.contains("m1.small"));

				String imageIDResult =
						service.path(
								"/admin/ProcessingUnits/Names/travel.tomcat/Instances/0/Statistics/Monitors/USM/Details/Attributes/" + CloudifyConstants.USM_DETAILS_IMAGE_ID)
								.get(String.class);
				assertTrue("error reading image id", !imageIDResult.toLowerCase().contains("error"));

			}
			// http://localhost:8100

		} finally {
			if ((getService() != null) && (getService().getRestUrls() != null)) {
				String command = "connect " + getRestUrl() + ";list-applications";
				String output = CommandTestUtils.runCommandAndWait(command);
				if (output.contains(applicationName)) {
					uninstallApplicationAndWait(applicationName);
				}
			}
		}
	}

	@AfterMethod
	public void afterTest() {
		if ((getService() != null) && (getService().getRestUrls() != null)) {
			String command = "connect " + getRestUrl() + ";list-applications";
			String output;
			try {
				output = CommandTestUtils.runCommandAndWait(command);
				if (output.contains(applicationName)) {
					uninstallApplicationAndWait(this.applicationName);
				}
			} catch (IOException e) {
				e.printStackTrace();
				AssertFail(e.getMessage());
			} catch (InterruptedException e) {
				e.printStackTrace();
				AssertFail(e.getMessage());
			}
		}
	}
}
