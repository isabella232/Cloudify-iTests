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
import iTests.framework.utils.ScriptUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import junit.framework.Assert;

import org.cloudifysource.dsl.internal.DSLException;
import org.cloudifysource.dsl.internal.packaging.PackagingException;
import org.cloudifysource.dsl.rest.response.InstallApplicationResponse;
import org.cloudifysource.dsl.rest.response.ServiceDescription;
import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
import org.cloudifysource.restclient.RestClient;
import org.cloudifysource.restclient.exceptions.RestClientException;
import org.testng.annotations.Test;

/**
 * Checks the output of the uninstall process.
 * @author yael
 *
 */
public class UninstallOutputTest extends AbstractLocalCloudTest {

	private static final String APP_NAME = "petclinic";
	private static final String APP_PATH = ScriptUtils.getBuildPath() + "/recipes/apps/" + APP_NAME;
	
	/**
	 * Uninstall application and check if the output contain all services and their number of running instances as expected.
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws DSLException
	 * @throws PackagingException
	 * @throws RestClientException
	 */
	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
	public void uninstallApplicationTest() throws IOException, InterruptedException, DSLException, PackagingException, RestClientException {
		// install
		File applicationFolder = new File(APP_PATH);
		InstallApplicationResponse response = NewRestTestUtils.installApplicationUsingNewRestApi(restUrl, APP_NAME, applicationFolder);
		
		// get all services planned instances
		Map<String, Integer> servicesPlannedInstances = new HashMap<String, Integer>();
		RestClient restClient = NewRestTestUtils.createAndConnect(restUrl);
		List<ServiceDescription> serviceDescriptions = restClient.getServiceDescriptions(response.getDeploymentID());
		for (ServiceDescription serviceDescription : serviceDescriptions) {
			int instanceCount = serviceDescription.getInstanceCount();
			int plannedInstances = serviceDescription.getPlannedInstances();
			Assert.assertEquals("instanceCount [" + instanceCount + "] is not equal to plannedInstances [" + plannedInstances + "]", 
					instanceCount, plannedInstances);
			servicesPlannedInstances.put(serviceDescription.getServiceName(), instanceCount);
		}
		
		// un-install
		final String uninstallOutput = runCommand("connect " + restUrl + ";uninstall-application --verbose " + APP_NAME);
		AssertUtils.repetitiveAssertConditionHolds("", new AssertUtils.RepetitiveConditionProvider() {
			@Override
			public boolean getCondition() {
				return uninstallOutput.contains("Application " + APP_NAME + " uninstalled successfully");
			}
		}, 5);
		
		// asserts
		for (Entry<String, Integer> entry : servicesPlannedInstances.entrySet()) {
			String serviceName = entry.getKey();
			Integer installed = entry.getValue();
			
			int indexOfService = uninstallOutput.indexOf(serviceName);
			Assert.assertTrue("the output doesn't contain service name [" + serviceName + "], output: " + uninstallOutput, indexOfService != -1);
			int indexOfInstalled = uninstallOutput.indexOf("installed", indexOfService);
			Assert.assertTrue("the output doesn't contain the string \"installed\" after service name [" + serviceName + "], output: " + uninstallOutput, indexOfInstalled != -1);
			int indexOfPlanned = uninstallOutput.indexOf("planned", indexOfInstalled);
			Assert.assertTrue("the output doesn't contain the string \"planned\" after \"installed\" for service [" + serviceName + "], output: " + uninstallOutput, indexOfPlanned != -1);

			String initialInstalledCount = uninstallOutput.substring(indexOfInstalled + 9, indexOfPlanned).trim();
			System.out.println(serviceName + " installed instances: " + initialInstalledCount);
			Assert.assertEquals(installed, Integer.valueOf(initialInstalledCount));
		}
	}
}
