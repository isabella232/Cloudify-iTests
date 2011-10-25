package test.servicegrid.failover;

import static test.utils.LogUtils.log;

import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.space.SpaceDeployment;
import org.testng.annotations.Test;


/**
 * Topology:
 * - 1 GSM and 2 GSC at 2 machines
 * - Cluster A - partitioned-sync2backup 1,1 with max-per-machine=1 
 * - Cluster B - partitioned-sync2backup 1,1 with max-per-machine=1
 * 
 * TEST (6) Machine GSM, GSC Failure and recovery, failure and recovery (cluster managed by different GSM)
 * 
 * Start GSM A, GSC A1, GSM B, GSC B1, GSC A2, GSC B2
 * Deploy Cluster A to GSM A
 * Deploy Cluster B to GSM B
 * 
 * a. Spaces are evenly distributed across all 4 GSCs
 * b. 2 clusters deployed, 4 spaces total
 * c. Kill -9 GSM A, GSC A1, GSC A2
 *    Space instances from A1, A2 are not re-deployed because of max-per-machine
 * d. Only 2 primaries are available.
 * e. Start GSM A, GSC A1, GSC A2
 *    2 backups are started on A1 and/or A2
 * f. 2 clusters are available, 4 spaces total
 * g. Kill -9 GSM B, GSC B1, GSC B2
 *    Space instances from B1, B2 are not re-deployed because of max-per-machine
 * h. Only 2 primaries are available.
 * i. Start GSM B, GSC B1, GSC B2
 *    Both backups are started on B1 and/or B2
 * j. 2 clusters are available, 4 spaces total
 * 
 * @author Moran Avigdor
 */
@Test(groups = "2")
public class MachineFailureAndRecoveryX2ManagedByDifferentGSMsTest extends MachineFailureAndRecoveryX2Test {

	@Override
	protected void doDeploy(GridServiceManager gsmA, GridServiceManager gsmB) {
		log("deploy cluster A, partitioned 1,1, via GSM A");
		ProcessingUnit puA = gsmA.deploy(new SpaceDeployment("A").partitioned(1, 1).maxInstancesPerMachine(1).maxInstancesPerVM(0));
		assertTrue(puA.waitFor(puA.getTotalNumberOfInstances()));

		log("deploy cluster B, partitioned 1,1, via GSM B");
		ProcessingUnit puB = gsmB.deploy(new SpaceDeployment("B").partitioned(1, 1).maxInstancesPerMachine(1).maxInstancesPerVM(0));
		assertTrue(puB.waitFor(puB.getTotalNumberOfInstances()));
	}
}
