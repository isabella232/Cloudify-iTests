package test.admin;

import static test.utils.AdminUtils.loadGSC;
import static test.utils.AdminUtils.loadGSM;

import java.util.concurrent.TimeUnit;

import org.openspaces.admin.Admin;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.internal.discovery.DiscoveryService;
import org.openspaces.admin.machine.Machine;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.AbstractTest;
import test.utils.LogUtils;
import test.utils.TeardownUtils;

/**
 * Tests that the {@link DiscoveryService} receives events when creating two lookup discoveries on the same VM.
 * @see GS-7898
 * 
 * @author Moran Avigdor
 * @since 8.0.4
 */
public class TwoAdminInstancesCreationTest extends AbstractTest {

	@BeforeMethod
	public void setup() {
		GridServiceAgent gsa = admin.getGridServiceAgents().waitForAtLeastOne();
		Machine machine = gsa.getMachine();
		loadGSM(machine);
		loadGSC(machine);
		loadGSC(machine);
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT , groups="1")
	public void test() throws Exception {
		Admin secondAdmin = newAdmin();
		try {
			assertTrue("timeout waiting for LUS", secondAdmin.getLookupServices().waitFor(admin.getLookupServices().getSize(), 30, TimeUnit.SECONDS));
			assertTrue("timeout waiting for GSA", secondAdmin.getGridServiceAgents().waitFor(admin.getGridServiceAgents().getSize(), 30, TimeUnit.SECONDS));
			assertTrue("timeout waiting for GSM", secondAdmin.getGridServiceManagers().waitFor(admin.getGridServiceManagers().getSize(), 30, TimeUnit.SECONDS));
			assertTrue("timeout waiting for GSC", secondAdmin.getGridServiceContainers().waitFor(admin.getGridServiceContainers().getSize(), 30, TimeUnit.SECONDS));
		}finally {
			LogUtils.log("snapshot for second Admin: ");
			TeardownUtils.snapshot(secondAdmin);
		}
	}

}
