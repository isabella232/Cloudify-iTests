package test.cli.cloudify.cloud.ec2;

import java.io.IOException;
import java.util.HashMap;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import framework.utils.LogUtils;

import test.cli.cloudify.CloudTestUtils;
import test.cli.cloudify.cloud.AbstractCloudTest;
import test.cli.cloudify.cloud.services.ec2.Ec2CloudService;

public class EuWestRegionEc2Test extends AbstractCloudTest {
	
	private Ec2CloudService service;
	private static final String CLOUD_SERVICE_UNIQUE_NAME = "EuWestRegionEc2Test";

	@BeforeMethod
	public void bootstrap() throws IOException, InterruptedException {	
		setCloudService("EC2", CLOUD_SERVICE_UNIQUE_NAME, false);
		service = (Ec2CloudService)getService();
		service.setPemFileName("sgtest-eu");
		service.setAdditionalPropsToReplace(new HashMap<String, String>());
		service.getAdditionalPropsToReplace().put("imageId \"us-east-1/ami-76f0061f\"", "imageId \"eu-west-1/ami-24506250\"");
		service.getAdditionalPropsToReplace().put("locationId \"us-east-1\"", "locationId \"eu-west-1\"");
		service.setMachinePrefix(this.getClass().getName() + CloudTestUtils.SGTEST_MACHINE_PREFIX);
		service.bootstrapCloud();
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, enabled = true)
	public void testEuWestRegion() throws IOException, InterruptedException {
		doSanityTest(EC2, "travel", "travel");
	}
	
	@AfterMethod(alwaysRun = true)
	public void teardown() throws IOException {
		try {
			service.teardownCloud();
		}
		catch (Throwable e) {
			LogUtils.log("caught an exception while tearing down ec2", e);
			sendTeardownCloudFailedMail("ec2", e);
		}
	}

}
