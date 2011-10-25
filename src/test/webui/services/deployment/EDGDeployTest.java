package test.webui.services.deployment;

import static test.utils.AdminUtils.loadGSCs;
import static test.utils.AdminUtils.loadGSM;
import static test.utils.LogUtils.log;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLStreamException;

import org.junit.Assert;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.DeploymentStatus;
import org.openspaces.admin.pu.ProcessingUnit;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import test.utils.LogUtils;
import test.utils.ProcessingUnitUtils;
import test.utils.WebUiUtils;
import test.webui.AbstractSeleniumTest;
import test.webui.interfaces.IDeployWindow;
import test.webui.objects.LoginPage;
import test.webui.objects.services.ServicesTab;
import test.webui.objects.services.PuTreeGrid.WebUIProcessingUnit;

public class EDGDeployTest extends AbstractSeleniumTest {
	
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
		
	}
	
	@Test(dataProvider = "TestParamProvider" ,timeOut = DEFAULT_TEST_TIMEOUT, groups = {"cloudify" , "xap"})
	public void deployEdgTest(String dataGridName, String isSecured, String userName, 
			String password, String numberOfInstances,
			String numberOfBackups, String clusterSchema, String maxInstPerVM,
			String maxInstPerMachine) throws InterruptedException {
		
		// get new login page
		LoginPage loginPage = getLoginPage();
		// get new topology tab
		ServicesTab topologyTab = loginPage.login().switchToServices();
		
		LogUtils.log("deploying data grid...");
		// open the deployment window
		IDeployWindow deployEDG = topologyTab.openEDGDeployWindow(dataGridName, isSecured, userName, password, numberOfInstances, 
				numberOfBackups, clusterSchema, maxInstPerVM, maxInstPerMachine);
		
		// submit deployment specs
		deployEDG.sumbitDeploySpecs();
		
		// deploy onto the grid
		deployEDG.deploy();
		
		// wait for PU
		ProcessingUnit test = null;
		int seconds = 0;
		while (test == null) {
			if (seconds >= 30) Assert.fail("admin wasnt able to capture pu"); 
			test = admin.getProcessingUnits().getProcessingUnit(dataGridName);
			seconds++;
			Thread.sleep(1000);
		}
		ProcessingUnitUtils.waitForDeploymentStatus(test, DeploymentStatus.INTACT);
		
		// close the deployment window
		deployEDG.closeWindow();
		
		// assert pu exists in pu tree grid
		WebUIProcessingUnit datagrid = topologyTab.getPuTreeGrid().getProcessingUnit(dataGridName);
		
		assertTrue(datagrid != null);
		
	}
	
	@DataProvider(name = "TestParamProvider") 
	public String[][] testParamProvider() throws XMLStreamException, FactoryConfigurationError, IOException {
		Iterator<List<String>> iter = new WebUiUtils.TestParamIterator(WebUiUtils.xmlPath);
		String[][] testData = WebUiUtils.getData(iter, "EDGDeployTest");
		return testData;	
	}
}
