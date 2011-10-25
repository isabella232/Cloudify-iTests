package test.servicegrid.events;

import static test.utils.AdminUtils.loadGSC;
import static test.utils.AdminUtils.loadGSM;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.openspaces.admin.AdminEventListener;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.gsc.events.GridServiceContainerLifecycleEventListener;
import org.testng.Assert;
import org.testng.annotations.Test;

import test.AbstractTest;
import test.utils.TestUtils;
/**
 * @author rafi
 *
 */
public class MiscEventsRemoveTest extends AbstractTest{

    @Test(timeOut = DEFAULT_TEST_TIMEOUT)
    public void test() throws InterruptedException{
	GridServiceAgent gsa = admin.getGridServiceAgents().waitForAtLeastOne();
	loadGSM(gsa);
	final CountDownLatch gscAddedLatch = new CountDownLatch(2);
	AdminEventListener gscLifeCycleListener = new GridServiceContainerLifecycleEventListener(){

	    public void gridServiceContainerAdded(
		    GridServiceContainer gridServiceContainer) {
		gscAddedLatch.countDown();
	    }

	    public void gridServiceContainerRemoved(
		    GridServiceContainer gridServiceContainer) {
	    }
	    
	};
	
	admin.addEventListener(gscLifeCycleListener);
	
	loadGSC(gsa);
	TestUtils.repetitive(new Runnable() {
	    
	    public void run() {
		Assert.assertEquals(1,gscAddedLatch.getCount());
	    }
	},3000);
	
	admin.removeEventListener(gscLifeCycleListener);
	loadGSC(gsa);
	
	boolean result =gscAddedLatch.await(10, TimeUnit.SECONDS);
	Assert.assertEquals(result, false);
    }
}
