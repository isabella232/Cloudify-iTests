package test.servicegrid.lookup;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static test.utils.AdminUtils.loadGSC;
import static test.utils.AdminUtils.loadGSM;
import static test.utils.LogUtils.log;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.space.SpaceDeployment;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.AbstractTest;


public class LookupLocatorsTest extends AbstractTest {

    private Machine machine;
    private GridServiceManager gsm;
    private GridServiceContainer gsc2;
    private GridServiceContainer gsc1;

    @Override
    @BeforeMethod
    public void beforeTest() {
        super.beforeTest();
        admin = newAdminWithLocators();
        GridServiceAgent gsa = admin.getGridServiceAgents().waitForAtLeastOne();
		machine = gsa.getMachine();
		gsm = loadGSM(machine);
		gsc1 = loadGSC(machine);
        gsc2 = loadGSC(machine);
    }


	@Test(timeOut=DEFAULT_TEST_TIMEOUT, groups = "1")
	public void test() throws Exception {
        ProcessingUnit pu = gsm.deploy(new SpaceDeployment("S").numberOfInstances(2));
		log("wait for " + pu.getName() + " to instantiate");
		pu.waitFor(pu.getTotalNumberOfInstances());

        assertThat(pu.getTotalNumberOfInstances(), equalTo(2));
        
    }
}
