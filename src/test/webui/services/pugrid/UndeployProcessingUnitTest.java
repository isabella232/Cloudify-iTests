package test.webui.services.pugrid;

import static test.utils.AdminUtils.loadGSCs;
import static test.utils.AdminUtils.loadGSM;
import static test.utils.LogUtils.log;

import java.io.IOException;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.DeploymentStatus;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.space.SpaceDeployment;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.utils.AssertUtils.RepetitiveConditionProvider;
import test.utils.AssertUtils;
import test.utils.LogUtils;
import test.utils.ProcessingUnitUtils;
import test.webui.AbstractSeleniumTest;
import test.webui.objects.LoginPage;
import test.webui.objects.services.PuTreeGrid;
import test.webui.objects.services.ServicesTab;
import test.webui.objects.services.PuTreeGrid.WebUIProcessingUnit;

public class UndeployProcessingUnitTest extends AbstractSeleniumTest {
	
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
		SpaceDeployment deployment = new SpaceDeployment("Test");
		Pu = gsmA.deploy(deployment);
		ProcessingUnitUtils.waitForDeploymentStatus(Pu, DeploymentStatus.INTACT);
	}
	
	 @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = {"cloudify" , "xap"})
	public void undeployPu() throws InterruptedException, IOException {
		
		// get new login page
		LoginPage loginPage = getLoginPage();

		// get new topology tab
		ServicesTab topologyTab = loginPage.login().switchToServices();
		
		// get the Processing Unit grid from topology tab
		final PuTreeGrid puGrid = topologyTab.getPuTreeGrid();
	
		RepetitiveConditionProvider condition = new RepetitiveConditionProvider() {	
			public boolean getCondition() {
				WebUIProcessingUnit test2 = null;
				test2 = puGrid.getProcessingUnit("Test");
				return (test2 != null);
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition,3000);
		
		LogUtils.log("undeploying processing unit...");
		WebUIProcessingUnit test = puGrid.getProcessingUnit("Test");
		test.undeploy();
		ProcessingUnitUtils.waitForDeploymentStatus(Pu, DeploymentStatus.UNDEPLOYED);
		
		// assert pu is no longer visible
		condition = new RepetitiveConditionProvider() {
			
			public boolean getCondition() {
				WebUIProcessingUnit test2 = null;
				test2 = puGrid.getProcessingUnit("Test");
				return (test2 == null);
			}
		};
		
		AssertUtils.repetitiveAssertTrue(null, condition,3000);
	}

}
