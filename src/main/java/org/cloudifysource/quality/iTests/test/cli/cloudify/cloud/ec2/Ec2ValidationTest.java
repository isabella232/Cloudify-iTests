package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.ec2;

import java.io.File;
import java.io.IOException;

import org.cloudifysource.quality.iTests.framework.tools.SGTestHelper;
import org.cloudifysource.quality.iTests.framework.utils.IOUtils;
import org.cloudifysource.quality.iTests.framework.utils.JCloudsUtils;
import org.cloudifysource.quality.iTests.framework.utils.LogUtils;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.NewAbstractCloudTest;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.services.ec2.Ec2CloudService;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

public class Ec2ValidationTest  extends NewAbstractCloudTest {

	private Ec2CloudService service;
	private String groovyFileName;

	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void wrongProviderTest() throws IOException, InterruptedException {
		try {
			groovyFileName = "wrongprovider-ec2-cloud.groovy";
			super.bootstrap();
		} catch (Throwable ae) {
			LogUtils.log(ae.getMessage());
		}
	}
	
	/*@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void missingEndpointTest() throws IOException, InterruptedException {
		try {
			groovyFileName = "missingendpoint-ec2-cloud.groovy";
			super.bootstrap();
		} catch (Throwable ae) {
			LogUtils.log(ae.getMessage());
		}
	}*/
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void wrongCredentialsTest() throws IOException, InterruptedException {
		try {
			groovyFileName = "wrongcredentials-ec2-cloud.groovy";
			super.bootstrap();
		} catch (Throwable ae) {
			LogUtils.log(ae.getMessage());
		}
	}

	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void wrongImageTest() throws IOException, InterruptedException {
		try {
			groovyFileName = "wrongimage-ec2-cloud.groovy";
			super.bootstrap();
			assertTrue("The imageId is invalid yet no error was thrown", false);
		} catch (Throwable ae) {
			assertTrue("The imageId is invalid but the wrong error was thrown. Error was: " + ae.getMessage(),
					ae.getMessage().contains("Invalid template configuration: could not get imageId"));
		}
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void wrongHardwareTest() throws IOException, InterruptedException {
		try {
			groovyFileName = "wronghardware-ec2-cloud.groovy";
			super.bootstrap();
		} catch (Throwable ae) {
			LogUtils.log(ae.getMessage());
		}
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void wrongSecurityGroupTest() throws IOException, InterruptedException {
		try {
			groovyFileName = "wrongsecuritygroup-ec2-cloud.groovy";
			super.bootstrap();
			assertTrue("The security group name is invalid yet no error was thrown", false);
		} catch (Throwable ae) {
			assertTrue("The security group name is invalid but the wrong error was thrown. Error was: " 
					+ ae.getMessage(), 
					ae.getMessage().contains("Invalid security groups configuration: Invalid security group name:"));
		}
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void wrongKeyPairTest() throws IOException, InterruptedException {
		try {
			groovyFileName = "wrongkeypair-ec2-cloud.groovy";
			super.bootstrap();
			assertTrue("The key-pair name is invalid yet no error was thrown", false);
		} catch (Throwable ae) {
			assertTrue("The key-pair name is invalid but the wrong error was thrown. Error was: " + ae.getMessage(),
					ae.getMessage().contains("Invalid key-pair configuration: Invalid key-pair name:"));
		}
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void wrongCloudifyUrlTest() throws IOException, InterruptedException {
		try {
			groovyFileName = "wrongcloudifyurl-ec2-cloud.groovy";
			super.bootstrap();
		} catch (Throwable ae) {
			LogUtils.log(ae.getMessage());
		}
	}

	@AfterClass
	public void teardown() throws Exception {
		JCloudsUtils.closeContext();		
		super.teardown();			
	}

	@Override
	protected String getCloudName() {
		return "ec2";
	}

	@Override
	protected boolean isReusableCloud() {
		return false;
	}

	protected void customizeCloud() throws IOException {
		//replace the ec2-cloud.groovy with a wrong version, to fail the validation.
		File standardGroovyFile = new File(getService().getPathToCloudFolder(), "ec2-cloud.groovy");
		File wrongproviderGroovyFile = new File(SGTestHelper.getSGTestRootDir() + "/src/main/resources/apps/cloudify/cloud/ec2/" + groovyFileName);
		IOUtils.replaceFile(standardGroovyFile, wrongproviderGroovyFile);
		File newFile = new File(getService().getPathToCloudFolder(), groovyFileName);
		if (newFile.exists()) {
			newFile.renameTo(standardGroovyFile);
		}
	}
	
	/*@Override
	protected void customizeCloud() throws IOException {
		service = (Ec2CloudService) getService();
		File groovyFile = new File(SGTestHelper.getSGTestRootDir() 
				+ "/src/main/resources/apps/cloudify/cloud/ec2/" + groovyFileName);
		service.setCloudGroovy(groovyFile);
	} */
}
