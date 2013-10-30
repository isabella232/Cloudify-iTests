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

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.cloudifysource.dsl.internal.DSLUtils;
import org.cloudifysource.dsl.rest.AddTemplatesException;
import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.templates.TemplateDetails;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.templates.TemplatesCommandsRestAPI;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.templates.TemplatesFolderHandler;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.templates.TemplatesUtils;
import org.cloudifysource.restclient.exceptions.RestClientException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * 
 * @author yael
 *
 */
public class AddTemplateDifferentFileNameTest extends AbstractByonAddRemoveTemplatesTest {

	@BeforeClass(alwaysRun = true)
	protected void bootstrap() throws Exception {
		super.bootstrap();
	}
	
	@Override
	public int getNumOfMngMachines() {
		return 1;
	}
	
	
	/**
	 * Creates template with name templateName and with file name other than "templateName-template.groovy".
	 * Add, Get and Remove this template, expecting success.
	 * @throws IOException .
	 * @throws InterruptedException .
	 */
	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 2, enabled = true)
	public void addRemoveTemplateWithWrongFileNameTest() throws IOException, InterruptedException {
		
		// create folder with one template
		TemplatesFolderHandler folderHandler = templatesHandler.createNewTemplatesFolderHandler();
		TemplateDetails tempalte = folderHandler.createAndAddDefaultTempalte();
		
		// rename template file
		File templateFile = tempalte.getTemplateFile();
		File parent = templateFile.getParentFile();
		File newNameGroovyFile = new File(parent, "myTemplate-template.groovy");
		templateFile.renameTo(newNameGroovyFile);
		
		// rename properties file
		File templatePropertiesFile = tempalte.getTemplatePropertiesFile();
		File newNamePropertiesFile = new File(parent, "myTemplate-template.properties");
		templatePropertiesFile.renameTo(newNamePropertiesFile);
		
		// add tempalte
		templatesHandler.addTemplatesToCloud(folderHandler);
		templatesHandler.assertExpectedList();
		
		// get tempalte
		String templateName = tempalte.getTemplateName();
		TemplatesCommandsRestAPI.getTemplate(getRestUrl(), templateName);
		
		// remove template
		templatesHandler.removeTemplateFromCloud(folderHandler, templateName, false, null);
		templatesHandler.assertExpectedList();
	
	}
	
	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 2, enabled = true)
	public void IllegalAddTemplatesSameNamesDifferentFileNames() throws IOException, RestClientException, AddTemplatesException {
		
		// create 2 folders with one template each
		TemplatesFolderHandler folderHandler1 = templatesHandler.createNewTemplatesFolderHandler();
		TemplateDetails template = folderHandler1.createAndAddDefaultTempalte();		
		TemplatesFolderHandler folderHandler2 = templatesHandler.createNewTemplatesFolderHandler();
		File folder2 = folderHandler2.getFolder();

		// add template with the same name but different file name
		File duplicateFile = new File(folder2, "duplicate" + DSLUtils.TEMPLATE_DSL_FILE_NAME_SUFFIX);
		FileUtils.copyFile(template.getTemplateFile(), duplicateFile);
		TemplateDetails duplicateTemplate = 
				TemplatesUtils.createTemplate(template.getTemplateName(), duplicateFile , folder2, null);
		duplicateTemplate.setExpectedToFailOnAdd(true);		
		folderHandler2.addCustomTemplate(duplicateTemplate);

		templatesHandler.addTemplatesToCloudUsingRestAPI(folderHandler1);
		templatesHandler.addTemplatesToCloudUsingRestAPI(folderHandler2);
		
		templatesHandler.assertExpectedList();
	}
	
	/**
	 * Creates 2 templates with the same file name and adds them, expecting success. 
	 * @throws IOException .
	 * @throws RestClientException .
	 * @throws AddTemplatesException .
	 */
	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 2, enabled = true)
	public void templatesWithSameFileName() throws IOException, RestClientException, AddTemplatesException {
		TemplatesFolderHandler handler1 = templatesHandler.createFolderWithTemplates(1);
		TemplateDetails template1 = handler1.getTemplates().values().iterator().next();
		TemplatesFolderHandler handler2 = templatesHandler.createFolderWithTemplates(1);
		TemplateDetails template2 = handler2.getTemplates().values().iterator().next();
		// rename templates groovy file
		File template2File = template2.getTemplateFile();
		File parentFile = template2File.getParentFile();
		File newFile = new File(parentFile, template1.getTemplateFile().getName());
		template2File.renameTo(newFile);
		// rename template2 properties file
		File template2PropertiesFile = template2.getTemplatePropertiesFile();
		File propertiesParentFile = template2PropertiesFile.getParentFile();
		File newPropertiesFile = new File(propertiesParentFile, template1.getTemplatePropertiesFile().getName());
		template2PropertiesFile.renameTo(newPropertiesFile);
		
		templatesHandler.addTemplatesToCloudUsingRestAPI(handler1);
		templatesHandler.addTemplatesToCloudUsingRestAPI(handler2);
		
		templatesHandler.assertExpectedList();
		
	}
	
}
