package test.servicegrid.replication;

import static test.utils.LogUtils.log;

import java.util.concurrent.CountDownLatch;

import org.openspaces.admin.Admin;
import org.openspaces.admin.AdminFactory;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.internal.space.InternalSpaceInstance;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.openspaces.admin.space.ReplicationTarget;
import org.openspaces.admin.space.Space;
import org.openspaces.admin.space.SpaceDeployment;
import org.openspaces.admin.space.SpaceInstance;
import org.openspaces.admin.space.events.ReplicationStatusChangedEvent;
import org.openspaces.admin.space.events.ReplicationStatusChangedEventListener;
import org.openspaces.admin.space.events.ReplicationStatusChangedEventManager;
import org.testng.Assert;
import org.testng.annotations.Test;

import test.AbstractTest;
import test.utils.AdminUtils;

public class GetReplicationTargetInstanceTest extends AbstractTest{

	@Test(timeOut = DEFAULT_TEST_TIMEOUT)
	public void test() throws InterruptedException{
		
		Admin admin = new AdminFactory().userDetails("xxx", "xxx").discoverUnmanagedSpaces().createAdmin();

	    assertTrue(admin.getMachines().waitFor(1));
	    assertTrue(admin.getGridServiceAgents().waitFor(1));
	    
	    GridServiceAgent gsa = admin.getGridServiceAgents().waitForAtLeastOne();
		
	    Machine machine = gsa.getMachine();
		log(machine.getHostName() + " " + machine.getOperatingSystem().getDetails().getAvailableProcessors()); 
		
		GridServiceManager gsm = AdminUtils.loadGSM(machine);
		AdminUtils.loadGSC(machine);
		
		ProcessingUnit pu = gsm.deploy(new SpaceDeployment("A").clusterSchema("sync_replicated").numberOfInstances(2));
		pu.waitFor(pu.getTotalNumberOfInstances());
		for (ProcessingUnitInstance pui : pu.getInstances()){
			pui.waitForSpaceInstance();
		}
		
		final Space spaceA = pu.getSpace();
		spaceA.waitFor(spaceA.getTotalNumberOfInstances());
		
		final CountDownLatch latch = new CountDownLatch(2);
		
		ReplicationStatusChangedEventManager repManager = spaceA.getReplicationStatusChanged(); 
		repManager.add(new ReplicationStatusChangedEventListener(){			
			public void replicationStatusChanged(
					ReplicationStatusChangedEvent arg0) {
				try{ 
					SpaceInstance spaceInstance = arg0.getSpaceInstance();
					assertTrue("ReplicationStatusChangedEvent argument's spaceInstance is null", spaceInstance != null);
					log("SpaceInstance [" + spaceInstance + "], replication target["+ spaceInstance.getSpaceUrl() + "], replication target[" + spaceInstance.getReplicationTargets() + "] previousStatus[" + arg0.getPreviousStatus() + "], [" +arg0.getNewStatus() + "]"); 
					log("This space instance " + spaceInstance); 
					
					
					InternalSpaceInstance replicationTargetSpaceInstace = arg0.getReplicationTarget().getSpaceInstance();
					assertTrue("replicationTarget.getSpaceInstance() is null", replicationTargetSpaceInstace != null);
					log("Replication target space instance " + replicationTargetSpaceInstace); 
					
					ReplicationTarget[] targets =spaceInstance.getReplicationTargets(); 
					assertTrue("spaceInstance.getReplicationTargets() is null", targets != null);
					
					for(ReplicationTarget target: targets){ 
						log("ReplicationTarget: "+target);
						log("target.getSpaceInstance(): "+target.getSpaceInstance().toString()); 
						log("target.getReplicationStatus() "+target.getReplicationStatus().toString()); 
					} 
				}catch(Throwable ex){ 
					Assert.fail();
				} 
				finally{
					latch.countDown();
				}
			} 
		});
		
		latch.await();
	}
}
