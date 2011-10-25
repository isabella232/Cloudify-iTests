package test.admin.jvm;

import static test.utils.AdminUtils.loadESM;
import static test.utils.AdminUtils.loadGSC;
import static test.utils.AdminUtils.loadGSM;

import java.util.concurrent.ExecutionException;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.vm.VirtualMachine;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.AbstractTest;
import test.utils.LogUtils;

/**
 * GS-8847
 * Test that we can conclude from a VirtualMachine it's type: GSA, GSM, GSC, ESM, 2xLUS
 * 
 * @author Moran Avigdor
 * @since 8.0.4
 */
public class VirtualMachineTypeTest extends AbstractTest {

    private static int NUMBER_OF_VMS = -1;
	private GridServiceAgent gsa;

    @BeforeMethod
	public void setup() {
		gsa = admin.getGridServiceAgents().waitForAtLeastOne();
		//2xLUS was loaded by SGTest
		loadGSM(gsa);
		loadGSC(gsa);
		loadESM(gsa);
	}

    @Test(timeOut = DEFAULT_TEST_TIMEOUT , groups="1")
    public void test() throws InterruptedException, ExecutionException {
    	//running locally or in regression?
    	final boolean usingSingleMachine = admin.getMachines().getSize() == 1;
    	
    	assertNumberOfVMs(usingSingleMachine);
    	
    	for (VirtualMachine virtualMachine : admin.getVirtualMachines()) {
    		if (virtualMachine.getLookupService() != null) {
    			LogUtils.log("found LUS at JVM with pid[" + virtualMachine.getDetails().getPid()+"]");
    			continue;
    		}

    		if (virtualMachine.getGridServiceManager() != null) {
    			LogUtils.log("found GSM at JVM with pid[" + virtualMachine.getDetails().getPid()+"]");
    			continue;
    		}

    		if (virtualMachine.getGridServiceAgent() != null) {
    			LogUtils.log("found GSA at JVM with pid[" + virtualMachine.getDetails().getPid()+"]");
    			continue;
    		}

    		if (virtualMachine.getGridServiceContainer() != null) {
    			LogUtils.log("found GSC at JVM with pid[" + virtualMachine.getDetails().getPid()+"]");
    			continue;
    		}
    		
    		if (virtualMachine.getElasticServiceManager() != null) {
    			LogUtils.log("found ESM at JVM with pid[" + virtualMachine.getDetails().getPid()+"]");
    			continue;
    		}
    		
    		AssertFail("Encountered an unknown JVM! with pid[" + virtualMachine.getDetails().getPid()+"]");
    	}
    	
    	//re-check
    	assertNumberOfVMs(usingSingleMachine);
    }

	private void assertNumberOfVMs(boolean usingSingleMachine) {
		if (usingSingleMachine) {
    		NUMBER_OF_VMS = 6;
    		assertEquals("Expected 6 JVMs: GSA, 2xLUS, GSM, GSC, ESM", NUMBER_OF_VMS, admin.getVirtualMachines().getSize());
    	} else {
    		//running in regression (4 machines)
    		NUMBER_OF_VMS = 9;
    		assertEquals("Expected 9 JVMs: 4xGSA, 2xLUS, GSM, GSC, ESM", NUMBER_OF_VMS, admin.getVirtualMachines().getSize());    		
    	}
	}
}
