package test.gsm.stateful.manual.memory;

import java.io.File;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitDeployment;
import org.openspaces.core.GigaSpace;
import org.openspaces.example.simpledata.common.SimpleDataPojo;
import org.testng.annotations.Test;

import test.AbstractTest;
import test.utils.AdminUtils;
import test.utils.DeploymentUtils;

import com.j_spaces.core.client.ReadModifiers;

public class SimpleDataAppTest extends AbstractTest {

	public static String POSTPROCESS_PU_STR = "Hello World !!";

	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1")
	public void testElasticStatefulProcessingUnitDeployment()
			throws InterruptedException {
			
		String appName = "simpledata";

		// String appName = "data";

		DeploymentUtils.prepareApp(appName);

		File puDir = DeploymentUtils.getProcessingUnit(appName, "processor");

		GridServiceAgent gsa = admin.getGridServiceAgents().waitForAtLeastOne();

		GridServiceManager gsm = AdminUtils.loadGSM(gsa);

		AdminUtils.loadGSCs(gsa, 2);

		ProcessingUnit pu = gsm.deploy(new ProcessingUnitDeployment(puDir)
				.maxInstancesPerVM(1).partitioned(1, 1));

		// finalize deploy

		pu.waitFor(pu.getTotalNumberOfInstances());

		GigaSpace gigaSpace = pu.waitForSpace().getGigaSpace();

		int numberOfPojos = 1000;

		SimpleDataPojo[] messages = new SimpleDataPojo[1000];

		for (int i = 0; i < numberOfPojos; i++) {

			messages[i] = new SimpleDataPojo();

			messages[i].setRawData("Hello ");

			messages[i].setId((long) i);

		}

		gigaSpace.writeMultiple(messages);

		Thread.sleep(5000);

		SimpleDataPojo template = new SimpleDataPojo();

		template.setRawData(POSTPROCESS_PU_STR);
		
		assertEquals("Number of Person Pojos in space", numberOfPojos,
				gigaSpace.count(template,ReadModifiers.READ_COMMITTED));

	}

}
