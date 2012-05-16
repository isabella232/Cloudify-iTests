/*******************************************************************************
* Copyright (c) 2012 GigaSpaces Technologies Ltd. All rights reserved
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
******************************************************************************/
package test.cli.cloudify.cloud;

import java.io.IOException;
import java.util.HashMap;

import org.junit.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.cli.cloudify.CloudTestUtils;
import test.cli.cloudify.CommandTestUtils;
import test.cli.cloudify.cloud.services.ec2.Ec2CloudService;
import framework.utils.IOUtils;
import framework.utils.LogUtils;
import framework.utils.ScriptUtils;

public class MissingPemEc2Test extends AbstractCloudTest {
	private Ec2CloudService service;

	@BeforeMethod
	public void bootstrap() throws IOException, InterruptedException {	
		service = new Ec2CloudService();
		service.setMachinePrefix(this.getClass().getName() + CloudTestUtils.SGTEST_MACHINE_PREFIX);
		service.setAdditionalPropsToReplace(new HashMap<String, String>());
		//rename pem file so it wouldn't be found later
		service.getAdditionalPropsToReplace().put("keyFile \"cloud-demo.pem\"", "keyFile \"wrong-file.pem\"");
		try{
			service.bootstrapCloud();	
		} catch (Throwable e) {
			Assert.assertTrue("Alert - Pem file is missing but bootstrap didn't fail", e.getMessage().contains("Process did not complete successfully"));
		}
		
		super.setService(service);
		super.getRestUrl();
	}
	
	@AfterMethod(alwaysRun = true)
	public void teardown() throws IOException {
		try {
			service.getAdditionalPropsToReplace().put("keyFile \"wrong-file.pem\"", "keyFile \"cloud-demo.pem\"");
			String pathToCloudGroovy = ScriptUtils.getBuildPath() + "/tools/cli/plugins/esc/" + service.getCloudName() + "/" + service.getCloudName() + "-cloud.groovy";
			IOUtils.replaceTextInFile(pathToCloudGroovy, service.getAdditionalPropsToReplace());
			// TODO : verify there are no servers up, and only if they are up - teardown
			//service.teardownCloud();
		}
		catch (Throwable e) {
			LogUtils.log("caught an exception while tearing down ec2", e);
			sendTeardownCloudFailedMail("ec2", e);
		}
		LogUtils.log("restoring original bootstrap-management file");
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, groups = "1", enabled = true)
	public void installTest() throws Exception {
		doTest(EC2, "petclinic", "petclinic");
	}

	protected void doTest(String cloudName, String applicationFolderName, String applicationName) throws IOException, InterruptedException {
		LogUtils.log("installing application " + applicationName + " on " + cloudName);
		String applicationPath = ScriptUtils.getBuildPath() + "/recipes/apps/" + applicationFolderName;
		try {
			installApplicationAndWait(applicationPath, applicationName);
		}
		finally {
			if ((getService() != null) && (getService().getRestUrls() != null)) {
				String command = "connect " + getRestUrl() + ";list-applications";
				String output = CommandTestUtils.runCommandAndWait(command);
				if (output.contains(applicationName)) {
					uninstallApplicationAndWait(applicationName);			
				}
			}
		}
	}
		
}