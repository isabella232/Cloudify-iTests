package test.servicegrid.sla.zones;

import static test.utils.AdminUtils.loadGSC;
import static test.utils.AdminUtils.loadGSCs;
import static test.utils.AdminUtils.loadGSM;
import static test.utils.LogUtils.log;
import static test.utils.ProcessingUnitUtils.waitForDeploymentStatus;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.DeploymentStatus;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.openspaces.admin.space.SpaceDeployment;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.AbstractTest;

/**
 * Topology: 1 GSM, 2 GSC in zone1, 1 GSC in zone2
 * Tests that zone SLA is obeyed for deployment of partitioned space (no backups).
 * 
 * 1. load 1 GSM,
 *    load 2 GSC in zone1
 *    load 1 GSC in zone2
 * 2. deploy partitioned 2,0 to zone1/1
 * 3. only one instance should instantiate
 * 4. deployment should be compromised
 * 5. zero instances on zone2
 * 
 * @author Moran Avigdor
 */
public class PartitionedNoBackupsZoneDeploymentTest extends AbstractTest {

	private Machine machine;
	private GridServiceManager gsm;
	private GridServiceContainer[] zone1;
	private GridServiceContainer zone2;

	@BeforeMethod
	public void setup() {
		GridServiceAgent gsa = admin.getGridServiceAgents().waitForAtLeastOne();
		machine = gsa.getMachine();
		
		gsm = loadGSM(machine);
		zone1 = loadGSCs(machine, 2, "zone1");
		zone2 = loadGSC(machine, "zone2");
	}
	
	@Test(timeOut=DEFAULT_TEST_TIMEOUT, groups = "1")
	public void test() {
		log("deploy partitioned X 2,0 zone1/1");
		ProcessingUnit pu = gsm.deploy(new SpaceDeployment("X").partitioned(2,
				0).addZone("zone1").maxInstancesPerZone("zone1", 1));
		
		log("wait for 1 space instance to instantiate");
		pu.waitForSpace();
		assertEquals(DeploymentStatus.COMPROMISED, waitForDeploymentStatus(pu, DeploymentStatus.COMPROMISED));
		
		for (ProcessingUnitInstance puInstance : pu) {
			assertTrue("expected zone1", puInstance.getGridServiceContainer().getZones().containsKey("zone1"));
		}
		
		assertEquals("expected 0 instances on this zone", 0, zone2.getProcessingUnitInstances().length);
		
		int count = 0;
		for (GridServiceContainer gsc : zone1) {
			count += gsc.getProcessingUnitInstances().length;
		}
		assertEquals("expected 1 instances on this zone", 1, count);
	}
}
