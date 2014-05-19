package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.openstack;

import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.DefaultBootstrapValidationTest;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

public class OpenstackValidationTest extends DefaultBootstrapValidationTest {


	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = false)
	public void wrongImageTest() throws Exception {
		super.wrongImageTest("wrongimage-openstack-cloud.groovy");
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = false)
	public void wrongHardwareTest() throws Exception {
		super.wrongHardwareTest("wronghardware-openstack-cloud.groovy");
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = false)
	public void wrongSecurityGroupTest() throws Exception {
		super.wrongSecurityGroupTest("wrongsecuritygroup-openstack-cloud.groovy");
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = false)
	public void wrongKeyPairTest() throws Exception {
		super.wrongKeyPairTest("wrongkeypair-openstack-cloud.groovy");
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = false)
	public void wrongCloudifyUrlTest() throws Exception {
		super.wrongCloudifyUrlTest("wrongcloudifyurl-openstack-cloud.groovy");
	}

	@Override
	protected String getCloudName() {
		return "hp-folsom";
	}
	
	@AfterClass
	public void teardown() throws Exception {
		super.teardown();
	}

}
