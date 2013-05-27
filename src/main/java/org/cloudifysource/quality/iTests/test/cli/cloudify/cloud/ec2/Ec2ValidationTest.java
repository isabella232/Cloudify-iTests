package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.ec2;

import iTests.framework.tools.SGTestHelper;
import iTests.framework.utils.IOUtils;

import java.io.File;
import java.io.IOException;

import org.cloudifysource.quality.iTests.framework.utils.CloudBootstrapper;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.NewAbstractCloudTest;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class Ec2ValidationTest  extends NewAbstractCloudTest {

	private String groovyFileName;
	CloudBootstrapper bootstrapper;

	
	@BeforeClass
	public void init() throws Exception {
		bootstrapper = new CloudBootstrapper();
		bootstrapper.scanForLeakedNodes(true);
		bootstrapper.setBootstrapExpectedToFail(true);
	}
	
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void wrongCredentialsTest() throws Exception {
		try {
			bootstrapper.scanForLeakedNodes(false);
			groovyFileName = "wrongcredentials-ec2-cloud.groovy";
			super.bootstrap(bootstrapper);
			String bootstrapOutput = bootstrapper.getLastActionOutput();
			assertTrue("The credentials are wrong but the wrong error was thrown. Reported error: " + bootstrapOutput,
					bootstrapOutput.contains("HTTP/1.1 401 Unauthorized"));
		} catch (Exception e) {
			throw e;
		} finally {
			bootstrapper.scanForLeakedNodes(true);
		}
	}

	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void wrongImageTest() throws Exception {
		groovyFileName = "wrongimage-ec2-cloud.groovy";
		super.bootstrap(bootstrapper);
		String bootstrapOutput = bootstrapper.getLastActionOutput();
		assertTrue("The imageId is invalid but the wrong error was thrown. Reported error: " + bootstrapOutput,
				bootstrapOutput.contains("Image") && bootstrapOutput.contains("is not valid for location"));
	}
	
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void wrongHardwareTest() throws Exception {
		groovyFileName = "wronghardware-ec2-cloud.groovy";
		super.bootstrap(bootstrapper);
		String bootstrapOutput = bootstrapper.getLastActionOutput();
		assertTrue("The hardwareId is invalid but the wrong error was thrown. Reported error: " + bootstrapOutput,
				bootstrapOutput.contains("hardware") && bootstrapOutput.contains("is not valid for location"));
	}
	
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void wrongSecurityGroupTest() throws Exception {
		groovyFileName = "wrongsecuritygroup-ec2-cloud.groovy";
		super.bootstrap(bootstrapper);
		String bootstrapOutput = bootstrapper.getLastActionOutput();
		assertTrue("The security group name is wrong but the wrong error was thrown. "
				+ "Reported error: " + bootstrapOutput,
				bootstrapOutput.contains("Security group") && bootstrapOutput.contains("does not exist"));
	}
	
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void wrongKeyPairTest() throws Exception {
		groovyFileName = "wrongkeypair-ec2-cloud.groovy";
		super.bootstrap(bootstrapper);
		String bootstrapOutput = bootstrapper.getLastActionOutput();
		assertTrue("The key-pair name is wrong but the wrong error was thrown. Reported error: " + bootstrapOutput,
				bootstrapOutput.contains("is invalid or in the wrong availability zone"));
	}
	
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void wrongCloudifyUrlTest() throws Exception {
		groovyFileName = "wrongcloudifyurl-ec2-cloud.groovy";
		super.bootstrap(bootstrapper);
		String bootstrapOutput = bootstrapper.getLastActionOutput();
		assertTrue("The cloudify URL is wrong but the wrong error was thrown. Reported error: " + bootstrapOutput,
				bootstrapOutput.contains("Invalid cloudify URL"));
	}
	

	@AfterClass
	public void teardown() throws Exception {
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
		File wrongGroovyFile = new File(SGTestHelper.getSGTestRootDir() + "/src/main/resources/apps/cloudify/cloud/ec2/" + groovyFileName);
		IOUtils.replaceFile(standardGroovyFile, wrongGroovyFile);
		File newFile = new File(getService().getPathToCloudFolder(), groovyFileName);
		if (newFile.exists()) {
			newFile.renameTo(standardGroovyFile);
		}
	}

}
