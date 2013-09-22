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
package org.cloudifysource.quality.iTests.test.cli.cloudify;

import iTests.framework.utils.AssertUtils;

import java.io.File;
import java.io.IOException;

import junit.framework.Assert;

import org.apache.commons.lang.StringUtils;
import org.cloudifysource.dsl.internal.DSLException;
import org.cloudifysource.dsl.internal.packaging.PackagingException;
import org.cloudifysource.dsl.rest.response.InstallApplicationResponse;
import org.cloudifysource.dsl.rest.response.UninstallApplicationResponse;
import org.testng.annotations.Test;

public class InstallApplicationWithCustomName extends AbstractLocalCloudTest {
	private static final String APPLICATION_MODIFIED_NAME = "groovyAppNewName";
	private static final String APPLICATION_DIR_PATH = 
			CommandTestUtils.getPath("src/main/resources/apps/USM/usm/applications/groovyApp");
	private static final int INSTALL_TIMEOUT_MILLIS = 60 * 15 * 1000;

	
	// using the CLI to install application with a customized name (use the -name option).
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, groups = "1")
	public void installApplicationWithCustomNameCLITest() 
			throws IOException, InterruptedException {

        StringBuilder installationCommand = new StringBuilder()
        .append("install-application").append(" ")
        .append("--verbose").append(" ")
        .append("-timeout").append(" ")
        .append(INSTALL_TIMEOUT_MILLIS).append(" ")
        .append("-name").append(" ").append(APPLICATION_MODIFIED_NAME).append(" ")
        .append(APPLICATION_DIR_PATH.replace('\\', '/'));
        String connectCommand = "connect " + restUrl;
        String output = CommandTestUtils.runCommandExpectedFail(connectCommand  + ";" + installationCommand);
        AssertUtils.assertTrue(output, output.contains("Application " + APPLICATION_MODIFIED_NAME + " installed successfully"));
       
        uninstallApplication(APPLICATION_MODIFIED_NAME);
		
	}
	
	// using the new REST client to install application with a customized name (pass the customized name in the request).
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, groups = "1")
	public void installApplicationWithCustomNameRestClientTest() 
			throws IOException, InterruptedException, DSLException, PackagingException {
		InstallApplicationResponse response = 
				RestTestUtils.installApplicationUsingNewRestApi(restUrl, APPLICATION_MODIFIED_NAME, new File(APPLICATION_DIR_PATH), null, null);
		Assert.assertNotNull(response);
		String installDeploymentID = response.getDeploymentID();
		Assert.assertFalse(StringUtils.isBlank(installDeploymentID));
		
		UninstallApplicationResponse uninstallResponse = 
				RestTestUtils.uninstallApplicationUsingNewRestClient(restUrl, APPLICATION_MODIFIED_NAME, installDeploymentID, INSTALL_TIMEOUT_MILLIS);
		Assert.assertNotNull(uninstallResponse);
		String uninstallDeploymentID = uninstallResponse.getDeploymentID();
		Assert.assertFalse(StringUtils.isBlank(uninstallDeploymentID));
		
		Assert.assertEquals(installDeploymentID, uninstallDeploymentID);
	}
}
