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
import org.openspaces.admin.space.ReplicationStatus;
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
 * GS-8231: Disconnected replication status is not triggered when backup becomes primary after failover.
 * 
 * @author moran
 */
public class ReplicationStatusChangedEventTest extends AbstractTest {

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
    	//deploy
    	log("deploy and wait for pu [foo 1,1] to instantiate");
        ProcessingUnit foo = gsm.deploy(new SpaceDeployment("foo").partitioned(1, 1).maxInstancesPerVM(1));
        foo.waitFor(foo.getTotalNumberOfInstances());

        //extract primary and backup space instances
        ProcessingUnitUtils.waitForActiveElection(foo);
        SpaceInstance tmpPrimarySpaceInstance = null;
        SpaceInstance tmpBackupSpaceInstance = null;
        for (SpaceInstance spaceInstance : foo.getSpace()) {
            if (spaceInstance.getMode() == SpaceMode.PRIMARY) {
            	tmpPrimarySpaceInstance = spaceInstance;
            } else if (spaceInstance.getMode() == SpaceMode.BACKUP) {
            	tmpBackupSpaceInstance = spaceInstance;
            }
        }
        Assert.assertNotNull(tmpPrimarySpaceInstance, "Expected non-null primary instance");
        Assert.assertNotNull(tmpBackupSpaceInstance, "Expected non-null backup instance");
        final SpaceInstance primarySpaceInstance = tmpPrimarySpaceInstance;
        final SpaceInstance backupSpaceInstance = tmpBackupSpaceInstance;
        
        //find out on which GSC this instance is on
        GridServiceContainer gscWithPrimary = primarySpaceInstance.getVirtualMachine().getGridServiceContainer();
        
        final CountDownLatch latchBeforeKill = new CountDownLatch(1);
        final CountDownLatch latchAfterKill = new CountDownLatch(1);
        final CountDownLatch latchAfterNewGsc = new CountDownLatch(1);
        final AtomicInteger totalEvents = new AtomicInteger();
        admin.getSpaces().getReplicationStatusChanged().add(new ReplicationStatusChangedEventListener() {
			public void replicationStatusChanged(ReplicationStatusChangedEvent event) {
				totalEvents.incrementAndGet();
				log("Replication " + ToStringUtils.spaceInstanceToString(event.getSpaceInstance()) 
						+ " prev.status: " + event.getPreviousStatus() +  " new.status: " + event.getNewStatus());

				if (event.getSpaceInstance().equals(
						primarySpaceInstance)
						&& event.getPreviousStatus() == null
						&& event.getNewStatus() == ReplicationStatus.ACTIVE) {
					//foo1 --> foo1_1 : replication established between primary and backup
					latchBeforeKill.countDown();
				} else if (event.getSpaceInstance().equals(backupSpaceInstance)
							&& event.getPreviousStatus() == null
							&& event.getNewStatus() == ReplicationStatus.DISCONNECTED) {
						//foo1_1 -x-> foo1 : replication disconnected after primary killed
						latchAfterKill.countDown();
				} else if (event.getSpaceInstance().equals(backupSpaceInstance)
						&& event.getPreviousStatus() == ReplicationStatus.DISCONNECTED
						&& event.getNewStatus() == ReplicationStatus.ACTIVE
						&& event.getReplicationTarget().getSpaceInstance() != primarySpaceInstance) {
						//foo1_1 ---> foo1 : replication established between primary and backup
						latchAfterNewGsc.countDown();
				}
				
			}
		});
        
        log("waiting for event: foo1 --> foo1_1 : replication established between primary and backup");
        latchBeforeKill.await();
        
        log("killing GSC with primary space");
        gscWithPrimary.kill();
        
        log("waiting for event: foo1_1 -x-> foo1 : replication disconnected after primary killed");
        latchAfterKill.await();
        
        log("loading new GSC");
        loadGSC(machine);
        log("waiting for event: foo1_1 ---> foo1 : replication established between primary and backup");
        latchAfterNewGsc.await();
        
        Assert.assertEquals(totalEvents.get(), 3, "Expected only 3 events");
    }
}
