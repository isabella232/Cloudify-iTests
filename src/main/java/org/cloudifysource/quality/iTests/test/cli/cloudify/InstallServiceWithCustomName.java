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

import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.rest.response.InstallServiceResponse;
import org.cloudifysource.dsl.rest.response.UninstallServiceResponse;
import org.testng.annotations.Test;

public class InstallServiceWithCustomName extends AbstractLocalCloudTest {
	private static final String SERVICE_UPDATED_NAME = "simpleNewName";
	private static final String SERVICE_DIR_PATH = 
			CommandTestUtils.getPath("src/main/resources/apps/USM/usm/simple");
	private static final int INSTALL_TIMEOUT_MILLIS = 60 * 15 * 1000;
	
	// using the CLI to install service with a customized name (use the -name option).
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, groups = "1")
	public void installServiceWithCustomNameCLITest() 
			throws IOException, InterruptedException {

        StringBuilder installationCommand = new StringBuilder()
        .append("install-service").append(" ")
        .append("--verbose").append(" ")
        .append("-timeout").append(" ")
        .append(INSTALL_TIMEOUT_MILLIS).append(" ")
        .append("-name").append(" ").append(SERVICE_UPDATED_NAME).append(" ")
        .append(SERVICE_DIR_PATH.replace('\\', '/'));
        String connectCommand = "connect " + restUrl;
        String output = CommandTestUtils.runCommandExpectedFail(connectCommand  + ";" + installationCommand);
        AssertUtils.assertTrue(output, output.contains("Service \"" + SERVICE_UPDATED_NAME + "\" successfully installed"));
       
        uninstallService(SERVICE_UPDATED_NAME);
		
	}
	
	// using the new REST client to install service with a customized name (pass the customized name in the request).
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, groups = "1")
	public void installServiceWithCustomNameRestClientTest() 
			throws IOException, InterruptedException {
		InstallServiceResponse response = NewRestTestUtils.installServiceUsingNewRestAPI(restUrl, new File(SERVICE_DIR_PATH), 
				CloudifyConstants.DEFAULT_APPLICATION_NAME, SERVICE_UPDATED_NAME, INSTALL_TIMEOUT_MILLIS, null);
		String deploymentID = response.getDeploymentID();
		UninstallServiceResponse uninstallServiceResponse = 
				NewRestTestUtils.uninstallServiceUsingNewRestClient(restUrl, SERVICE_UPDATED_NAME, deploymentID, INSTALL_TIMEOUT_MILLIS);
		Assert.assertEquals(deploymentID, uninstallServiceResponse.getDeploymentID());
	}
}
