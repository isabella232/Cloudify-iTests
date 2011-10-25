package test.servicegrid.relocation;

import static test.utils.AdminUtils.loadGSCs;
import static test.utils.AdminUtils.loadGSM;
//import static test.utils.AdminUtils.loadGSMWithSystemProperty;
import static test.utils.AdminUtils.loadGSCWithSystemProperty;
import static test.utils.LogUtils.log;

import java.io.File;
import java.util.ArrayList;

import org.junit.Assert;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitDeployment;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.gigaspaces.log.ContainsStringLogEntryMatcher;
import com.gigaspaces.log.LogEntries;

import test.AbstractTest;
import test.utils.DeploymentUtils;
import test.utils.ToStringUtils;

/**
 * GS-9082: Failed instantiation of processing unit in more than one GSC results in an instantiation loop on one of the GSC!
 * This causes the GSC to crash with an OOM.
 * 
 * Test: 
 * use one machine
 * 1. load 1 GSM, 2 GSCs
 * 2. deploy 2 instances of simpleFaultyStatelessPU
 * 3. the deploy should succeed.
 * 4. load 3 extra GSCs with the fail property: -Dtest.fail-instantiation.enabled
 * 5. relocate one instance to one of the 3 GSCs in step 4
 * 6. relocation should fail
 * 7. instance should be loaded in origin GSC.
 * 8. make sure GSM tried to instantiate on all bad GSCs before giving up and returning to origin.
 * 
 * @author Moran Avigdor
 * @since 8.0.3
 */
public class RelocateAndFailInstantiationTest  extends AbstractTest {
	private Machine machine;
	private GridServiceManager gsm;

	@BeforeMethod
	public void setup() {
		GridServiceAgent gsa = admin.getGridServiceAgents().waitForAtLeastOne();
		machine = gsa.getMachine();
		gsm = loadGSM(machine);
		loadGSCs(machine, 2);
	}
	
	@Test(timeOut=DEFAULT_TEST_TIMEOUT, groups = "1")
	public void test() throws Exception {
		
		// extra step - deploy another pu to occupy the 'good' GSCs so that when
		// we relocate the GSM will prefer one of the 'bad' GSCs because it
		// weights less when empty.
		deployStatelessProcessingUnitOnSingleMachine(gsm, 2);
		
		//2. deploy 2 instances of simpleFaultyStatelessPU
		ProcessingUnit pu = deployFaultyStatelessProcessingUnitOnSingleMachine(gsm, 2);
		ProcessingUnitInstance puInstanceToRelocate = pu.getInstances()[0];
		GridServiceContainer originGsc = puInstanceToRelocate.getGridServiceContainer();
		
		//4. load 3 extra GSCs with the fail property
		GridServiceContainer[] badGscs = loadGscsWithFaultyProperty();
		
		//5. relocate one instance to one of the 3 GSCs in step 4
		log("relocating pu from " + ToStringUtils.gscToString(originGsc) + " to " + ToStringUtils.gscToString(badGscs[0]));
		ProcessingUnitInstance relocatedPuInstance = puInstanceToRelocate.relocateAndWait(badGscs[0]);
		GridServiceContainer hostingGsc = relocatedPuInstance.getGridServiceContainer();
		if (!hostingGsc.equals(originGsc)) {
			Assert.fail("processing unit instance has been relocated to an unexpected GSC: " + ToStringUtils.gscToString(hostingGsc)
					+"  - expected GSC: " + ToStringUtils.gscToString(originGsc));
		}
		
		//8. make sure GSM tried to instantiate on all bad GSCs before giving up and returning to origin.
		checkForFailedInstantiationOnBadGscs(badGscs);
	}
	
	private void checkForFailedInstantiationOnBadGscs(
			GridServiceContainer[] badGscs) {

		for (int i=0; i<badGscs.length; ++i) {
			GridServiceContainer gsc = badGscs[i];
			LogEntries logEntries = gsc.logEntries(new ContainsStringLogEntryMatcher("Caused by: org.jini.rio.core.JSBInstantiationException: java.lang.RuntimeException: Simple Faulty PU 'test.fail-instantiation.enabled=true' - failing instantiation!"));
			assertEquals(1, logEntries.getTotalLogFiles());
		}
	}

	private GridServiceContainer[] loadGscsWithFaultyProperty() {
		ArrayList<GridServiceContainer> list = new ArrayList<GridServiceContainer>();
		
		for (int i=0; i<3; ++i) {
			GridServiceContainer gsc = loadGSCWithSystemProperty(machine, "-Dtest.fail-instantiation.enabled=true");
			log("loaded GSC with -Dtest.fail-instantiation.enabled=true property " + ToStringUtils.gscToString(gsc));
			list.add(gsc);
		}
		
		return list.toArray(new GridServiceContainer[list.size()]);
	}

	private ProcessingUnit deployStatelessProcessingUnitOnSingleMachine(GridServiceManager gsm, int numberOfInstances) {
        
        File archive = DeploymentUtils.getArchive("simpleStatelessPu.jar");
        final ProcessingUnit pu = gsm.deploy(
                new ProcessingUnitDeployment(archive)
                .numberOfInstances(numberOfInstances)
                .maxInstancesPerVM(1));
        
        pu.waitFor(numberOfInstances);
        return pu;
    }
	
	private ProcessingUnit deployFaultyStatelessProcessingUnitOnSingleMachine(GridServiceManager gsm, int numberOfInstances) {
        
        File archive = DeploymentUtils.getArchive("simpleFaultyStatelessPu.jar");
        final ProcessingUnit pu = gsm.deploy(
                new ProcessingUnitDeployment(archive)
                .numberOfInstances(numberOfInstances)
                .maxInstancesPerVM(1));
        
        pu.waitFor(numberOfInstances);
        return pu;
    }
	
}
