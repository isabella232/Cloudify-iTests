package test.servicegrid.relocation;

import static test.utils.AdminUtils.loadGSC;
import static test.utils.AdminUtils.loadGSMs;
import static test.utils.LogUtils.log;
import static test.utils.ToStringUtils.gscToString;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.openspaces.admin.space.SpaceDeployment;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.AbstractTest;

/**
 * Topology: 1 machine, 1 GSM, 2 GSCs
 * Tests that if relocatedAnWait to the same GSC works.
 *
 * a. Deploy single space
 * b. relocate space to the same GSC
 * c. verify no extra instance is instantiated
 *
 * @author Kobi
 */
public class RelocateAndWaitTest extends AbstractTest {

    private Machine machine;
    private GridServiceManager gsm;
    private GridServiceContainer gsc0;
    private GridServiceContainer gsc1;

    @BeforeMethod
    public void setup() {
        GridServiceAgent gsa = admin.getGridServiceAgents().waitForAtLeastOne();
        machine = gsa.getMachine();
        gsm = loadGSMs(machine, 1)[0];
        gsc0 = loadGSC(machine);
    }

    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1")
    public void test() throws Exception {
        ProcessingUnit puP = gsm.deploy(new SpaceDeployment("P"));
        log("wait for " + puP.getName() + " to instantiate");
        puP.waitFor(puP.getTotalNumberOfInstances());
        
        gsc1 = loadGSC(machine);
        
        log("Relocate Space P from GSC[ "+ gscToString(gsc0) +" ] to GSC [ "+gscToString(gsc0)+" ]");
        ProcessingUnitInstance toBeRelocated = gsc0.getProcessingUnitInstances()[0];
        ProcessingUnitInstance relocated = toBeRelocated.relocateAndWait(gsc1);
        Assert.assertFalse(relocated.getUid().equals(toBeRelocated.getUid()));

        Assert.assertEquals(0, gsc0.getProcessingUnitInstances().length);
        Assert.assertEquals(1, gsc1.getProcessingUnitInstances().length);
    }
}
