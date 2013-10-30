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
package org.cloudifysource.quality.iTests.test.rest;

import java.io.File;

import junit.framework.Assert;

import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
import org.cloudifysource.quality.iTests.test.cli.cloudify.AbstractLocalCloudTest;
import org.cloudifysource.quality.iTests.test.cli.cloudify.CommandTestUtils;
import org.cloudifysource.quality.iTests.test.cli.cloudify.NewRestTestUtils;
import org.testng.annotations.Test;

public class ValidateApplicationServicesTest extends AbstractLocalCloudTest {
	
	private static final String INVALID_CUSTOM_COMMAND_NAME_APP_FOLDER_PATH = 
			CommandTestUtils.getPath("src/main/resources/apps/USM/usm/applications/invalid-custom-command");
	private static final String APP_NAME = "simple";
	

	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 2, enabled = true)
	public void testValidateCustomCommandName() {
		File appFolder = new File(INVALID_CUSTOM_COMMAND_NAME_APP_FOLDER_PATH);
		try {
			NewRestTestUtils.installApplicationUsingNewRestApi(
						restUrl, 
						APP_NAME, 
						appFolder, 
						null /* overrides file */, 
						"custom command name [cloudify:] should not start with");
		} catch (Exception e) {
			Assert.fail(e.getLocalizedMessage());
		}
	}

}
