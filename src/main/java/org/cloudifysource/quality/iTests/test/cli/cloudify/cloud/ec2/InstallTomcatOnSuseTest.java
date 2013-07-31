/*******************************************************************************
 * Copyright (c) 2013 GigaSpaces Technologies Ltd. All rights reserved
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *******************************************************************************/
package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.ec2;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import junit.framework.Assert;

import org.apache.commons.io.FileUtils;
import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
import org.cloudifysource.quality.iTests.test.cli.cloudify.CommandTestUtils;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.NewAbstractCloudTest;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Bootstraps cloud ec2.
 * Adds new template - SMALL_SUSE. 
 * Installs Tomcat service on Suse, using a custom Tomcat that declares the new added template. 
 * @author yael
 *
 */
public class InstallTomcatOnSuseTest extends NewAbstractCloudTest {

	private static final String TEMPLATE_NAME = "SMALL_SUSE";
	private static final String TEMPLATE_DIR_PATH = CommandTestUtils.getPath("src/main/resources/templates/SMALL_SUSE");
	private static final String TEMPLATE_PROPERTIES_FILE_PATH = CommandTestUtils.getPath("src/main/resources/templates/SMALL_SUSE/SMALL_SUSE-template.properties");
	private static final String SERVICE_NAME = "tomcat";
	private static final String SERVICE_DIR_PATH = CommandTestUtils.getPath("src/main/resources/apps/USM/usm/tomcatOnSuse");
	private static final String SUSE_IMAGE_ID = "eu-west-1/ami-60576214";
	private static final String GET_TEMPLATE_NAME_CUSTOM_COMMAND_NAME = "GetTemplateName";
	private static final String GET_IMAGE_ID_CUSTOM_COMMAND_NAME = "GetImageID";

	// properties
	private static final String SUSE_IMAGE_ID_PROPERTY_VALUE = "\"" + SUSE_IMAGE_ID + "\"";
	private static final String SUSE_IMAGE_ID_PROPERTY_NAME = "suseImageId";
	private static final String LOCATION_ID_PROPERTY_VALUE = "\"eu-west-1\"";
	private static final String LOCATION_ID_PROPERTY_NAME = "locationId";
	private static final String HARDWARE_ID_PROPERTY_VALUE = "\"m1.small\"";
	private static final String HARDWARE_ID_PROPERTY_NAME = "hardwareId";
	private static final String KEY_PAIR_PROPERTY_VALUE = "\"ec2-sgtest-eu\"";
	private static final String KEY_PAIR_PROPERTY_NAME = "keyPair";
	private static final String KEY_FILE_PROPERTY_VALUE = "\"ec2-sgtest-eu.pem\"";
	private static final String KEY_FILE_PROPERTY_NAME = "keyFile";
	private static final int TOMCAT_INSTALLATION_TIMEOUT_IN_MINUTES = 15;
				
	@BeforeClass(alwaysRun = true)
	protected void bootstrap() throws Exception {
		super.bootstrap();
	}
	
	@AfterClass(alwaysRun = true)
	protected void teardown() throws Exception {
		super.teardown();
	}
	
	@Override
	protected void afterBootstrap() throws Exception {
		super.afterBootstrap();
	}
	
	@Override
	protected String getCloudName() {
		return "ec2";
	}

	@Override
	protected boolean isReusableCloud() {
		return false;
	}

	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 4, enabled = false)
	public void testInstallCustomTomcatOnSuse() throws IOException, InterruptedException {
		addSuseTempalte();
		try {
			installServiceAndWait(SERVICE_DIR_PATH, SERVICE_NAME, TOMCAT_INSTALLATION_TIMEOUT_IN_MINUTES);
			assertSuseTemplate();
		} finally {
			uninstallServiceIfFound(SERVICE_NAME);	
		}
	}

	
	private void addSuseTempalte() throws IOException, InterruptedException {
		// create properties
		File propsFile = new File(TEMPLATE_PROPERTIES_FILE_PATH);	
		Properties properties = new Properties();
		properties.setProperty(SUSE_IMAGE_ID_PROPERTY_NAME, SUSE_IMAGE_ID_PROPERTY_VALUE);
		properties.setProperty(LOCATION_ID_PROPERTY_NAME, LOCATION_ID_PROPERTY_VALUE);
		properties.setProperty(HARDWARE_ID_PROPERTY_NAME, HARDWARE_ID_PROPERTY_VALUE);
		properties.setProperty(KEY_FILE_PROPERTY_NAME, KEY_FILE_PROPERTY_VALUE);
		properties.setProperty(KEY_PAIR_PROPERTY_NAME, KEY_PAIR_PROPERTY_VALUE);
		// write properties to file
		properties.store(new FileOutputStream(propsFile), null);
		String readFileToString = FileUtils.readFileToString(propsFile);
		FileUtils.writeStringToFile(propsFile, readFileToString.replaceAll("#", "//"));
		// add templates
		String command = "connect " + getRestUrl() + ";add-templates " + TEMPLATE_DIR_PATH;
		String output = CommandTestUtils.runCommandAndWait(command);
		Assert.assertTrue(output.contains("Templates added successfully"));
		Assert.assertTrue(output.contains(TEMPLATE_NAME));
	}

	
	private void assertSuseTemplate() throws IOException, InterruptedException {	
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
		Assert.assertEquals(SUSE_IMAGE_ID, imageID);
	}

}
