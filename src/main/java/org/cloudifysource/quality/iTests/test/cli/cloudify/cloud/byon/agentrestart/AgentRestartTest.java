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
	
	@BeforeClass(alwaysRun = true)
	protected void bootstrap() throws Exception {
		super.bootstrap();
	}
	
    @Test(timeOut = DEFAULT_TEST_TIMEOUT * 4, enabled = true)
    public void testAgentRestart() throws Exception {
        installServiceAndWait(getServicePath(SERVICE_NAME), SERVICE_NAME);
        final String absolutePuName = ServiceUtils.getAbsolutePUName(APP_NAME, SERVICE_NAME);
        
		final CountDownLatch removed = new CountDownLatch(1);
		//TODO: the latch should be 1.
		final CountDownLatch added = new CountDownLatch(2);
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
		
		LogUtils.log("Starting maintenance mode for pu with name " + absolutePuName);
		//set maintenance mode for a long time.
		startMaintenanceMode(10000);
		
		restartAgentMachine(absolutePuName);
		
		assertTrue(removed.await(DEFAULT_WAIT_MINUTES, TimeUnit.MINUTES));
		assertTrue(added.await(DEFAULT_WAIT_MINUTES, TimeUnit.MINUTES));
		
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
