package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.openstack;

import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.DefaultBootstrapValidationTest;
import org.testng.annotations.Test;

public class OpenstackValidationTest extends DefaultBootstrapValidationTest {


	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void wrongImageTest() throws Exception {
		super.wrongImageTest("wrongimage-openstack-cloud.groovy");
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void wrongHardwareTest() throws Exception {
		super.wrongHardwareTest("wronghardware-openstack-cloud.groovy");
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void wrongSecurityGroupTest() throws Exception {
		super.wrongSecurityGroupTest("wrongsecuritygroup-openstack-cloud.groovy");
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void wrongKeyPairTest() throws Exception {
		super.wrongKeyPairTest("wrongkeypair-openstack-cloud.groovy");
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void wrongCloudifyUrlTest() throws Exception {
		super.wrongCloudifyUrlTest("wrongcloudifyurl-openstack-cloud.groovy");
	}

	@Override
	protected String getCloudName() {
		return "hp-folsom";
	}

}
