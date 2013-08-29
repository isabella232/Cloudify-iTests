package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.ec2;

import iTests.framework.tools.SGTestHelper;
import iTests.framework.utils.IOUtils;
import iTests.framework.utils.LogUtils;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.Assert;

import org.apache.commons.io.FileUtils;
import org.cloudifysource.quality.iTests.framework.utils.JCloudsUtils;
import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
import org.cloudifysource.quality.iTests.test.cli.cloudify.CommandTestUtils;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.NewAbstractCloudTest;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.services.ec2.Ec2CloudService;
import org.jclouds.compute.domain.ComputeMetadata;
import org.jclouds.compute.domain.NodeMetadata;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * 
 * @author yael
 *
 */
public class Ec2AddTemplatesTest extends NewAbstractCloudTest {

	private static final String SERVICE_FOLDER_PATH = 
			CommandTestUtils.getPath("src/main/resources/apps/USM/usm/services/simpleForAddTemplateOnEC2");
	private static final String CREDENTIALS_FOLDER = System.getProperty("iTests.credentialsFolder",
            SGTestHelper.getSGTestRootDir() + "/src/main/resources/credentials");
	private final String PEM_FILE_NAME = "ec2-sgtest-eu.pem";
	private final String PEM_FILE_PATH = CREDENTIALS_FOLDER + "/cloud/ec2/" + PEM_FILE_NAME;
	private static final String SERVICE_NAME = "simpleForEC2";
	private static final String TEMPLATE_FOLDER_PATH =
			CommandTestUtils.getPath("src/main/resources/templates/ubuntu");
	private static final String UPLOAD_FOLDER_PATH = TEMPLATE_FOLDER_PATH + "/ubuntu_upload";
	private static final String TEMPLATE_NAME = "UBUNTU_TEST";
	private static final String TEMPLATE_PROPERTIES_FILE_PATH = TEMPLATE_FOLDER_PATH + "/ubuntu-template.properties";
	private static final String UBUNTU_IMAGE_ID_US = "us-east-1/ami-82fa58eb";
	private static final String UBUNTU_IMAGE_ID_EU = "eu-west-1/ami-ce7b6fba";
    private static final int SERVICE_INSTALLATION_TIMEOUT_IN_MINUTES = 15;

    @Override
	@BeforeClass(alwaysRun = true)
	protected void bootstrap() throws Exception {
		super.bootstrap();
	}

	@Override
	@AfterClass(alwaysRun = true)
	protected void teardown() throws Exception {
		super.teardown();
		File pemFile = new File(UPLOAD_FOLDER_PATH, PEM_FILE_NAME);
		pemFile.delete();
	}

	@Override
	protected String getCloudName() {
		return "ec2";
	}

	@Override
	protected boolean isReusableCloud() {
		return false;
	}

	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 4, enabled = true)
	public void testAddTemplateAndInstallService() 
			throws IOException, InterruptedException {	

		// update properties if working in us region
		if (((Ec2CloudService)getService()).getRegion().contains("us")) {
			updatePropertiesFile();			
		}
		copyPemFile();

		// add templates
		String command = "connect " + getRestUrl() + ";add-templates " + TEMPLATE_FOLDER_PATH;
		String output = CommandTestUtils.runCommandAndWait(command);
		Assert.assertTrue(output.contains("Templates added successfully"));
		Assert.assertTrue(output.contains(TEMPLATE_NAME));

		//install service
		installServiceAndWait(SERVICE_FOLDER_PATH, SERVICE_NAME, SERVICE_INSTALLATION_TIMEOUT_IN_MINUTES);
		if (((Ec2CloudService)getService()).getRegion().contains("us")) {
			assertImageID(UBUNTU_IMAGE_ID_US);
		} else {
			assertImageID(UBUNTU_IMAGE_ID_EU);
		}
		
		
		uninstallServiceIfFound(SERVICE_NAME);	
		// remove templates
		command = "connect " + getRestUrl() + ";remove-template " + TEMPLATE_NAME;
		output = CommandTestUtils.runCommandAndWait(command);
		Assert.assertTrue(output.contains("Template " + TEMPLATE_NAME + " removed successfully"));
		assertTemplateRemoved(TEMPLATE_NAME);
	}

	private void assertImageID(String expectedImageID) {
		JCloudsUtils.createContext(getService());
		Set<? extends ComputeMetadata> allNodes = JCloudsUtils.getAllNodes();
		List<String> foundImages = new LinkedList<String>();
		for (ComputeMetadata computeMetadata : allNodes) {
			NodeMetadata nodeMetadata = (NodeMetadata)computeMetadata;
			String imageId = nodeMetadata.getImageId();
			foundImages.add(imageId);
			if (expectedImageID.equals(imageId)) {
				return;
			}
		}
		Assert.fail("Expecting to find image id " + expectedImageID + ", but found " + foundImages);
		
	}

	private void updatePropertiesFile() throws IOException {

		Ec2CloudService service = (Ec2CloudService)getService();

        Map<String, Object> props = service.getProperties();

		File propsFile = new File(TEMPLATE_PROPERTIES_FILE_PATH);

        if (service.getRegion().contains("eu")) {
            props.put("ubuntuImageId", UBUNTU_IMAGE_ID_EU);
        } else {
            props.put("ubuntuImageId", UBUNTU_IMAGE_ID_US);
        }
		IOUtils.writePropertiesToFile(props, propsFile);
	}


	private void assertTemplateRemoved(String templateName) {
		String command = "connect " + getRestUrl() + ";list-templates";
		try {
			String output = CommandTestUtils.runCommandAndWait(command);
			Assert.assertFalse(output.contains(templateName));

		} catch (final Exception e) {
			LogUtils.log(e.getMessage(), e);
			Assert.fail(e.getMessage());
		}
	}
	
	private void copyPemFile() throws IOException {
		File pemFile = new File(PEM_FILE_PATH);
		File uploadFolder = new File(UPLOAD_FOLDER_PATH);
		FileUtils.copyFileToDirectory(pemFile, uploadFolder);
	}

}
