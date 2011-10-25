package test.servicegrid.exceptions;

import static test.utils.AdminUtils.loadGSCs;
import static test.utils.AdminUtils.loadGSM;
import static test.utils.LogUtils.log;
import static test.utils.ToStringUtils.machineToString;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitAlreadyDeployedException;
import org.openspaces.admin.space.SpaceDeployment;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.AbstractTest;

public class ProcessingUnitAlreadyDeployedExceptionTest extends AbstractTest {
    private Machine machine;
    private GridServiceManager gsm;
    private GridServiceContainer[] gsc;


    @BeforeMethod
    public void setup() {
        assertTrue(admin.getMachines().waitFor(1));
        assertTrue(admin.getGridServiceAgents().waitFor(1));
        
        GridServiceAgent gsa = admin.getGridServiceAgents().waitForAtLeastOne();
        
        machine = gsa.getMachine();

        log("load 1 GSM and 1 GSC on " + machineToString(machine));
        gsm = loadGSM(machine);
        gsc = loadGSCs(machine, 1);

    }

    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1")
    public void test() throws Exception {
        ProcessingUnit puA = gsm.deploy(new SpaceDeployment("A"));
        assertTrue(puA.waitFor(puA.getTotalNumberOfInstances()));
        try {
            ProcessingUnit puB = gsm.deploy(new SpaceDeployment("A"));
            Assert.fail("ProcessingUnitAlreadyDeployedException should be thrown");
        } catch (ProcessingUnitAlreadyDeployedException e) {
            //expected exception, do nothing.
        }
    }
}
