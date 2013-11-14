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
package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.byon.persistence;

import iTests.framework.tools.SGTestHelper;
import iTests.framework.utils.LogUtils;

import java.io.File;
import java.util.List;

import junit.framework.Assert;

import org.apache.commons.io.FileUtils;
import org.cloudifysource.quality.iTests.framework.utils.CloudBootstrapper;
import org.cloudifysource.quality.iTests.test.cli.cloudify.CommandTestUtils;
import org.cloudifysource.quality.iTests.test.cli.cloudify.NewRestTestUtils;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.byon.AbstractByonAddRemoveTemplatesTest;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.templates.TemplatesCommandsRestAPI;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.templates.TemplatesFolderHandler;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * 
 * @author yael
 *
 */
public class AddTemplatesPersistenceTest extends AbstractByonAddRemoveTemplatesTest {
	private final String TEMPLATES_ROOT_PATH = CommandTestUtils.getPath("src/main/resources/templates");
	private final String TEMP_TEMPLATES_DIR_PATH = TEMPLATES_ROOT_PATH + File.separator + "templates.tmp";
	private final String MANAGERS_FILE_PATH = SGTestHelper.getBuildDir() + "/backup-details.txt";
    
	@Override
    protected void customizeCloud() throws Exception {
        super.customizeCloud();
        getService().setNumberOfManagementMachines(getNumOfMngMachines());
        getService().getProperties().put("persistencePath", "/tmp/tgrid/persistency");
    }
	
	
	@BeforeClass(alwaysRun = true)
    public void bootstrapAndInit() throws Exception{
        super.bootstrap();
        File templatesTempFolder = new File(TEMP_TEMPLATES_DIR_PATH);
        templatesTempFolder.mkdirs();
    }

    @Test(timeOut = DEFAULT_TEST_TIMEOUT * 4, enabled = true)
    public void testTemplatesPersistency() throws Exception {
    	
    	// add one additional template to the cloud
    	TemplatesFolderHandler folderHandler = templatesHandler.createNewTemplatesFolderHandler();
    	String templateName = folderHandler.createAndAddDefaultTempalte().getTemplateName();
		templatesHandler.addTemplatesToCloud(folderHandler);
		
		// check templates list before shutdown
		List<String> listTemplates = TemplatesCommandsRestAPI.listTemplates(getRestUrl());
		int expectedNumOfTemplates = defaultTemplates.size() + 1;
		Assert.assertEquals("expected total of " + expectedNumOfTemplates + " templates after adding a template: the default templates [" 
				+ defaultTemplates + "] and the one additional template [" + templateName + "]",
				expectedNumOfTemplates, listTemplates.size());
		Assert.assertTrue("list-templates expected to contain the additional template [" + templateName + "]", listTemplates.contains(templateName));
    	
		//shutdown
		LogUtils.log("shutting down managers");
		File persistenceFile = new File(MANAGERS_FILE_PATH);
		if (persistenceFile.exists()) {
			FileUtils.deleteQuietly(persistenceFile);
		}
		closeAdmin();
		NewRestTestUtils.shutdownManagers(getRestUrl(), null, 3, persistenceFile);
		
		//bootstrap
        CloudBootstrapper bootstrapper = getService().getBootstrapper();
        bootstrapper.scanForLeakedNodes(false);
        bootstrapper.useExistingFilePath(MANAGERS_FILE_PATH);
        bootstrapper.bootstrap();
        bootstrapper.setRestUrl(getRestUrl());

		// check templates list after shutdown and bootstrap
		listTemplates = TemplatesCommandsRestAPI.listTemplates(getRestUrl());
		Assert.assertEquals("After shutdown, expected total of " + expectedNumOfTemplates + " templates after adding a template: the default templates " 
				+ defaultTemplates + " and the one additional template [" + templateName + "], instead got " + listTemplates, 
				expectedNumOfTemplates, listTemplates.size());
		Assert.assertTrue("After shutdown, list-templates " + listTemplates + " does not contain the additional template [" + templateName + "]", listTemplates.contains(templateName));
    }

    @AfterClass(alwaysRun = true)
    public void teardown() throws Exception{
        super.teardown();
    }

	@Override
	public int getNumOfMngMachines() {
		return 2;
	}

}
