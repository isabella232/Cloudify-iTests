package test.servicegrid.failover;

import static test.utils.AdminUtils.loadGSCs;
import static test.utils.AdminUtils.loadGSMs;
import static test.utils.LogUtils.log;
import static test.utils.ProcessingUnitUtils.getProcessingUnitInstanceName;
import static test.utils.ProcessingUnitUtils.waitForDeploymentStatus;
import static test.utils.ProcessingUnitUtils.waitForManaged;

import java.util.concurrent.CountDownLatch;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.DeploymentStatus;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.openspaces.admin.pu.events.ProcessingUnitInstanceAddedEventListener;
import org.openspaces.admin.space.SpaceDeployment;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.AbstractTest;

/**
 * Topology: 1 machine, 2 GSM and 2 GSC.
 * Tests that when a managing GSM and GSC fail, the services are provisioned by the backup GSM on an available GSC. 
 * 
 * 1. start 2 GSMs, 3 GSCs
 * 2. deploy partitioned cluster 1,1
 * 3. verify 1 instance per GSC
 * 4. kill managing GSM and 1 occupied GSC
 * 5. verify that backup GSM takes over management of failed service
 * 6. service instance is provisioned on available GSC
 * 7. verify that deployment status is intact
 * 
 * @author Moran Avigdor
 */
public class GsmGscFailureAndProvisioningOnAvailableGscTest extends
		AbstractTest {

	private Machine machine;
	private GridServiceManager[] gsms;
	private GridServiceContainer[] gscs;
	
	@BeforeMethod
	public void setup() {
		GridServiceAgent gsa = admin.getGridServiceAgents().waitForAtLeastOne();
		machine = gsa.getMachine();
		
		log("loading 2 GSM, 2 GSCs on " + machine.getHostName());
		gsms = loadGSMs(machine, 2);
		gscs = loadGSCs(machine, 2);
	}
	
	@Test(timeOut=DEFAULT_TEST_TIMEOUT, groups = "1")
	public void test() throws InterruptedException {
		
		log("Deploying cluster A partitioned 1,1");
		ProcessingUnit pu = gsms[0].deploy(new SpaceDeployment("A").partitioned(1, 1));
		pu.waitFor(pu.getTotalNumberOfInstances());
        log("ProcessingUnit A total Number of instances "+ pu.getTotalNumberOfInstances());
		for (GridServiceContainer gsc : gscs) {
			assertEquals("Expected each GSC to contain 1 instance", 1, gsc.getProcessingUnitInstances().length);
		}
		
		final CountDownLatch addedLatch = new CountDownLatch(pu.getTotalNumberOfInstances());
		gscs[1].getProcessingUnitInstanceAdded().add(new ProcessingUnitInstanceAddedEventListener() {
			
			public void processingUnitInstanceAdded(
					ProcessingUnitInstance processingUnitInstance) {
				addedLatch.countDown();				
			}
		});

		log("killing managing GSM #1");
		assertEquals("expected GSM to manage pu", gsms[0], pu.getManagingGridServiceManager());
		gsms[0].kill();
		
		log("killing GSC #1, containing: " + getProcessingUnitInstanceName(gscs[0].getProcessingUnitInstances()));
		gscs[0].kill();
		
		log("waiting for deployment status to be compromised");
		assertEquals("Expected scheduled deployment status", DeploymentStatus.SCHEDULED, waitForDeploymentStatus(pu, DeploymentStatus.SCHEDULED));
		
		log("waiting for backup GSM to manage processing unit");
		assertEquals("expected GSM to manage pu", gsms[1], waitForManaged(pu, gsms[1]));
		
		log("waiting for processing units to be instantiated on GSC #2");
		addedLatch.await();
		
		assertEquals("Expected processing units to be provisioned on GSC #2", 2, gscs[1].getProcessingUnitInstances().length);
		assertEquals("Expected intact deployment status", DeploymentStatus.INTACT, waitForDeploymentStatus(pu, DeploymentStatus.INTACT));
	}
}
