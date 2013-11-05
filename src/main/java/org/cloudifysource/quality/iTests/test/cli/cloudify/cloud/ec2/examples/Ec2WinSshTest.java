package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.ec2.examples;

import java.io.File;

import org.cloudifysource.quality.iTests.test.cli.cloudify.CommandTestUtils;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.ec2win.Ec2WinExamplesTest;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.services.CloudServiceManager;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.services.ec2.Ec2CloudService;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class Ec2WinSshTest extends Ec2WinExamplesTest{

	@Override
	protected String getCloudName() {
		return "ec2-win";
	}

	@Override
	@BeforeClass(alwaysRun = true)
	protected void bootstrap() throws Exception {

		try {
			prepareApplication("petclinic-win");
		} catch (final Exception e) {
			Assert.fail("Failed preparing windows applications for deployment. Reason : " + e.getMessage());
		}


		Ec2CloudService service = (Ec2CloudService) CloudServiceManager.getInstance().getCloudService(getCloudName());

		final String cloudPath = CommandTestUtils.getPath("src/main/resources/apps/cloudify/cloud/ec2-win-ssh/ec2-win-cloud.groovy");
		final File cloudFile = new File(cloudPath);
		Assert.assertTrue(cloudFile.exists() && cloudFile.isFile());
		service.setCloudGroovy(cloudFile);
		final String password = service.getCertProperty("windowsImagePassword");
		Assert.assertNotNull(password, "The windows image password is not available in the EC2 credentials property file");
		service.getAdditionalPropsToReplace().put("ENTER_PASSWORD", password);



		super.bootstrap(service);
	}


    @Override
    @Test(enabled = false)
    public void testTravel() throws Exception {
    }

	@Test(enabled = false)
    @Override
    public void testHelloWorld() throws Exception {
    }


	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, enabled = true)
    @Override
    public void testPetclinic() throws Exception {
		super.testPetclinic();
    }

	@Test(enabled = false)
    @Override
    public void testPetclinicSimple() throws Exception {
    	// TODO Auto-generated method stub
    }
	
	@Test(enabled = false)
    @Override
	public void testLinuxAgent() throws Exception {
		
	}

	@Override
	@AfterClass(alwaysRun = true)
	protected void teardown() throws Exception {
		super.teardown();
	}

}
