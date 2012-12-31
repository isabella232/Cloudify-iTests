package test.gsm.failover.xen;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.space.Space;
import org.openspaces.admin.space.SpaceDeployment;
import org.openspaces.core.GigaSpace;
import org.openspaces.core.executor.Task;
import org.openspaces.grid.gsm.machines.MachinesSlaEnforcementEndpoint;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.gsm.AbstractXenGSMTest;

import com.gigaspaces.async.AsyncFuture;

import framework.utils.AdminUtils;
import framework.utils.AssertUtils;
import framework.utils.LogUtils;

public class FailoverDuringExecutorXenTest extends AbstractXenGSMTest {

    private static final String PU_NAME = "testspace";
	protected final static long CONTAINER_MEGABYTES = 250;
	protected static final long FOREVER_MILLIS = DEFAULT_TEST_TIMEOUT*10;
	// more than find active member timeout ~20 secs and watchdog 30 secs
	private static final long EXEC_OPERATION_TIMEOUT = 2*60*1000; 
	private static final int SHUTDOWN_DELAY_MILLIS = 5*1000;
    protected MachinesSlaEnforcementEndpoint endpoint;
    protected ProcessingUnit pu;
    GridServiceAgent gsa2;
    
    @BeforeMethod
    public void beforeTest() {
        super.beforeTest();
        pu = super.deploy(new SpaceDeployment(PU_NAME).partitioned(1,0));
    }

    @Test(timeOut=DEFAULT_TEST_TIMEOUT, invocationCount=5, enabled=  true)
    public void test() throws InterruptedException, ExecutionException, TimeoutException {
    	
    	// the first GSAs is already started in BeginTest
        GridServiceAgent[] agents = admin.getGridServiceAgents().getAgents();
		Assert.assertEquals(agents.length,1);
		gsa2 = super.startNewVM(OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
        AdminUtils.loadGSC(gsa2);
        Assert.assertTrue(pu.waitFor(pu.getTotalNumberOfInstances(), OPERATION_TIMEOUT, TimeUnit.MILLISECONDS));
        
        Space space = pu.waitForSpace(OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
        Assert.assertNotNull(space);
        GigaSpace gigaSpace = space.getGigaSpace();
        
        //warm up lrmi socket to space
        gigaSpace.count(null);
        
        int routing = 0;
		AsyncFuture<Integer> future = gigaSpace.execute(new ExecuteForeverTask(),routing);
		
		new ShutdownMachineDelayedTask().run();
		
		// wait for task execution to finish
		// even if tcp keepalive is enabled it would only mark the tcp socket, but will not trigger a disconnect exception
		// therefore requires lrmi based leep alive, however this test seems to pass even with only
		//
		// If this future happens to throw TimeoutException check that the SGTest machine has the TCP keepalive settings
		// defined here http://wiki.gigaspaces.com/wiki/display/XAP9/Tuning+Infrastructure
		//
		try {
			future.get(EXEC_OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
			Assert.fail("expected exception");
		}
		catch (TimeoutException e) {
			String ipaddress;
			try {
				ipaddress = InetAddress.getLocalHost().getHostAddress();
			} catch (UnknownHostException e1) {
				ipaddress = "unknown"; 
			}
			Assert.fail("Check that SGTest machine " + ipaddress + " has correct TCP keepalive settings as defined here http://wiki.gigaspaces.com/wiki/display/XAP9/Tuning+Infrastructure",e);
		}
		catch (ExecutionException e) {
			Throwable cause = e.getCause();
			Assert.assertEquals(RemoteException.class, cause.getClass());
		}
    	pu.undeployAndWait();
    }  
    
    static class ExecuteForeverTask implements Task<Integer> {

		private static final long serialVersionUID = 1L;

		@Override
		public Integer execute() throws Exception {
			Thread.sleep(FOREVER_MILLIS);
			return 0;
		}
    }
    
    class ShutdownMachineDelayedTask extends Thread {

		public void run() {
            try {
				AssertUtils.sleep(SHUTDOWN_DELAY_MILLIS);
				shutdownMachine(gsa2.getMachine(), FailoverDuringExecutorXenTest.this.getMachineProvisioningConfig(), OPERATION_TIMEOUT);
			} catch (InterruptedException e) {
				LogUtils.log("Unexpected Exception",e);
			}
        }
    }
}
