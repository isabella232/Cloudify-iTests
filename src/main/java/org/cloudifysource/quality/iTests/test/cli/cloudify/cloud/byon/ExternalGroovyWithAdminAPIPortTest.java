/*
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
 * *****************************************************************************
 */
package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.byon;

import iTests.framework.utils.SSHUtils;

import java.io.File;
import java.net.MalformedURLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.rest.request.InvokeCustomCommandRequest;
import org.cloudifysource.dsl.rest.response.InvokeServiceCommandResponse;
import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
import org.cloudifysource.quality.iTests.test.cli.cloudify.CommandTestUtils;
import org.cloudifysource.quality.iTests.test.cli.cloudify.util.NewRestTestUtils;
import org.cloudifysource.restclient.RestClient;
import org.cloudifysource.restclient.exceptions.RestClientException;
import org.junit.Assert;
import org.openspaces.admin.machine.Machine;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Checks that external groovy script that starts Admin API uses the correct port range.
 * 
 * @author yael
 *
 */
public class ExternalGroovyWithAdminAPIPortTest extends AbstractByonCloudTest {
	
	private static final String SERVICE_FOLDER_PATH = 
			CommandTestUtils.getPath("src/main/resources/apps/USM/usm/services/simpleCustomCmd");
	private static final String SERVICE_NAME = "simple";
	private static final String CUSTOM_COMMAND_NAME = "init";
	private static final long NETSTAT_TIMEOUT_MILLIS = 1000;
	
	private RestClient restClient;
	
	@BeforeClass
	protected void bootstrap() throws Exception {
		super.bootstrap();
		String restUrl = getRestUrl();
//		String restUrl = "http://pc-lab102:8100/";
		restClient = NewRestTestUtils.createAndConnect(restUrl);
	}
	
	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 2, enabled = true)
	public void test() 
			throws MalformedURLException, RestClientException, InterruptedException {
				
		// install the service
		installService();
		// invoke custom command and save into future object
		Future<InvokeServiceCommandResponse> future = executeInvokeCustomCommand();
		// wait for a second before performing netstat
		Thread.sleep(5000);
		// run netstat
		String netstatResult = netstat();
		// wait for invocation to end and get the invocation's result
		String invocationResult = getInvocationResult(future);
		// get the pid from the invocation result
		String pid = getPidFromInvocationResult(invocationResult);
		// get the listening port of the PID from the netstat result.
		int port = getPortFromNetstatResults(netstatResult, pid);
		//assert - check that listening port is in the correct range (7010-7110)
		AssertPortInTheCorrectRange(port);
	}

	private void AssertPortInTheCorrectRange(int port) {
		String[] portRange = CloudifyConstants.LRMI_BIND_PORT_RANGE.split("-");
		int lowerPortRange = Integer.parseInt(portRange[0]);
		int highPortRange = Integer.parseInt(portRange[1]);
		Assert.assertTrue("port [" + port + "] is not in the right range [" + CloudifyConstants.LRMI_BIND_PORT_RANGE + "]", 
				port >= lowerPortRange && port <= highPortRange);
	}

	private int getPortFromNetstatResults(final String netstatResult, final String pid) {
		int indexOfPid = netstatResult.indexOf(pid);
		if (indexOfPid == -1) {
			Assert.fail("failed to find process id " + pid + " in the netstat results: " + netstatResult);
		}
		String netstatResultSubstring = netstatResult.substring(0, indexOfPid);
		String netstatPIDRow = netstatResultSubstring.substring(netstatResultSubstring.lastIndexOf("tcp"));
		String[] split = netstatPIDRow.split(":");
		String portStr = split[4].trim();
		return Integer.parseInt(portStr);
	}

	private Future<InvokeServiceCommandResponse> executeInvokeCustomCommand() {
		return Executors.newSingleThreadExecutor().submit(new Callable<InvokeServiceCommandResponse>() {

			@Override
			public InvokeServiceCommandResponse call() throws Exception {
				return invokeCustomCommand(restClient);
			}
		});
	}

	private String getInvocationResult(Future<InvokeServiceCommandResponse> future) {
		InvokeServiceCommandResponse invokeServiceResponse = null;
		try {
			invokeServiceResponse = future.get();
		} catch (Exception e) {
			Assert.fail("failed to get invocation result: " + e.getMessage());
		} 
		Assert.assertNotNull(invokeServiceResponse);
		
		// get invocation result
		Collection<Map<String, String>> instancesInvocationResults = invokeServiceResponse.getInvocationResultPerInstance().values();
		Assert.assertEquals("expected 1 instance", 1, instancesInvocationResults.size());
		Map<String, String> instanceInvocationResults = instancesInvocationResults.iterator().next();
		String invocationResult = null;
		for (Entry<String, String> valueEntry : instanceInvocationResults.entrySet()) {
			String key = valueEntry.getKey();
			if (CloudifyConstants.INVOCATION_RESPONSE_RESULT.equals(key)) {
				invocationResult = valueEntry.getValue();
			}
		}
		Assert.assertNotNull("invocation result should not be null, custom command invocation results per instance: " 
				+ invokeServiceResponse.getInvocationResultPerInstance(), invocationResult);
		
		return invocationResult;
	}

	private String getPidFromInvocationResult(String invocationResult) {
		String str = "pid <";
		int beginIndex = invocationResult.indexOf(str) + str.length();
		int endIndex = invocationResult.indexOf(">", beginIndex);
		return invocationResult.substring(beginIndex, endIndex);
	}

	private void installService() {
		 File serviceFolder = new File(SERVICE_FOLDER_PATH);
		 try {
			NewRestTestUtils.installServiceUsingNewRestAPI(restClient, serviceFolder, SERVICE_NAME);
		} catch (Exception e) {
			Assert.fail("failed to install service [" + SERVICE_NAME + "] : " + e.getMessage());
		} 
	}

	private InvokeServiceCommandResponse invokeCustomCommand(RestClient restClient) {
		InvokeCustomCommandRequest request = new InvokeCustomCommandRequest();
		request.setCommandName(CUSTOM_COMMAND_NAME);
		InvokeServiceCommandResponse response = null;
		try {
			response = restClient.invokeServiceCommand(CloudifyConstants.DEFAULT_APPLICATION_NAME, SERVICE_NAME, request);
		} catch (RestClientException e) {
			Assert.fail("failed to invoke service command: " + e.getMessageFormattedText());
		}
		return response;
	}

	private String netstat() {
		
		String command = "sudo netstat -anp | grep LISTEN";
		List<Machine> puMachines = getProcessingUnitMachines(CloudifyConstants.DEFAULT_APPLICATION_NAME + "." + SERVICE_NAME);
		Machine machine = puMachines.get(0);
		String hostAddress = machine.getHostAddress();
//		String hostAddress = "pc-lab114";
		return SSHUtils.runCommand(hostAddress, NETSTAT_TIMEOUT_MILLIS, command , USERNAME, PASSWORD);
	}

}
