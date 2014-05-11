package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.ec2;

import iTests.framework.utils.LogUtils;
import iTests.framework.utils.ScriptUtils;

import java.io.File;
import java.util.Map;

import org.cloudifysource.domain.Service;
import org.cloudifysource.domain.cloud.Cloud;
import org.cloudifysource.domain.cloud.compute.ComputeTemplate;
import org.cloudifysource.dsl.internal.ServiceReader;
import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.NewAbstractCloudTest;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.services.ec2.Ec2CloudService;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class Ec2MixedOSesTest extends NewAbstractCloudTest {

	final String serviceName = "tomcat";
	final String tomcatServicePath = ScriptUtils.getBuildPath() + "/recipes/services/" + serviceName;

	@Override
	protected String getCloudName() {
		return "ec2";
	}
	
	@BeforeClass(alwaysRun = true)
	protected void bootstrap() throws Exception {
		super.bootstrap();
	}
	
	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 4, enabled = true)
	public void testInstallationWithMixedOSes() throws Exception {
		
		Cloud cloud = this.cloudService.getCloud();
		String managementTemplateName = cloud.getConfiguration().getManagementMachineTemplate();
		Map<String, ComputeTemplate> templates = cloud.getCloudCompute().getTemplates();
		ComputeTemplate managementTemplate = templates.get(managementTemplateName);
		LogUtils.log("Management template name: " + managementTemplateName);
		final String managementRemoteDir = managementTemplate.getRemoteDirectory();
		LogUtils.log("Management remote directory: " + managementRemoteDir);
		
		Service tomcatService = ServiceReader.readService(new File(tomcatServicePath));
		String tomcatTemplateName = tomcatService.getCompute().getTemplate();
		ComputeTemplate tomcatTemplate = templates.get(tomcatTemplateName);
		LogUtils.log("Tomcat template name: " + tomcatTemplateName);
		final String tomcatRemoteDir = tomcatTemplate.getRemoteDirectory();
		LogUtils.log("Tomcat remote directory: " + tomcatRemoteDir);
		
		AbstractTestSupport.assertTrue("The management template and the Tomcat template are the same!", 
				!managementTemplateName.equalsIgnoreCase(tomcatTemplateName));
		
		AbstractTestSupport.assertTrue("The management remote directory and the Tomcat remote directory are "
				+ "the same!", !managementRemoteDir.equalsIgnoreCase(tomcatRemoteDir));
		
		
		LogUtils.log("Testing service installation");
		installServiceAndWait(tomcatServicePath, serviceName);
		uninstallServiceAndWait(serviceName);
		
		super.scanForLeakedAgentNodes();
	}

	@Override
	protected void customizeCloud() throws Exception {
		
		//Set the management machine template option to be taken from the cloud props file.
		((Ec2CloudService)cloudService).getAdditionalPropsToReplace().put("managementMachineTemplate \"SMALL_LINUX\"",
				"managementMachineTemplate \"SMALL_UBUNTU\"");
	}
	
	@AfterClass(alwaysRun = true)
	protected void teardown() throws Exception {
		super.teardown();
	}
	
	@Override
	protected boolean isReusableCloud() {
		return false;
	}

}
