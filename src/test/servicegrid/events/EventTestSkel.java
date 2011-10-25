package test.servicegrid.events;

import static test.utils.AdminUtils.loadGSC;
import static test.utils.AdminUtils.loadGSM;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.machine.Machine;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.AbstractTest;

public abstract class EventTestSkel extends AbstractTest {

	protected Machine machineA;
	protected Machine machineB;
	protected GridServiceManager gsmA;
	protected GridServiceManager gsmB;
	protected GridServiceContainer gscA;
	protected GridServiceContainer gscB;

	public EventTestSkel() {
		super();
	}

	/**
	 * Test the arrival of notifications for the following events :
	 * 
	    GridServiceContainerLifecycleEventListener,
	    GridServiceManagerLifecycleEventListener,
	    ProcessingUnitLifecycleEventListener,
	    ProcessingUnitInstanceLifecycleEventListener,
	    LookupServiceLifecycleEventListener,
	    VirtualMachineLifecycleEventListener,
	    SpaceLifecycleEventListener,
	    SpaceInstanceLifecycleEventListener,
	    SpaceModeChangedEventListener,
	    ReplicationStatusChangedEventListener,
	    ZoneLifecycleEventListener 
	 *
	 * @author rafi
	 * @throws Exception 
	 */
	@BeforeMethod
	public void setup() throws Exception {
		assertTrue(admin.getMachines().waitFor(2));
		assertTrue(admin.getGridServiceAgents().waitFor(2));
		
		GridServiceAgent[] agents = admin.getGridServiceAgents().getAgents();
		GridServiceAgent gsaA = agents[0];
		GridServiceAgent gsaB = agents[1];
		
		machineA = gsaA.getMachine();
		machineB = gsaB.getMachine();
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "2")
	public abstract void test() throws Exception;
		
	protected void loadGS() throws Exception {
		gsmA=loadGSM(machineA);
		gsmB=loadGSM(machineB);
		gscA=loadGSC(machineA);
		gscB=loadGSC(machineB);
	}

	protected void testCleanup(){
		gscA.kill();
		gsmA.kill();
		gscB.kill();
		gsmB.kill();
	}
}