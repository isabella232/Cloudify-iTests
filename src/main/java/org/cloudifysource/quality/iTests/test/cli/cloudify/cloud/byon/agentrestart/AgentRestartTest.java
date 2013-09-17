package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.byon.agentrestart;

import iTests.framework.utils.LogUtils;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.cloudifysource.dsl.utils.ServiceUtils;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsa.events.GridServiceAgentAddedEventListener;
import org.openspaces.admin.gsa.events.GridServiceAgentLifecycleEventListener;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class AgentRestartTest extends AbstractAgentMaintenanceModeTest {
	
	private static final int INFINITY_MINUTES = 600;

	@BeforeClass(alwaysRun = true)
	protected void bootstrap() throws Exception {
		super.bootstrap();
	}
	
    @Test(timeOut = DEFAULT_TEST_TIMEOUT * 4, enabled = true)
    public void testAgentRestart() throws Exception {
        installServiceAndWait(getServicePath(SERVICE_NAME), SERVICE_NAME);
        final String absolutePuName = ServiceUtils.getAbsolutePUName(APP_NAME, SERVICE_NAME);
        

        //These latches describe a proper esm behaviur. 
        final CountDownLatch removed = new CountDownLatch(1);
		final CountDownLatch added = new CountDownLatch(3);
		
		final CountDownLatch removedAfterStabilized = new CountDownLatch((int) removed.getCount() + 1);
		final CountDownLatch addedAfterStabilized = new CountDownLatch((int) added.getCount() + 1);
        final GridServiceAgentAddedEventListener agentListener = new GridServiceAgentLifecycleEventListener() {
			
			@Override
			public void gridServiceAgentRemoved(GridServiceAgent gridServiceAgent) {
				LogUtils.log("agent removed event has been fired");
				removed.countDown();
				removedAfterStabilized.countDown();
			}
			
			@Override
			public void gridServiceAgentAdded(GridServiceAgent gridServiceAgent) {
				LogUtils.log("agent added event has been fired");
				added.countDown();
				addedAfterStabilized.countDown();
			}
		};
		
		admin.addEventListener(agentListener);
		LogUtils.log("Starting maintenance mode for pu with name " + absolutePuName);
		//set maintenance mode for a long time.
		startMaintenanceMode(TimeUnit.MINUTES.toMillis(INFINITY_MINUTES));
		
		restartAgentMachine(absolutePuName);
		
		assertTrue("agent machine did not stop as expected.", removed.await(DEFAULT_WAIT_MINUTES, TimeUnit.MINUTES));
		assertTrue("agent machine was not added as expected.",added.await(DEFAULT_WAIT_MINUTES, TimeUnit.MINUTES));
		
		assertTrue("detected agent removed after cluster was stabilized.", !removedAfterStabilized.await((long) 5, TimeUnit.MINUTES));
		assertTrue("detected agent added after cluster was stabilized.", addedAfterStabilized.getCount() == 1);
		
		assertNumberOfMachines(2);
		
		stopMaintenanceMode(absolutePuName);
		
		uninstallServiceAndWait(SERVICE_NAME);
    }
    
	@Override
	protected void customizeCloud() throws Exception {
		super.customizeCloud();
		getService().setSudo(false);
		getService().getProperties().put("keyFile", "testKey.pem");
	}
}
