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
import iTests.framework.utils.LogUtils;
import iTests.framework.utils.ScriptUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import junit.framework.Assert;

import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.DSLException;
import org.cloudifysource.dsl.internal.packaging.PackagingException;
import org.cloudifysource.dsl.rest.response.InstallApplicationResponse;
import org.cloudifysource.dsl.rest.response.ServiceDescription;
import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
import org.cloudifysource.restclient.RestClient;
import org.cloudifysource.restclient.exceptions.RestClientException;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.openspaces.admin.pu.events.ProcessingUnitInstanceAddedEventListener;
import org.openspaces.admin.pu.events.ProcessingUnitInstanceRemovedEventListener;
import org.testng.annotations.Test;

/**
 * Checks the output of the uninstall process.
 * @author yael
 *
 */
public class UninstallOutputTest extends AbstractLocalCloudTest {

	private static final String APP_NAME = "petclinic";
	private static final String APP_PATH = ScriptUtils.getBuildPath() + "/recipes/apps/" + APP_NAME;
	private static final String SERVICE_NAME = "tomcat";
	private static final String SERVICE_PATH = ScriptUtils.getBuildPath() + "/recipes/services/" + SERVICE_NAME;	


	/**
	 * Uninstall application and check whether the output contains all services and their number of running instances as expected.
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws DSLException
	 * @throws PackagingException
	 * @throws RestClientException
	 */
	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
	public void uninstallApplicationTest() throws IOException, InterruptedException, DSLException, PackagingException, RestClientException {

		ProcessingUnitInstanceAddedEventListener addedListener = getAddedListener();
		ProcessingUnitInstanceRemovedEventListener removedListener = getRemovedListener();
		admin.getProcessingUnits().getProcessingUnitInstanceAdded().add(addedListener);
		admin.getProcessingUnits().getProcessingUnitInstanceRemoved().add(removedListener);

		try {
			// install
			File applicationFolder = new File(APP_PATH);
			InstallApplicationResponse response = NewRestTestUtils.installApplicationUsingNewRestApi(restUrl, APP_NAME, applicationFolder);

			// get all services planned instances
			Map<String, Integer> servicesPlannedInstances = new HashMap<String, Integer>();
			RestClient restClient = NewRestTestUtils.createAndConnect(restUrl);
			List<ServiceDescription> serviceDescriptions = restClient.getServiceDescriptions(response.getDeploymentID());
			for (ServiceDescription serviceDescription : serviceDescriptions) {
				int instanceCount = serviceDescription.getInstanceCount();
				String serviceName = serviceDescription.getServiceName();
				if (instanceCount == 0) {
					ServiceDescription serviceDescription2 = restClient.getServiceDescription(APP_NAME, serviceName);
					LogUtils.log("Instances count of " + serviceName + " after installation was 0, got it again from service's description and it was : " 
							+ serviceDescription2.getInstanceCount() + " current time: " + System.currentTimeMillis());
				}
				int plannedInstances = serviceDescription.getPlannedInstances();
				Assert.assertEquals("instanceCount [" + instanceCount + "] is not equal to plannedInstances [" + plannedInstances + "]", 
						instanceCount, plannedInstances);
				servicesPlannedInstances.put(serviceName, instanceCount);
			}
			System.out.println("Instances count after install: " + servicesPlannedInstances);
			LogUtils.log("Instances count after install: " + servicesPlannedInstances + " current time: " + System.currentTimeMillis());
			ServiceDescription apacheLBDescription = restClient.getServiceDescription(APP_NAME, "apacheLB");
			LogUtils.log("ApacheLB instance count after install: " + apacheLBDescription.getInstanceCount() + " current time: " + System.currentTimeMillis());

			// un-install
			final String uninstallOutput = runCommand("connect " + restUrl + ";uninstall-application --verbose " + APP_NAME);
			AssertUtils.repetitiveAssertConditionHolds("", new AssertUtils.RepetitiveConditionProvider() {
				@Override
				public boolean getCondition() {
					return uninstallOutput.contains("Application " + APP_NAME + " uninstalled successfully");
				}
			}, 5);
			LogUtils.log("uninstalled at: " + System.currentTimeMillis());		
			
			// asserts
			for (Entry<String, Integer> entry : servicesPlannedInstances.entrySet()) {
				String serviceName = entry.getKey();
				Integer installed = entry.getValue();

				int indexOfService = uninstallOutput.indexOf(serviceName + ":");
				Assert.assertTrue("the output doesn't contain service name [" + serviceName + "], output: " + uninstallOutput, indexOfService != -1);
				int indexOfInstalled = uninstallOutput.indexOf("installed", indexOfService);
				Assert.assertTrue("the output doesn't contain the string \"installed\" after service name [" + serviceName + "], output: " + uninstallOutput, indexOfInstalled != -1);
				int indexOfPlanned = uninstallOutput.indexOf("planned", indexOfInstalled);
				Assert.assertTrue("the output doesn't contain the string \"planned\" after \"installed\" for service [" + serviceName + "], output: " + uninstallOutput, indexOfPlanned != -1);

				String initialInstalledCount = uninstallOutput.substring(indexOfInstalled + 9, indexOfPlanned).trim();
				String currentServiceLine = uninstallOutput.substring(indexOfService, indexOfPlanned);
				Assert.assertEquals(currentServiceLine, installed, Integer.valueOf(initialInstalledCount));
			}
		} finally {
			admin.getProcessingUnits().getProcessingUnitInstanceAdded().remove(addedListener);
			admin.getProcessingUnits().getProcessingUnitInstanceRemoved().remove(removedListener);
		}
	}

	
	private ProcessingUnitInstanceAddedEventListener getAddedListener() {
		ProcessingUnitInstanceAddedEventListener eventListener = new ProcessingUnitInstanceAddedEventListener() {

			@Override
			public void processingUnitInstanceAdded(ProcessingUnitInstance processingUnitInstance) {
				if (processingUnitInstance.getProcessingUnit().getName().contains("apacheLB")) {
					LogUtils.log(APP_NAME + ".apacheLB instance added at: " + System.currentTimeMillis());
				}
			}
		};
		return eventListener;
	}
	
	
	private ProcessingUnitInstanceRemovedEventListener getRemovedListener() {
		ProcessingUnitInstanceRemovedEventListener eventListener = new ProcessingUnitInstanceRemovedEventListener() {
			
			@Override
			public void processingUnitInstanceRemoved(ProcessingUnitInstance processingUnitInstance) {
				if (processingUnitInstance.getProcessingUnit().getName().contains("apacheLB")) {
					LogUtils.log(APP_NAME + ".apacheLB instance removed at: " + System.currentTimeMillis());
				}
				
			}
		};
		return eventListener;
	}
	
	/**
	 * Uninstall 1 instance of a service and check whether the output contains "installed 1 planned 0".
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws RestClientException
	 */
	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
	public void uninstallServiceTest() throws IOException, InterruptedException, RestClientException {
		// install
		File serviceFolder = new File(SERVICE_PATH);
		NewRestTestUtils.installServiceUsingNewRestAPI(restUrl, serviceFolder , CloudifyConstants.DEFAULT_APPLICATION_NAME, SERVICE_NAME, 5);
		RestClient restClient = NewRestTestUtils.createAndConnect(restUrl);
		ServiceDescription serviceDescription = restClient.getServiceDescription(CloudifyConstants.DEFAULT_APPLICATION_NAME, SERVICE_NAME);
		int installed = serviceDescription.getInstanceCount();
		int planned = serviceDescription.getPlannedInstances();
		Assert.assertEquals(planned, installed);
		
		// un-install
		final String uninstallOutput = runCommand("connect " + restUrl + ";uninstall-service --verbose " + SERVICE_NAME);
		AssertUtils.repetitiveAssertConditionHolds("", new AssertUtils.RepetitiveConditionProvider() {
			@Override
			public boolean getCondition() {
				return uninstallOutput.contains("Successfully undeployed " + SERVICE_NAME);
			}
		}, 5);
		
		// asserts
		Assert.assertTrue(uninstallOutput, uninstallOutput.contains(SERVICE_NAME + ": installed " + 	installed + " planned 0"));
		}
	
}
