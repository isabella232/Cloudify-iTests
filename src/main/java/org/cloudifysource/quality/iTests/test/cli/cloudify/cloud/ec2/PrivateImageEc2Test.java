package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.ec2;

import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.NewAbstractCloudTest;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.services.ec2.Ec2CloudService;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class PrivateImageEc2Test extends NewAbstractCloudTest {

	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 2, groups = "1", enabled = true)
	public void installTest()
			throws Exception {
		doSanityTest("petclinic", "petclinic", 15);
	}

	@Override
	protected String getCloudName() {
		return "ec2";
	}

	@Override
	protected boolean isReusableCloud() {
		return false;
	}
	
	@BeforeClass(alwaysRun = true)
	protected void bootstrap() throws Exception {
		super.bootstrap();
	}
	
	@AfterClass(alwaysRun = true)
	protected void teardown() throws Exception {
		super.teardown();
	}

	@Override
	protected void customizeCloud() {
		final Ec2CloudService ec2Service = (Ec2CloudService) cloudService;
		ec2Service.getAdditionalPropsToReplace().put("imageId \"us-east-1/ami-76f0061f\"",
				"imageId \"us-east-1/ami-93b068fa\"");
	};

}
