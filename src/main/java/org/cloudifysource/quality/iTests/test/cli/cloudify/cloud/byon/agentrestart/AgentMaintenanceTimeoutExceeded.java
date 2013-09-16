package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.byon.agentrestart;

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

	@BeforeClass(alwaysRun = true)
	protected void bootstrap() throws Exception {
		super.bootstrap();
	}
	
	/**
	 * set
	 * @throws Exception
	 */
    @Test(timeOut = DEFAULT_TEST_TIMEOUT * 4, enabled = true)
    public void testAgentRestartWithMaintenanceTimeoutExceeded() throws Exception {
    	installServiceAndWait(getServicePath(SERVICE_NAME), SERVICE_NAME);
    	
    	//set maintenance mode for 1 second using the service context.
    	startMaintenanceMode(1);
    	
		final CountDownLatch removed = new CountDownLatch(1);
		final CountDownLatch added = new CountDownLatch(1);
        final GridServiceAgentAddedEventListener agentListener = new GridServiceAgentLifecycleEventListener() {
			
			@Override
			public void gridServiceAgentRemoved(GridServiceAgent gridServiceAgent) {
				removed.countDown();
			}
			
			@Override
			public void gridServiceAgentAdded(GridServiceAgent gridServiceAgent) {
				added.countDown();
			}
		};
				
		admin.addEventListener(agentListener);
    	
		gracefullyShutdownAgent(absolutePuName);
    	
		assertTrue(removed.await(DEFAULT_WAIT_MINUTES, TimeUnit.MINUTES));
		assertTrue(added.await(DEFAULT_WAIT_MINUTES, TimeUnit.MINUTES));
//		assertNewMachineIpNotSameAsOldMachineIp();
		
		assertNumberOfMachines(2);
		
		uninstallServiceAndWait(absolutePuName);
    }
}