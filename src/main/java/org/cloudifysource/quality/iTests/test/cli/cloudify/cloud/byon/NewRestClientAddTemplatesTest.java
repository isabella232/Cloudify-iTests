 /* Copyright (c) 2013 GigaSpaces Technologies Ltd. All rights reserved
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
package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.byon;

import iTests.framework.utils.AssertUtils;

import java.io.File;
import java.io.IOException;

import junit.framework.Assert;

import org.apache.commons.io.FileUtils;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.packaging.ZipUtils;
import org.cloudifysource.dsl.rest.AddTemplatesException;
import org.cloudifysource.dsl.rest.response.AddTemplatesResponse;
import org.cloudifysource.dsl.rest.response.InstallServiceResponse;
import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
import org.cloudifysource.quality.iTests.test.cli.cloudify.RestTestUtils;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.templates.TemplateDetails;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.templates.TemplatesCommandsRestAPI;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.templates.TemplatesFolderHandler;
import org.cloudifysource.restclient.exceptions.RestClientException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class NewRestClientAddTemplatesTest extends AbstractByonAddRemoveTemplatesTest {

	@BeforeClass(alwaysRun = true)
	protected void bootstrap() throws Exception {		
		super.bootstrap();
	}
	
	@Override
	public int getNumOfMngMachines() {
		return 1;
	}
	
	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 2, enabled = true)
	public void basicAddTemplatesTest() throws IOException, RestClientException, AddTemplatesException {
		TemplatesFolderHandler folderHandler1 = templatesHandler.createFolderWithTemplates(5);
		TemplatesFolderHandler folderHandler2 = templatesHandler.createFolderWithTemplates(5);
		TemplatesFolderHandler folderHandler3 = templatesHandler.createFolderWithTemplates(5);
		templatesHandler.addTemplatesToCloudUsingRestAPI(folderHandler1);
		templatesHandler.addTemplatesToCloudUsingRestAPI(folderHandler2);
		templatesHandler.addTemplatesToCloudUsingRestAPI(folderHandler3);
		templatesHandler.assertExpectedList();
	}
	
	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 2, enabled = true)
	public void addTemplateAndInstallService() throws IOException, RestClientException, AddTemplatesException {
		TemplatesFolderHandler folderHandler = templatesHandler.createNewTemplatesFolder();
		TemplateDetails template = folderHandler.addTempalteForServiceInstallation();
		// add template
		templatesHandler.addTemplatesToCloudUsingRestAPI(folderHandler);
		templatesHandler.assertExpectedList();

		final String templateName = template.getTemplateName();
		String serviceName = templateName + "_service";
		String restUrl = getRestUrl();
		File serviceDir = serviceCreator.createServiceDir(serviceName, templateName);
		InstallServiceResponse installServiceResponse = RestTestUtils.installServiceUsingNewRestAPI(restUrl, serviceDir, CloudifyConstants.DEFAULT_APPLICATION_NAME, serviceName, 5);
		assertRightUploadDir(serviceName, template.getUploadDirName());
		RestTestUtils.uninstallServiceUsingNewRestClient(restUrl, serviceName, installServiceResponse.getDeploymentID(), 5);
	}

	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 2, enabled = true)
	public void addZippedTemplateAndInstallService() throws IOException, RestClientException, AddTemplatesException {
		TemplatesFolderHandler folderHandler = templatesHandler.createNewTemplatesFolder();
		TemplateDetails template = folderHandler.addTempalteForServiceInstallation();
		File templateFolder = template.getTemplateFolder();
		File zippedTemplateFile = new File(templateFolder + File.separator + ".."  + File.separator + "zipped-template.zip");
		ZipUtils.zip(templateFolder, zippedTemplateFile);
		AssertUtils.assertTrue("zip file not found,  zip failed", zippedTemplateFile.exists());
		
		// add templates
		folderHandler.setFolder(zippedTemplateFile);
		templatesHandler.addTemplatesToCloudUsingRestAPI(folderHandler);
		templatesHandler.assertExpectedList();

		final String templateName = template.getTemplateName();
		String serviceName = templateName + "_service";
		
		installAndUninstall(getRestUrl(), serviceName, templateName, template.getUploadDirName(), 5);
	}

	private void installAndUninstall(String restUrl, String serviceName, String templateName, String uploadDirName, int timeout) 
			throws IOException {
		File serviceDir = serviceCreator.createServiceDir(serviceName, templateName);
		InstallServiceResponse installServiceResponse = 
				RestTestUtils.installServiceUsingNewRestAPI(getRestUrl(), serviceDir, CloudifyConstants.DEFAULT_APPLICATION_NAME, serviceName, timeout);
		try {
			assertRightUploadDir(serviceName, uploadDirName);
		} finally {
			RestTestUtils.uninstallServiceUsingNewRestClient(getRestUrl(), serviceName, installServiceResponse.getDeploymentID(), timeout);
		}
	}

	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 2, enabled = true)
	public void templatesWithTheSameUpload() throws IOException, InterruptedException, RestClientException, AddTemplatesException {
		TemplatesFolderHandler folderHandler = templatesHandler.createNewTemplatesFolder();
		TemplateDetails template1 = folderHandler.addTempalteForServiceInstallation();
		TemplateDetails templateDetails = new TemplateDetails();
		templateDetails.setUploadDirName(template1.getUploadDirName());
		templateDetails.setForServiceInstallation(true);
		TemplateDetails template2 = folderHandler.addCustomTemplate(templateDetails);
		// try to add the templates
		templatesHandler.addTemplatesToCloudUsingRestAPI(folderHandler);
		templatesHandler.assertExpectedList();
		// install 2 services
		installAndUninstall(getRestUrl(), template1.getTemplateName() + "_service", template1.getTemplateName(), template1.getUploadDirName(), 5);
		installAndUninstall(getRestUrl(), template2.getTemplateName() + "_service", template2.getTemplateName(), template2.getUploadDirName(), 5);
	}

	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 2, enabled = true)
	public void addExistAndNotExistTemplates() throws IOException, RestClientException, AddTemplatesException {
		TemplatesFolderHandler templatesFolder1 = templatesHandler.createFolderWithTemplates(2);
		String templateName = templatesFolder1.getExpectedToBeAddedTempaltes().get(0);
		TemplateDetails addedTemplate = templatesFolder1.getTemplates().get(templateName);

		templatesHandler.addTemplatesToCloudUsingRestAPI(templatesFolder1);
		templatesHandler.assertExpectedList();

		addedTemplate.setTemplateFolder(null);
		TemplatesFolderHandler templatesFolder2 = templatesHandler.createNewTemplatesFolder();
		addedTemplate.setExpectedToFail(true);
		templatesFolder2.addCustomTemplate(addedTemplate);
		AddTemplatesResponse response = templatesHandler.addTemplatesToCloudUsingRestAPI(templatesFolder2);
		templatesHandler.assertExpectedList();
		String errMsg = response.getTemplates().get(addedTemplate.getTemplateName()).getFailedToAddHosts().get(response.getInstances().get(0));
		Assert.assertEquals("template already exists", errMsg);
	}

	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 2, enabled = true)
	public void templateNotExists() throws IOException, InterruptedException {
		String serviceName = "notExistTemplate_service";
		File serviceDir = serviceCreator.createServiceDir(serviceName, "notExistTemplate");
		RestTestUtils.installServiceUsingNewRestAPI(
				getRestUrl(), 
				serviceDir, 
				CloudifyConstants.DEFAULT_APPLICATION_NAME, 
				serviceName, 
				5, 
				"template [notExistTemplate] does not exist at clouds template list");
	}

	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 2, enabled = true)
	public void removeTemplateAndTryToInstallService() throws IOException, InterruptedException {
		TemplatesFolderHandler folderHandler = templatesHandler.createNewTemplatesFolder();
		TemplateDetails tempalte = folderHandler.addDefaultTempalte();
		templatesHandler.addTemplatesToCloud(folderHandler);
		templatesHandler.assertExpectedList();

		final String templateName = tempalte.getTemplateName();
		templatesHandler.removeTemplatesFromCloudUsingRestAPI(folderHandler, templateName, false, null);
		templatesHandler.assertExpectedList();

		String serviceName = templateName + "_service";
		File serviceDir = serviceCreator.createServiceDir(serviceName, templateName);
		RestTestUtils.installServiceUsingNewRestAPI(
				getRestUrl(), 
				serviceDir, 
				CloudifyConstants.DEFAULT_APPLICATION_NAME, 
				serviceName, 
				5, 
				"template [" + templateName + "] does not exist");
	}

	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 2, enabled = true)
	public void removeNotExistTemplate() {
		TemplatesCommandsRestAPI.removeTemplate(getRestUrl(), "not_exist", true, "Failed to remove template [not_exist]");
		templatesHandler.assertExpectedList();
	}

	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 2, enabled = false)
	public void illegalDuplicateTemplatesInTheSameFolder() throws IOException, AddTemplatesException {
		
		TemplatesFolderHandler folderHandler = templatesHandler.createNewTemplatesFolder();
		TemplateDetails addedTemplate = folderHandler.addDefaultTempalte();

		TemplateDetails duplicateTemplate = new TemplateDetails();
		String tempalteName = addedTemplate.getTemplateName();
		duplicateTemplate.setTemplateName(tempalteName);
		File duplicateTemplateFile = new File(addedTemplate.getTemplateFolder(), "duplicateTemplate-template.groovy");
		duplicateTemplate.setTemplateFile(duplicateTemplateFile);
		duplicateTemplate.setUploadDirName(addedTemplate.getUploadDirName());
		duplicateTemplate.setMachineIP(addedTemplate.getMachineIP());
		duplicateTemplate.setExpectedToFail(true);
		folderHandler.addCustomTemplate(duplicateTemplate);
		// try to add the template
		try {
			templatesHandler.addTemplatesToCloudUsingRestAPI(folderHandler);
			Assert.fail("Expected RestClientException");
		} catch (RestClientException e) {
			Assert.assertTrue(e.getMessageFormattedText().contains("Template with name [" + tempalteName + "] already exist in folder"));
		}
		
		templatesHandler.assertExpectedList();
	}

	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 2, enabled = false)
	public void illegalTemplateWithoutLocalUploadDir() throws IOException, AddTemplatesException {
		TemplatesFolderHandler folderHandler = templatesHandler.createNewTemplatesFolder();
		TemplateDetails addedTemplate = folderHandler.addExpectedToFailTempalte();
		// delete upload directory
		File uploadDir = new File(addedTemplate.getTemplateFolder(), addedTemplate.getUploadDirName());
		FileUtils.deleteDirectory(uploadDir);
		// try to add the template
		try {
			 templatesHandler.addTemplatesToCloudUsingRestAPI(folderHandler);
			Assert.fail("Expected RestClientException");
		} catch (RestClientException e) {
			Assert.assertTrue(e.getMessageFormattedText().contains("Could not find upload directory"));
		}
		templatesHandler.assertExpectedList();
	}

	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 2, enabled = true)
	public void tryToRemoveUsedTemplate() throws IOException, RestClientException, AddTemplatesException {
		TemplatesFolderHandler folderHandler = templatesHandler.createNewTemplatesFolder();
		TemplateDetails addedTemplate = folderHandler.addTempalteForServiceInstallation();
		String templateName = addedTemplate.getTemplateName();
		templatesHandler.addTemplatesToCloudUsingRestAPI(folderHandler);
		templatesHandler.assertExpectedList();

		String serviceName = templateName + "_service";
		String restUrl = getRestUrl();
		File serviceDir = serviceCreator.createServiceDir(serviceName, templateName);
		InstallServiceResponse installServiceResponse = RestTestUtils.installServiceUsingNewRestAPI(restUrl, serviceDir, CloudifyConstants.DEFAULT_APPLICATION_NAME, serviceName, 5);
		try {
			assertRightUploadDir(serviceName, addedTemplate.getUploadDirName());
			templatesHandler.removeTemplatesFromCloudUsingRestAPI(folderHandler, templateName, true, "the template is being used by the following services");
			templatesHandler.assertExpectedList();
		} finally {
			RestTestUtils.uninstallServiceUsingNewRestClient(restUrl, serviceName, installServiceResponse.getDeploymentID(), 5);
		}
	}

	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 2, enabled = true)
	public void addRemoveAndAddAgainTemplates() throws IOException, RestClientException, AddTemplatesException {
	
		TemplatesFolderHandler folderHandler = templatesHandler.createNewTemplatesFolder();
		TemplateDetails toRemoveTemplate = folderHandler.addDefaultTempalte();
		templatesHandler.addTemplatesToCloudUsingRestAPI(folderHandler);
		templatesHandler.removeTemplatesFromCloudUsingRestAPI(folderHandler, toRemoveTemplate.getTemplateName(), false, null);
		
		templatesHandler.assertExpectedList();
		
		folderHandler.addCustomTemplate(toRemoveTemplate);
		templatesHandler.addTemplatesToCloudUsingRestAPI(folderHandler);
		
		templatesHandler.assertExpectedList();
	}

}
