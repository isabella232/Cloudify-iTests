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
 ******************************************************************************/
package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.ec2win.agentrestart;

import iTests.framework.utils.AssertUtils;
import iTests.framework.utils.AssertUtils.RepetitiveConditionProvider;
import iTests.framework.utils.LogUtils;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.cloudifysource.dsl.rest.response.ApplicationDescription;
import org.cloudifysource.quality.iTests.test.cli.cloudify.CommandTestUtils;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.NewAbstractCloudTest;
import org.cloudifysource.restclient.GSRestClient;
import org.cloudifysource.restclient.RestClient;
import org.cloudifysource.restclient.RestException;
import org.cloudifysource.restclient.exceptions.RestClientException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.j_spaces.kernel.PlatformVersion;

/**
 * tests agent maintenance mode on windows machines.
 * 
 * @author adaml
 *
 */
public class AgentRestartOnWinTest extends NewAbstractCloudTest {
	
	private static final String SERVICE_NAME = "simpleRestartAgent";
	protected static final String APP_NAME = "default";
	private static final int INSTALL_SERVICE_TIMEOUT_MIN = 30;
	private static final int INFINITY_MINUTES = 600;
	private RestClient newClient;
	private GSRestClient oldClient;
	
	@Override
	protected String getCloudName() {
		return "ec2-win";
	}
	
	@Override
	@BeforeClass(alwaysRun = true)
	protected void bootstrap() throws Exception {
		super.bootstrap();
		this.newClient = new RestClient(new URL(this.getRestUrl()), "", "", PlatformVersion.getVersion());
		this.oldClient = new GSRestClient("", "", new URL(this.getRestUrl()), PlatformVersion.getVersionNumber());
		
	}

	@Override
	protected boolean isReusableCloud() {
		return false;
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 4, enabled = true)
	public void testAgentRestartWindows() throws IOException, InterruptedException {
		
		installServiceAndWait(getServicePath(SERVICE_NAME), SERVICE_NAME, INSTALL_SERVICE_TIMEOUT_MIN);
		
		LogUtils.log("setting agent to maintenance mode.");
		startMaintenanceMode(TimeUnit.MINUTES.toMillis(INFINITY_MINUTES));
		
		LogUtils.log("restarting agent machine..");
		restartWinMachine();
		
		// make sure failure was recorded.
		LogUtils.log("waiting for machine restart to be detected by the remote admin.");
		waitForServiceInstances(0);
		
		// wait for machine to restart and start the agent again.
		LogUtils.log("waiting for agent to recover.");
		waitForServiceInstances(1);
		
		// assert esm is not starting new machines.
		LogUtils.log("asserting cluster is stabilized. waiting for " + DEFAULT_TEST_TIMEOUT 
				+ " millis to see no new agent has been created");
		assertClusterStabilized();
	}
	
	private void assertClusterStabilized() {
    	RepetitiveConditionProvider condition = new RepetitiveConditionProvider() {
			
			@Override
			public boolean getCondition() {
				try {
					@SuppressWarnings("unchecked")
					final List<String> agents = (List<String>) oldClient.getAdmin("GridServiceAgents").get("Agents-Elements");
					return agents.size() == 2;
				} catch (RestException e) {
					LogUtils.log("error occured while polling for service state. " + e.getMessage());
					e.printStackTrace();
				}
				return false;
			}
		};
	   	AssertUtils.repetitiveAssertTrue("ESM started another agent, maintenance mode restart failed.",
    			condition,
    			DEFAULT_TEST_TIMEOUT,
    			5000);
	}
    
    private void waitForServiceInstances(final int numInstances) {
    	RepetitiveConditionProvider provider = new RepetitiveConditionProvider() {
			
			@Override
			public boolean getCondition() {
				try {
					return getNumOfServiceInstances() == numInstances;
				} catch (RestClientException e) {
					LogUtils.log("error occured while polling for service state. " + e.getMessage());
					e.printStackTrace();
				}
				return false;
			}
		};
    	AssertUtils.repetitiveAssertTrue("service did not reach the expected instance count of " + numInstances,
    			provider,
    			DEFAULT_TEST_TIMEOUT,
    			5000);
	}

	private int getNumOfServiceInstances() throws RestClientException {
		final ApplicationDescription description = this.newClient.getApplicationDescription(APP_NAME);
    	return description.getServicesDescription().get(0).getInstanceCount();
	}
	
	@Override
	@AfterClass(alwaysRun = true)
	public void teardown() throws Exception {
		uninstallServiceIfFound(SERVICE_NAME);
		super.teardown();
	}
	
	private void startMaintenanceMode(final long timeoutInSeconds) throws IOException, InterruptedException {
    	CommandTestUtils.runCommand("connect " + this.getRestUrl() + ";" 
    			+ " invoke simpleRestartAgent startMaintenanceMode " + timeoutInSeconds);
	}
    
    private String getServicePath(final String serviceName) {
    	return CommandTestUtils.getPath("src/main/resources/apps/USM/usm/" + serviceName);
    }
    
	private void restartWinMachine() throws IOException, InterruptedException {
    	final String connectCommand 		= "connect " + this.getRestUrl();
    	final String shutdownCommand 	= connectCommand + ";invoke simpleRestartAgent restartWindows";
    	String shutdownOut = CommandTestUtils.runCommandAndWait(shutdownCommand);
    	assertTrue(shutdownOut.contains("invocation completed successfully."));
    }
}
