package test.servicegrid.undeploy;

import static test.utils.AdminUtils.loadGSCs;
import static test.utils.AdminUtils.loadGSM;
import static test.utils.LogUtils.log;
import static test.utils.ProcessingUnitUtils.getProcessingUnitInstanceName;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.openspaces.admin.pu.events.ProcessingUnitInstanceRemovedEventListener;
import org.openspaces.admin.space.SpaceDeployment;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.AbstractTest;
/**
 * Topology:
 * - 2 GSMs and 2 GSCs at 1 Machine
 * - Deploy cluster A, partitioned 1,1 via GSM A
 * - Deploy cluster B, partitioned 1,1 via GSM A
 * 
 * Test undeploy when two deployments are managed by the same GSM.
 * 
 * Start GSM A, GSC A1, GSC A2, and GSM B
 * Deploy cluster A via GSM A
 * Deploy cluster B via GSM B
 * 
 * a. clusters are deployed
 * b. undeploy cluster A
 * c. undeploy cluster B
 * d. removal events are received - undeploy is successful
 * 
 * @author Moran Avigdor
 */
public class UndeployManagedBySameGsmTest extends AbstractTest {

	@BeforeMethod
	public void setup() {
		//1 GSM and 2 GSC at 1 machine
		log("waiting for 1 machines");
		admin.getMachines().waitFor(1);

		log("waiting for 1 GSAs");
		admin.getGridServiceAgents().waitFor(1);

		GridServiceAgent[] agents = admin.getGridServiceAgents().getAgents();
		GridServiceAgent gsaA = agents[0];

		//Start GSM A, GSC A1-A2, GSM B
		log("starting: 1 GSM and 2 GSC");
		GridServiceManager gsmA = loadGSM(gsaA); //GSM A
		loadGSCs(gsaA, 2); //GSC A1, GSC A2
		loadGSM(gsaA); //GSM B
		
		//Deploy Cluster A to GSM A
		log("deploy Cluster A, partitioned 1,1 via GSM A");
		ProcessingUnit puA = gsmA.deploy(new SpaceDeployment("A").partitioned(1, 1).maxInstancesPerVM(1));

		//Deploy Cluster B to GSM A
		log("deploy Cluster B, partitioned 1,1 via GSM A");
		ProcessingUnit puB = gsmA.deploy(new SpaceDeployment("B").partitioned(1, 1).maxInstancesPerVM(1));

		assertTrue(puA.waitFor(puA.getTotalNumberOfInstances()));
		assertTrue(puB.waitFor(puB.getTotalNumberOfInstances()));
	}
	
	@Test(timeOut=DEFAULT_TEST_TIMEOUT, groups = "1")
	public void test() throws Exception {
		final CountDownLatch removedLatch = new CountDownLatch(4);
		admin.getProcessingUnits().getProcessingUnitInstanceRemoved().add(new ProcessingUnitInstanceRemovedEventListener() {
			public void processingUnitInstanceRemoved(
					ProcessingUnitInstance processingUnitInstance) {
				log("processingUnitInstanceRemoved event: " + getProcessingUnitInstanceName(processingUnitInstance));
				removedLatch.countDown();
			}
		});
		long interval = 10;
		admin.setProcessingUnitMonitorInterval(interval, TimeUnit.MILLISECONDS);
		ProcessingUnit[] processingUnits = admin.getProcessingUnits().getProcessingUnits();
		assertEquals(2, processingUnits.length);
		for (ProcessingUnit pu : processingUnits) {
			pu.undeploy();
			Thread.sleep(interval*100); //simulate wait-for parsing of scheduler
		}
		
		removedLatch.await();
		assertEquals("After undeploy of processing units", 0, admin.getProcessingUnits().getSize());
	}
}
