package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.byon;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import junit.framework.Assert;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.packaging.Packager;
import org.cloudifysource.dsl.internal.packaging.ZipUtils;
import org.cloudifysource.esc.driver.provisioning.byon.ByonProvisioningDriver;
import org.cloudifysource.esc.driver.provisioning.jclouds.DefaultProvisioningDriver;
import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
import org.cloudifysource.quality.iTests.test.cli.cloudify.CloudTestUtils;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.services.byon.ByonCloudService;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.services.ec2.Ec2CloudService;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.openspaces.admin.pu.events.ProcessingUnitInstanceRemovedEventListener;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import iTests.framework.utils.AssertUtils;
import iTests.framework.utils.CommandTestUtils;
import iTests.framework.utils.GsmTestUtils;
import iTests.framework.utils.LogUtils;
import iTests.framework.utils.SSHUtils;
import iTests.framework.utils.ThreadBarrier;

/**
 * 
 * @author barakme
 * 
 */
public class ProvisioningContextByonTest extends AbstractByonCloudTest {

	@BeforeClass(alwaysRun = true)
	protected void bootstrap() throws Exception {
		super.bootstrap();
	}

	@Override
	public void beforeBootstrap() throws IOException {

		String newCloudDriverClazz = "CustomCloudDriver";

		CloudTestUtils.replaceGroovyDriverImplementation(
				getService(),
				ByonProvisioningDriver.class.getName(), // old class
				newCloudDriverClazz, // new class
				new File("src/main/resources/custom-cloud-configs/byon/provisioning-context")); // version
	}

	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 2, enabled = true)
	public void installTomcatTest() throws IOException, InterruptedException {
		try {
			installServiceAndWait("tomcat", "tomcat");
		} finally {
			uninstallServiceAndWait("tomcat");
		}

	}

	@AfterClass(alwaysRun = true)
	protected void teardown() throws Exception {
		super.teardown();
	}

}
