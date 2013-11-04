package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.ec2win.agentrestart;

import iTests.framework.utils.AssertUtils;
import iTests.framework.utils.AssertUtils.RepetitiveConditionProvider;
import iTests.framework.utils.LogUtils;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.cloudifysource.dsl.rest.response.ApplicationDescription;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.byon.agentrestart.AbstractAgentMaintenanceModeTest;
import org.cloudifysource.restclient.GSRestClient;
import org.cloudifysource.restclient.RestClient;
import org.cloudifysource.restclient.RestException;
import org.cloudifysource.restclient.exceptions.RestClientException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.j_spaces.kernel.PlatformVersion;

public class AgentRestartOnWinTest extends AbstractAgentMaintenanceModeTest {

	
	private static final int INSTALL_SERVICE_TIMEOUT_MIN = 30;
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
	protected void teardown() throws Exception {
		uninstallServiceIfFound(SERVICE_NAME);
		super.teardown();
	}
}
