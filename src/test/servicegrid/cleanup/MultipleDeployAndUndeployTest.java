package test.servicegrid.cleanup;

import static test.utils.AdminUtils.loadGSC;
import static test.utils.AdminUtils.loadGSM;
import static test.utils.JvmUtils.runGC;
import static test.utils.LogUtils.log;

import java.text.NumberFormat;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.space.Space;
import org.openspaces.admin.space.SpaceDeployment;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.AbstractTest;

/**
 * Topology: 1 GSm, 1 GSC on 1 Machine
 * Tests that there is no memory leak when performing multiple deploy and undeploy of a single space.
 * 
 * 1. start 1 GSM and 1 GSC
 * 2. measure resources
 * 3. perform:
 * 	3.1 deploy partitioned 1,1
 *  3.2 verify deployment is successful
 *  3.3 undeploy it
 *  3.4 measure resources
 * 
 * @author Moran Avigdor
 */
public class MultipleDeployAndUndeployTest extends AbstractTest {

	private Machine machine;
	private GridServiceManager gsm;
	private GridServiceContainer gsc;
	
	@BeforeMethod
	public void setup() {
        GridServiceAgent gsa = admin.getGridServiceAgents().waitForAtLeastOne();
        machine = gsa.getMachine();

        gsm = loadGSM(machine);
        gsc = loadGSC(machine);
        assertUsedMemory(false);
	}
	
	@Test(timeOut=DEFAULT_TEST_TIMEOUT, groups = "1")
	public void test() {
		for (int i=1; i<=20; ++i) {
			log("deployment #"+i);
			final ProcessingUnit pu = gsm.deploy(new SpaceDeployment("A").maxInstancesPerVM(1));
			log("wait for instantiation");
			pu.waitFor(pu.getTotalNumberOfInstances());
			Space space = pu.waitForSpace();
			log("write to space");
			space.getGigaSpace().writeMultiple(createObjects());
			log("undeploy #"+i);
			pu.undeploy();
			assertEquals("unexpected instances after undeployment", 0, machine.getProcessingUnitInstances().length);
			assertUsedMemory(false);
		}
		
		assertUsedMemory(true);
	}
	
	private void assertUsedMemory(boolean forceAssert) {
		NumberFormat instance = NumberFormat.getInstance();
    	instance.setMaximumFractionDigits(2);
    	
		runGC(gsm.getVirtualMachine());
		double gsmMemoryHeapUsedPerc = gsm.getVirtualMachine().getStatistics().getMemoryHeapUsedPerc();
		log("GSM used memory: " + instance.format(gsmMemoryHeapUsedPerc) + "%");
		
		runGC(gsc.getVirtualMachine());
		double gscMemoryHeapUsedPerc = gsc.getVirtualMachine().getStatistics().getMemoryHeapUsedPerc();
		log("GSC used memory: " + instance.format(gscMemoryHeapUsedPerc) + "%");

		if (forceAssert) {
    		assertTrue("expected less than 10% memory usage for GSM", gsmMemoryHeapUsedPerc < 10.0);
    		assertTrue("expected less than 30% memory usage for GSC", gscMemoryHeapUsedPerc < 30.0);
		}
	}

	private String[] createObjects() {
		String[] objs = new String[50000];
		for (int i=0; i<objs.length; ++i) {
			objs[i] = new String("str-"+i);
		}
		return objs;
	}
}
