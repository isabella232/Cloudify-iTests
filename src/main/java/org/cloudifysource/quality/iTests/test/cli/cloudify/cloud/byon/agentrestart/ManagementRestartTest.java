package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.byon.agentrestart;

import iTests.framework.utils.LogUtils;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsa.events.GridServiceAgentAddedEventListener;
import org.openspaces.admin.gsa.events.GridServiceAgentLifecycleEventListener;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * 
 * @author adaml
 *
 */
public class ManagementRestartTest extends AbstractAgentMaintenanceModeTest {
    
	
	private static final String REST_PU_NAME = "rest";

	@BeforeClass(alwaysRun = true)
	protected void bootstrap() throws Exception {
		super.bootstrap();
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 4, enabled = true)
    public void testManagementRestart() throws Exception {
        installServiceAndWait(getServicePath(SERVICE_NAME), SERVICE_NAME);

        //These latches describe a proper esm behaviur. 
        final CountDownLatch removed = new CountDownLatch(1);
		final CountDownLatch added = new CountDownLatch(4);
		
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
		
		restartAgentMachine(REST_PU_NAME);
		
		LogUtils.log("Waiting for management machine to recover from restart.");
		
		assertTrue("agent machine did not stop as expected.", removed.await(DEFAULT_WAIT_MINUTES, TimeUnit.MINUTES));
		assertTrue("agent machine was not added as expected.",added.await(DEFAULT_WAIT_MINUTES, TimeUnit.MINUTES));
		
		
		LogUtils.log("verifying cluster is stable.");
		assertTrue("detected agent removed after cluster was stabilized.", !removedAfterStabilized.await(5l, TimeUnit.MINUTES));
		assertTrue("detected agent added after cluster was stabilized.", addedAfterStabilized.getCount() == 1);
		
		assertNumberOfMachines(3);
		
		LogUtils.log("ESM has recovered successfully. uninstalling service " + SERVICE_NAME);
		uninstallServiceAndWait(SERVICE_NAME);
    }
	
	@Override
	protected void customizeCloud() throws Exception {
		getService().setNumberOfManagementMachines(2);
	}
	
	@Override
	@AfterClass(alwaysRun = true)
	protected void teardown() throws Exception {
		uninstallServiceIfFound(SERVICE_NAME);
		super.teardown();
	}
}
