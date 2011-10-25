package test.webui.services.pugrid;

import static test.utils.AdminUtils.loadGSCs;
import static test.utils.AdminUtils.loadGSM;
import static test.utils.LogUtils.log;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.DeploymentStatus;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.openspaces.admin.space.SpaceDeployment;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.gigaspaces.cluster.activeelection.SpaceMode;

import test.utils.LogUtils;
import test.utils.ProcessingUnitUtils;
import test.webui.AbstractSeleniumTest;
import test.webui.objects.LoginPage;
import test.webui.objects.services.PuTreeGrid;
import test.webui.objects.services.ServicesTab;
import test.webui.objects.services.PuTreeGrid.WebUIProcessingUnit;

public class RestartPuInstanceTest extends AbstractSeleniumTest {

	Machine machineA;
	ProcessingUnit Pu;

	@BeforeMethod(alwaysRun = true)
	public void startSetUp() {	
		log("waiting for 1 machine");
		admin.getMachines().waitFor(1);

		log("waiting for 1 GSA");
		admin.getGridServiceAgents().waitFor(1);

		GridServiceAgent[] agents = admin.getGridServiceAgents().getAgents();
		GridServiceAgent gsaA = agents[0];

		machineA = gsaA.getMachine();

		log("starting: 1 GSM and 2 GSC's on 1 machine");
		GridServiceManager gsmA = loadGSM(machineA); 
		loadGSCs(machineA, 2);
		
		LogUtils.log("deploying processing unit...");
		SpaceDeployment deployment = new SpaceDeployment("Test")
		.partitioned(2, 1).maxInstancesPerVM(1);
		Pu = gsmA.deploy(deployment);
		ProcessingUnitUtils.waitForDeploymentStatus(Pu, DeploymentStatus.INTACT);
	}

	 @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = {"cloudify" , "xap"})
	public void restartInstance() throws InterruptedException {

		// get new login page
		LoginPage loginPage = getLoginPage();

		// get new services tab
		ServicesTab servicesTab = loginPage.login().switchToServices();

		// get the Processing Unit grid from topology tab
		PuTreeGrid puGrid = servicesTab.getPuTreeGrid();

		LogUtils.log("restarting pu Instance...");
		WebUIProcessingUnit test = puGrid.getProcessingUnit("Test");
		
		assertTrue(test.getActualInstances() == 4);
		assertTrue(test.getPlannedInstances() == 4);
		
		// retart a pu instance
		test.restartPuInstance(1);
		admin.getProcessingUnits().getProcessingUnit("Test").waitFor(3);
		admin.getProcessingUnits().getProcessingUnit("Test").waitFor(4);
		ProcessingUnitUtils.waitForDeploymentStatus(Pu, DeploymentStatus.INTACT);
		ProcessingUnitInstance[] processingUnits = Pu.getInstances();
		int second = 0;
		while (second <= 10) {
			assertTrue(second <= 10);
			int prim = 0;
			int back = 0;
			for (int i = 0 ; i < processingUnits.length ; i++) {
				ProcessingUnitInstance  puInst = processingUnits[i];
				if (puInst.getSpaceInstance().getMode().equals(SpaceMode.PRIMARY)) prim++;
				if (puInst.getSpaceInstance().getMode().equals(SpaceMode.BACKUP)) back++;
			}
			if ((prim == 2) && (back == 2)) break;
			else {
				Thread.sleep(1000);
				second++;
			}
		}
		
		test = puGrid.getProcessingUnit("Test");
		
		assertTrue(test.getActualInstances() == 4);
		assertTrue(test.getPlannedInstances() == 4);
		
	}
}
