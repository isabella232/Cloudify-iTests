package test.servicegrid.failover;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.openspaces.admin.Admin;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsa.events.GridServiceAgentAddedEventListener;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.lus.LookupService;
import org.openspaces.admin.machine.Machine;
import org.testng.Assert;
import org.testng.annotations.Test;

import test.AbstractTest;
import test.gsm.GsmTestUtils;
import test.utils.AdminUtils;

public class TwoLookupServicesFailoverTest extends AbstractTest {
    
	
    private static final String ZONE = "zone";

	@Override
    protected Admin newAdmin() {
        return AdminUtils.createSingleThreadAdmin();
    }
    /**
     * GS-9614
     */
    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = false)
	public void lookupRestartTest() {
    	LookupService[] lookupServices =  restartLus();
      	GridServiceAgent gsa = lookupServices[0].getGridServiceAgent();
      	assertEquals("All containers must be terminated before test starts",false,admin.getGridServiceContainers().waitFor(5,1,TimeUnit.SECONDS));
      
		Machine machine = gsa.getMachine();
			    
	    //deploy pu partitioned(2,1) on 4 GSCs
	    Assert.assertEquals(admin.getGridServiceContainers().getSize(), 0);
	    GridServiceContainer[] containers = AdminUtils.loadGSCs(gsa, 4, ZONE);
	    Assert.assertEquals(containers.length, 4);
	    admin.getGridServiceContainers().waitFor(4);
	    assertTrue("machine.getGridServiceContainers() references an old object",machine.getGridServiceContainers().waitFor(4,30, TimeUnit.SECONDS));
	}
    
    private LookupService[] restartLus() {
		admin.getLookupServices().waitFor(2);
		LookupService[] oldLookupServices = admin.getLookupServices().getLookupServices();
		LookupService[] newLookupServices = new LookupService[2]; 
		assertEquals(2, oldLookupServices.length);
    	for (int i = 0 ; i < oldLookupServices.length ; i++) {
			LookupService oldLus = oldLookupServices[i];
    		waitForAgentOnMachine(admin, oldLus.getMachine());
    		if (oldLus.getGridServiceAgent() == null) {
    		    throw new IllegalStateException("LookupService is expected to be managed by an Agent");
    		}
    		newLookupServices[i] = GsmTestUtils.restartLookupService(oldLus);
    	}
    	//uncomment the following lines in order for the test to pass
    	//this restarts the admin view of the world
    	//admin.close();
    	//admin = newAdmin();
    	//admin.getLookupServices().waitFor(2);
    	//LookupService[] newLookupServices =  admin.getLookupServices().getLookupServices();
    	//assertEquals("Expected 2 lookup services",2, newLookupServices.length);
    	return newLookupServices;
	}
    
    private static GridServiceAgent waitForAgentOnMachine(final Admin admin, final Machine machine) {
		final AtomicReference<GridServiceAgent> gsaRef = new AtomicReference<GridServiceAgent>();
		final CountDownLatch latch = new CountDownLatch(1);
    	final GridServiceAgentAddedEventListener eventListener = new GridServiceAgentAddedEventListener() {

			public void gridServiceAgentAdded(final GridServiceAgent gridServiceAgent) {
				if (gridServiceAgent.getMachine().equals(machine)) {
					gsaRef.set(gridServiceAgent);
					latch.countDown();
				}				
			}};
		admin.getGridServiceAgents().getGridServiceAgentAdded().add(eventListener);
		try {
			latch.await();
		} catch (final InterruptedException e) {
			Thread.interrupted();
			Assert.fail("Interrupted while waitForAgentOnMachine", e);
		}
		finally {
			admin.getGridServiceAgents().getGridServiceAgentAdded().remove(eventListener);
		}
		return gsaRef.get();
	}
}
