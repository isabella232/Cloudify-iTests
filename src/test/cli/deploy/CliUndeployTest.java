package test.cli.deploy;

import static test.utils.AdminUtils.loadGSCs;
import static test.utils.AdminUtils.loadGSM;
import static test.utils.LogUtils.log;
import static test.utils.ProcessingUnitUtils.getProcessingUnitInstanceName;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.openspaces.admin.pu.events.ProcessingUnitInstanceRemovedEventListener;
import org.openspaces.admin.pu.events.ProcessingUnitRemovedEventListener;
import org.openspaces.admin.space.SpaceDeployment;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.AbstractTest;
import test.utils.CliUtils;

/***
 * Setup: Bring up 1 GSM and 2 GSC's on on machine.
 *        Then deploy space "A" partitioned 2,1
 * 
 * Test: "cli undeploy" functionality
 * 
 * @author Dan Kilman
 *
 */
public class CliUndeployTest extends AbstractTest {
	
    @Override
	@BeforeMethod
	public void beforeTest() {
	
        super.beforeTest();
        
        //Needed so GS.main could work properly
        String key = "com.gs.jini_lus.groups";
        String value = admin.getGroups()[0];
        System.setProperty(key, value);
	    
	    //1 GSM and 2 GSC at 1 machines
		log("waiting for 1 machine");
		admin.getMachines().waitFor(1);

		log("waiting for 1 GSA");
		admin.getGridServiceAgents().waitFor(1);
		
		GridServiceAgent[] agents = admin.getGridServiceAgents().getAgents();
		GridServiceAgent gsaA = agents[0];
		
		Machine machineA = gsaA.getMachine();
		
		//Start GSM A, GSC A1, A2
		log("starting: 1 GSM and 2 GSC's");
		GridServiceManager gsm = loadGSM(machineA); //GSM A
		loadGSCs(machineA, 2); //GSC A1, GSC A2

		//Deploy Cluster A to GSM A
        log("deploy Cluster A, partitioned 2,1");
        ProcessingUnit puA= gsm.deploy(new SpaceDeployment("A").partitioned(2, 1).maxInstancesPerVM(1));
		
        assertTrue(puA.waitFor(puA.getTotalNumberOfInstances()));
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1")
	public void testUndeploy() throws InterruptedException {
       
	    final CountDownLatch removedLatch = new CountDownLatch(4);
        admin.getProcessingUnits().getProcessingUnitInstanceRemoved().add(new ProcessingUnitInstanceRemovedEventListener() {
            public void processingUnitInstanceRemoved(
                    ProcessingUnitInstance processingUnitInstance) {
                log("processingUnitInstanceRemoved event: " + getProcessingUnitInstanceName(processingUnitInstance));
                removedLatch.countDown();
            }
        });
        
        final CountDownLatch removedPULatch = new CountDownLatch(1);
        admin.getProcessingUnits().getProcessingUnitRemoved().add(new ProcessingUnitRemovedEventListener() {
            public void processingUnitRemoved(ProcessingUnit processingUnit) {
                log("processingUnitRemoved event: " + processingUnit.getName());
                removedPULatch.countDown();
            }
        });
        
        
        String[] args = { 
            "undeploy", "A" 
        };
        CliUtils.invokeGSMainOn(false, args);
        
        boolean returned = removedLatch.await(30, TimeUnit.SECONDS);
        assertTrue(returned);
        
        returned = removedPULatch.await(30, TimeUnit.SECONDS);
        assertTrue(returned);
    
	}
	
}
