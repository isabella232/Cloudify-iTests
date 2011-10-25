package test.webui.services.pugrid;

import static test.utils.AdminUtils.loadGSCs;
import static test.utils.AdminUtils.loadGSM;
import static test.utils.LogUtils.log;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.DeploymentStatus;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitDeployment;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.utils.DeploymentUtils;
import test.utils.ProcessingUnitUtils;
import test.webui.AbstractSeleniumTest;
import test.webui.objects.LoginPage;
import test.webui.objects.services.ServicesTab;
import test.webui.objects.services.PuTreeGrid.WebUIProcessingUnit;

public class StatelessFeederStateTest extends AbstractSeleniumTest {
	
	Machine machineA;
	
	@BeforeMethod(alwaysRun = true)
	public void startSetup() {
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
		
		ProcessingUnit processor = gsmA.deploy(new ProcessingUnitDeployment(DeploymentUtils.getArchive("processorPu.jar")).partitioned(1, 0));
		ProcessingUnitUtils.waitForDeploymentStatus(processor, DeploymentStatus.INTACT);
		
		ProcessingUnit feeder = gsmA.deploy(new ProcessingUnitDeployment(DeploymentUtils.getArchive("feederPu.jar")));
		ProcessingUnitUtils.waitForDeploymentStatus(feeder, DeploymentStatus.INTACT);
		
	}
	
	 @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = {"cloudify" , "xap"})
	public void feederTest() throws InterruptedException {
		
		// get new login page
		LoginPage loginPage = getLoginPage();
		
		// get new topology tab
		ServicesTab servicesTab = loginPage.login().switchToServices();
		
		WebUIProcessingUnit feeder = servicesTab.getPuTreeGrid().getProcessingUnit("feederPu");
		
		assertTrue(feeder.getType().equals("Stateless"));
		
	}

}
