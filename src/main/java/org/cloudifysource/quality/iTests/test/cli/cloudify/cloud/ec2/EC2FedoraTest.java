/*******************************************************************************
 * Copyright (c) 2013 GigaSpaces Technologies Ltd. All rights reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.ec2;

import iTests.framework.tools.SGTestHelper;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import junit.framework.Assert;

import org.apache.commons.io.FileUtils;
import org.cloudifysource.dsl.rest.AddTemplatesException;
import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
import org.cloudifysource.quality.iTests.test.cli.cloudify.CommandTestUtils;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.NewAbstractCloudTest;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.services.ec2.EC2TemplatesFolderHandler;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.templates.EC2TemplateDetails;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.templates.EC2TemplatesHandler;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.templates.TemplateDetails;
import org.cloudifysource.restclient.exceptions.RestClientException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Tests that bootstrap is successful with fedora.
 * 
 * @author yael
 *
 */
public class EC2FedoraTest extends NewAbstractCloudTest {
	
	private static final String CREDENTIALS_FOLDER = System.getProperty("iTests.credentialsFolder",
            SGTestHelper.getSGTestRootDir() + "/src/main/resources/credentials");
	private final String PEM_FILE_NAME = "ec2-sgtest-eu.pem";
	private final String PEM_FILE_PATH = CREDENTIALS_FOLDER + "/cloud/ec2/" + PEM_FILE_NAME;
	
	private static final String POST_BOOTSTRAP_PATH = CommandTestUtils.getPath("src/main/resources/templates/post-bootstrap.sh");

	private final String TEMPLATES_ROOT_PATH = CommandTestUtils.getPath("src/main/resources/templates");
	private final String TEMP_TEMPLATES_DIR_PATH = TEMPLATES_ROOT_PATH + File.separator + "templates.tmp";
	public static final String IMAGE_ID = "eu-west-1/ami-f1031e85";
	private static final String TEMPLATE_NAME = "SMALL_FEDORA";
	private static final String LOCATION_ID = "eu-west-1";
	private static final String HARDWARE_ID = "m1.small";
	private static final String KEY_PER = "ec2-sgtest-eu";
	private static final int MACHINE_MEMORY_MB = 1600;
	private static final String USERNAME = "fedora";
	private static final String REMOTE_DIR = "/home/fedora/gs-files";

	private static final String SERVICE_DIR_PATH = CommandTestUtils.getPath("src/main/resources/apps/USM/usm/tomcatOnFedora");
	private static final String SERVICE_NAME = "tomcat";
	private static final int INSTALLATION_TIMEOUT_IN_MINUTES = 15;
	private static final String GET_TEMPLATE_NAME_CUSTOM_COMMAND_NAME = "GetTemplateName";
	private static final String GET_IMAGE_ID_CUSTOM_COMMAND_NAME = "GetImageID";
	
	private EC2TemplatesHandler handler;
	private File templatesTempFolder;
	
    @Override
	@BeforeClass(alwaysRun = true)
	protected void bootstrap() throws Exception {
    	super.bootstrap();
		List<String> defaultTemplates = new LinkedList<String>();
		defaultTemplates.add("SMALL_LINUX");
		defaultTemplates.add("SMALL_UBUNTU");
		defaultTemplates.add("MEDIUM_UBUNTU");
		
		templatesTempFolder = new File(TEMP_TEMPLATES_DIR_PATH);
		templatesTempFolder.mkdirs();
		handler = new EC2TemplatesHandler(defaultTemplates , getRestUrl(), templatesTempFolder);
	}

	@Override
	@AfterClass(alwaysRun = true)
	protected void teardown() throws Exception {
		FileUtils.deleteDirectory(templatesTempFolder);
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

	
	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 4, enabled = true)
	public void testAddFedoraTemplateAndInstallService() throws IOException, InterruptedException {
		addTemplate();
		try {
			installServiceAndWait(SERVICE_DIR_PATH, SERVICE_NAME, INSTALLATION_TIMEOUT_IN_MINUTES);
			assertFedoraTemplate();
		} finally {
			uninstallServiceIfFound(SERVICE_NAME);
		}
		
	}
	
	private void addTemplate() throws IOException {
		EC2TemplatesFolderHandler folderHandler = handler.createNewTemplatesFolderHandler();
		EC2TemplateDetails template = new EC2TemplateDetails();	
		template.setTemplateName(TEMPLATE_NAME);
		template.setForServiceInstallation(true);
		template.setImageId(IMAGE_ID);
		template.setLocationId(LOCATION_ID);
		template.setHardwareId(HARDWARE_ID);
		template.setKeyFile(PEM_FILE_NAME);
		template.setKeyPair(KEY_PER);
		template.setMachineMemoryMB(MACHINE_MEMORY_MB);
		template.setUsername(USERNAME);
		template.setRemoteDirectory(REMOTE_DIR);
		
		// add template to folder handler
		TemplateDetails addedTemplate = folderHandler.addCustomTemplate(template);
		try {
			// copy the pem file to the upload directory
			File uploadFolder = new File(addedTemplate.getTemplateFolder(), addedTemplate.getUploadDirName());
			FileUtils.copyFileToDirectory(new File(PEM_FILE_PATH), uploadFolder);
			FileUtils.copyFileToDirectory(new File(POST_BOOTSTRAP_PATH), uploadFolder);

			// add the template to the cloud
			handler.addTemplatesToCloudUsingRestAPI(folderHandler);
			handler.assertExpectedList();
		} catch (RestClientException e) {
			Assert.fail(e.getMessageFormattedText());
		} catch (AddTemplatesException e) {
			Assert.fail(e.toString());
		}
	}
	
	private void assertFedoraTemplate() throws IOException, InterruptedException {	
		// test custom commands
		final String basicCommand = "connect " + getRestUrl() + "; invoke " + SERVICE_NAME + " ";
		// GetTemplateName
		final String invokeGetTemplateNameResult  = 
				CommandTestUtils.runCommandAndWait(basicCommand + GET_TEMPLATE_NAME_CUSTOM_COMMAND_NAME);
		Assert.assertNotNull(invokeGetTemplateNameResult);
		String[] getTemplateNameResultSplit = invokeGetTemplateNameResult.split("Template:");
		Assert.assertEquals(2, getTemplateNameResultSplit.length);
		String[] templateNameSplit = getTemplateNameResultSplit[1].split("\n");
		Assert.assertTrue(templateNameSplit.length > 1);
		String templateName = templateNameSplit[0].trim();
		Assert.assertEquals(TEMPLATE_NAME, templateName);
		// GetImageID
		final String invokeGetImageIDResult  = 
				CommandTestUtils.runCommandAndWait(basicCommand + GET_IMAGE_ID_CUSTOM_COMMAND_NAME);
		Assert.assertNotNull(invokeGetImageIDResult);
		String[] getImageIDResultSplit = invokeGetImageIDResult.split("Image ID:");
		Assert.assertEquals(2, getImageIDResultSplit.length);
		String[] imageIDSplit = getImageIDResultSplit[1].split("\n");
		Assert.assertTrue(imageIDSplit.length > 1);
		String imageID = imageIDSplit[0].trim();
		Assert.assertEquals(IMAGE_ID, imageID);
	}
	
}
