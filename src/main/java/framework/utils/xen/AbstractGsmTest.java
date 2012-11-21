package framework.utils.xen;


import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.openspaces.admin.Admin;
import org.openspaces.admin.esm.ElasticServiceManager;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsa.events.GridServiceAgentAddedEventListener;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.lus.LookupService;
import org.openspaces.admin.machine.Machine;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import framework.utils.AdminUtils;
import framework.utils.GridServiceAgentsCounter;
import framework.utils.GridServiceContainersCounter;

import test.AbstractTest;


/**
 * Abstract test base class for elastic processing units running on SGTest agents 
 * (without machine virtualization)
 * 
 * @author giladh
 *
 */
public class AbstractGsmTest extends AbstractTest {

    private ElasticServiceManager esm;
	
	private GridServiceContainersCounter gscCounter;
	private GridServiceAgentsCounter gsaCounter;

    protected GridServiceAgent gsa;
    
    protected GridServiceManager gsm;
    
    
    @Override 
    @BeforeMethod
    public void beforeTest() {
    	super.beforeTest();
    	    	
    	LookupService[] lookupServices =  restartLus(); // workaround for admin.close() container discovery leakage from previous test
    	gsa = lookupServices[0].getGridServiceAgent();
    	assertEquals("All containers must be terminated before test starts",false,admin.getGridServiceContainers().waitFor(5,1,TimeUnit.SECONDS));
    	gscCounter = new GridServiceContainersCounter(admin);
    	gsaCounter = new GridServiceAgentsCounter(admin);
    	gsm = AdminUtils.loadGSM(gsa);    	
    	esm = AdminUtils.loadESM(gsa);
    	
    	assertEquals(0, gscCounter.getNumberOfGSCsAdded());
    	
    }

	private LookupService[] restartLus() {
		admin.getLookupServices().waitFor(2);
		LookupService[] oldLookupServices = admin.getLookupServices().getLookupServices();
		assertEquals(2, oldLookupServices.length);
    	for (int i = 0 ; i < oldLookupServices.length ; i++) {
    		LookupService oldLus = oldLookupServices[i];
    		waitForAgentOnMachine(admin, oldLus.getMachine());
    		if (oldLus.getGridServiceAgent() == null) {
    		    throw new IllegalStateException("LookupService is expected to be managed by an Agent");
    		}
    		GsmTestUtils.restartLookupService(oldLus);
    	}
    	admin.close();
    	admin = newAdmin();
    	admin.getLookupServices().waitFor(2);
    	LookupService[] newLookupServices =  admin.getLookupServices().getLookupServices();
    	assertEquals("Expected 2 lookup services",2, newLookupServices.length);
    	for (int i = 0 ; i < newLookupServices.length ; i++) {
    		LookupService newLus = newLookupServices[i];
    		waitForAgentOnMachine(admin, newLus.getMachine());
    		if (newLus.getGridServiceAgent() == null) {
    		    throw new IllegalStateException("LookupService is expected to be managed by an Agent");
    		}
    	}
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
    
    @Override
    @AfterMethod
    public void afterTest() throws Exception {
        gscCounter.close();
        gsaCounter.close();
       	super.afterTest();
    }

    public void repetitiveAssertNumberOfGSAsAdded(int expected, long timeoutMilliseconds) {
    	gsaCounter.repetitiveAssertNumberOfGridServiceAgentsAdded(expected, timeoutMilliseconds);
    }
    
    public void repetitiveAssertNumberOfGSAsRemoved(int expected, long timeoutMilliseconds) {
    	gsaCounter.repetitiveAssertNumberOfGridServiceAgentsRemoved(expected, timeoutMilliseconds);
    }
    
    public void repetitiveAssertNumberOfGSCsAdded(int expected, long timeoutMilliseconds) {
    	gscCounter.repetitiveAssertNumberOfGridServiceContainersAdded(expected, timeoutMilliseconds);
    }
    
    public void repetitiveAssertNumberOfGSCsRemoved(int expected, long timeoutMilliseconds) {
    	gscCounter.repetitiveAssertNumberOfGridServiceContainersRemoved(expected, timeoutMilliseconds);
    }
    
    
}
