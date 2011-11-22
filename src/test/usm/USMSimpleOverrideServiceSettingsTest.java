package test.usm;

import static framework.utils.AdminUtils.loadGSM;
import static framework.utils.LogUtils.log;

import java.io.File;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.ProcessingUnit;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.cli.cloudify.CommandTestUtils;

import com.gigaspaces.cloudify.dsl.Service;
import com.gigaspaces.cloudify.dsl.internal.ServiceReader;
import com.gigaspaces.cloudify.dsl.internal.packaging.PackagingException;

import framework.tools.SGTestHelper;
import framework.utils.AdminUtils;

public class USMSimpleOverrideServiceSettingsTest extends UsmAbstractTest {

	private Machine machineA;

	@Override
	@BeforeMethod
	public void beforeTest() {
		super.beforeTest();

		// 1 GSM and 2 GSC at 2 machines
		log("waiting for 2 machines!");
		admin.getMachines().waitFor(2);

		log("waiting for 2 GSA");
		admin.getGridServiceAgents().waitFor(2);

		GridServiceAgent[] agents = admin.getGridServiceAgents().getAgents();
		GridServiceAgent gsaA = agents[0];
		GridServiceAgent gsaB = agents[1];

		machineA = gsaA.getMachine();

		// Start GSM A, GSC A,B
		log("starting: 1 GSM and 2 GSC at 2 machines");
		loadGSM(machineA); // GSM A
		AdminUtils.loadGSC(gsaA);
		AdminUtils.loadGSC(gsaB);

	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "2")
	public void testOverrideNumberOfInstances() throws Exception {

		int numberOfInstances = 2;

		File folderPath = new File(CommandTestUtils.getPath("apps/USM/usm/" + processName));
		Service service = ServiceReader.readService(folderPath);
		service.setNumInstances(numberOfInstances);
		USMTestUtils.packAndDeploy(folderPath.getAbsolutePath(), service);

		ProcessingUnit pu = admin.getProcessingUnits().waitFor(service.getName());

		assertEquals(numberOfInstances, pu.getTotalNumberOfInstances());

		pu.waitFor(pu.getTotalNumberOfInstances());
		pu.startStatisticsMonitor();

		USMTestUtils.assertMonitors(pu);

		pu.undeploy();
		Assert.assertNull(admin.getProcessingUnits().getProcessingUnit(service.getName()));

	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "2")
	public void testOverrideMaxJarSize() throws Exception {

		int maxJarSize = 1024;

		File folderPath = new File(SGTestHelper.getSGTestRootDir() , "apps/USM/usm/" + processName);
		Service service = ServiceReader.readService(folderPath);
		service.setMaxJarSize(maxJarSize);
		
		try {
			USMTestUtils.packAndDeploy(folderPath.getAbsolutePath(), service);
			Assert.fail("Failed overriding max jar property");
		} catch (PackagingException e) {
			assertTrue("Unexpected exception", e.getMessage().contains("it must be smaller than: 1 KB"));
		}

	}

}
