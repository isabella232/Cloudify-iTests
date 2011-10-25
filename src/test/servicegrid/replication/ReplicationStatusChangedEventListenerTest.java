package test.servicegrid.replication;

import static test.utils.AdminUtils.loadGSC;
import static test.utils.AdminUtils.loadGSM;
import static test.utils.LogUtils.log;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.space.SpaceDeployment;
import org.openspaces.admin.space.SpaceInstance;
import org.openspaces.admin.space.events.ReplicationStatusChangedEvent;
import org.openspaces.admin.space.events.ReplicationStatusChangedEventListener;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.AbstractTest;
import test.utils.ProcessingUnitUtils;
import test.utils.ToStringUtils;

import com.gigaspaces.cluster.activeelection.SpaceMode;

/**
 * Topology: 1 machine, 1 GSM, 2 GSCs 
 * 
 * GS-8026: Method replicationStatusChanged() of ReplicationStatusChangedEventListener is not called
 * after adding event listener to already existing Admin object.
 * 
 * Tests that listeners receive all the events in two cases:
 * 1. when adding a listener
 * 2. when a change occurs
 * 
 * @author moran
 */
public class ReplicationStatusChangedEventListenerTest extends AbstractTest {

    private Machine machine;
    private GridServiceManager gsm;

    @BeforeMethod
    public void setup() {
        GridServiceAgent gsa = admin.getGridServiceAgents().waitForAtLeastOne();
        machine = gsa.getMachine();
        gsm = loadGSM(machine);
        loadGSC(machine);
        loadGSC(machine);
    }
    
    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1")
    public void test() throws Exception {
        ProcessingUnit foo = gsm.deploy(new SpaceDeployment("foo").partitioned(1, 1).maxInstancesPerVM(1));
        log("wait for " + foo.getName() + " to instantiate");
        foo.waitFor(foo.getTotalNumberOfInstances());

        //extract backup space instance
        ProcessingUnitUtils.waitForActiveElection(foo);
        SpaceInstance backupSpaceInstance = null;
        for (SpaceInstance spaceInstance : foo.getSpace()) {
            if (spaceInstance.getMode() == SpaceMode.BACKUP) {
            	backupSpaceInstance = spaceInstance;
            }
        }
        Assert.assertNotNull(backupSpaceInstance, "Expected to find a backup instance");
        
        
        final AtomicInteger numOfEvents = new AtomicInteger();
        final CountDownLatch latch1 = new CountDownLatch(1);
        final CountDownLatch latch2 = new CountDownLatch(1);
        final CountDownLatch latch3 = new CountDownLatch(2);
        
        //should receive 2 events - one for active and one for disconnected
        admin.getSpaces().getReplicationStatusChanged().add(new ReplicationStatusChangedEventListener() {
			public void replicationStatusChanged(ReplicationStatusChangedEvent event) {
				log("listener1 - Replication " + ToStringUtils.spaceInstanceToString(event.getSpaceInstance()) 
						+ " prev.status: " + event.getPreviousStatus() +  " new.status: " + event.getNewStatus());
				numOfEvents.incrementAndGet();
				
				if (latch1.getCount() != 0) {
					//foo1 --> foo1_1 ACTIVE
					latch1.countDown();
				} else {
					//foo1 -x-> foo1_1 DISCONNECTED
					latch3.countDown();
				}
			}
        });
        
        log("wait for 1 event to arrive at listener 1");
        latch1.await();
        
        //should receive 2 events - one for active and one for disconnected
        admin.getSpaces().getReplicationStatusChanged().add(new ReplicationStatusChangedEventListener() {
			public void replicationStatusChanged(ReplicationStatusChangedEvent event) {
				log("listener2 - Replication " + ToStringUtils.spaceInstanceToString(event.getSpaceInstance()) 
						+ " prev.status: " + event.getPreviousStatus() +  " new.status: " + event.getNewStatus());
				numOfEvents.incrementAndGet();
				
				if (latch2.getCount() != 0) {
					//foo1 --> foo1_1 ACTIVE
					latch2.countDown();
				} else {
					//foo1 -x-> foo1_1 DISCONNECTED
					latch3.countDown();
				}
			}
        });
        
        log("wait for 1 event to arrive at listener 2");
        latch2.await();
        
        log("kill GSC with backup Space, and wait for events to arrive");
        GridServiceContainer gsc = backupSpaceInstance.getVirtualMachine().getGridServiceContainer();
        gsc.kill();
        latch3.await();
        
        Assert.assertEquals(numOfEvents.get(), 4, "Expected only a total of 4 events to arrive");
    }
}
