package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.byon.agentrestart;

import iTests.framework.utils.LogUtils;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsa.events.GridServiceAgentAddedEventListener;
import org.openspaces.admin.gsa.events.GridServiceAgentLifecycleEventListener;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Sets agent to maintenance mode for a very short period of time and restarts the agent machine.
 * The expected behavior would be that the esm will detect the maintenance mode has expired
 * during the restart and start a new agent. When reboot is over, a new agent will be started 
 * and then removed by the esm for it is no longer needed. 
 *  
 * @author adaml
 *
 */
class AgentMaintenanceTimeoutExceeded extends AbstractAgentMaintenanceModeTest {

	private static final long FIVE_SECONDS_MILLIS = 5000;

	@BeforeClass(alwaysRun = true)
	protected void bootstrap() throws Exception {
		try {
			
			super.bootstrap();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * set
	 * @throws Exception
	 */
    @Test(timeOut = DEFAULT_TEST_TIMEOUT * 4, enabled = true)
    public void testAgentRestartWithMaintenanceTimeoutExceeded() throws Exception {
    	installServiceAndWait(getServicePath(SERVICE_NAME), SERVICE_NAME);
    	
    	//set maintenance mode for 1 second using the service context.
    	LogUtils.log("Setting maintenance mode for a very short period");
    	startMaintenanceMode(1);
    	LogUtils.log("Waiting for maintenance mode to expire");
    	Thread.sleep(FIVE_SECONDS_MILLIS);
    	LogUtils.log("maintenance mode expired");
    	
		final CountDownLatch removed = new CountDownLatch(1);
		final CountDownLatch added = new CountDownLatch(2);
		
        final GridServiceAgentAddedEventListener agentListener = new GridServiceAgentLifecycleEventListener() {
			
			@Override
			public void gridServiceAgentRemoved(GridServiceAgent gridServiceAgent) {
				LogUtils.log("agent removed event has been fired");
				removed.countDown();
			}
			
			@Override
			public void gridServiceAgentAdded(GridServiceAgent gridServiceAgent) {
				LogUtils.log("agent added event has been fired");
				added.countDown();
			}
		};
				
		admin.addEventListener(agentListener);
    	
		//Shutdown agent. This machine should not start again.
		gracefullyShutdownAgent(absolutePuName);
    	
		assertTrue("agent machine did not stop as expected.", removed.await(DEFAULT_WAIT_MINUTES, TimeUnit.MINUTES));
		assertTrue("agent machine was not added as expected. esm did not start a machine",added.await(DEFAULT_WAIT_MINUTES, TimeUnit.MINUTES));
		
		uninstallServiceAndWait(absolutePuName);
    }
}