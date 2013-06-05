package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.ec2;

import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.DefaultBootstrapValidationTest;
import org.testng.annotations.Test;

public class Ec2ValidationTest extends DefaultBootstrapValidationTest {

	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void wrongCredentialsTest() throws Exception {
		super.wrongCredentialsTest("wrongcredentials-ec2-cloud.groovy");
	}

	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void wrongImageTest() throws Exception {
		super.wrongImageTest("wrongimage-ec2-cloud.groovy");
	}
	
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void wrongHardwareTest() throws Exception {
		super.wrongHardwareTest("wronghardware-ec2-cloud.groovy");
	}
	
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void wrongSecurityGroupTest() throws Exception {
		super.wrongSecurityGroupTest("wrongsecuritygroup-ec2-cloud.groovy");
	}
	
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void wrongKeyPairTest() throws Exception {
		super.wrongKeyPairTest("wrongkeypair-ec2-cloud.groovy");
	}
	
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void wrongCloudifyUrlTest() throws Exception {
		super.wrongCloudifyUrlTest("wrongcloudifyurl-ec2-cloud.groovy");
	}

	@Override
	protected String getCloudName() {
		return "ec2";
	}

}
