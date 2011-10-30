package test.webui.services.deployment;

import static framework.utils.AdminUtils.loadGSCs;
import static framework.utils.AdminUtils.loadGSM;
import static framework.utils.LogUtils.log;

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

import framework.utils.LogUtils;
import framework.utils.ProcessingUnitUtils;
import framework.utils.WebUiUtils;

import test.webui.AbstractSeleniumTest;
import test.webui.interfaces.IDeployWindow;
import test.webui.objects.LoginPage;
import test.webui.objects.services.PuTreeGrid;
import test.webui.objects.services.ServicesTab;
import test.webui.objects.services.PuTreeGrid.WebUIProcessingUnit;

public class MemcachedDeployTest extends AbstractSeleniumTest {
	
	Machine machineA;
	String PuName = "test-memcached";
	
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
	
	@Test(dataProvider = "TestParamProvider" ,timeOut = DEFAULT_TEST_TIMEOUT)
	public void memcachedDeployment(String spaceUrl, String isSecured, String userName, 
			String password, String numberOfInstances,
			String numberOfBackups, String clusterSchema, String maxInstPerVM,
			String maxInstPerMachine) throws InterruptedException {
		
		String puName = "test-memcached";
		
		// get new login page
		LoginPage loginPage = getLoginPage();
		
		// get new topology tab
		ServicesTab servicesTab = loginPage.login().switchToServices();
		
		LogUtils.log("doplying memcached...");
		// open the deployment window
		IDeployWindow deployMemcached = servicesTab.openMemcachedDeployWindow(spaceUrl, isSecured, userName, password, numberOfInstances, 
				numberOfBackups, clusterSchema, maxInstPerVM, maxInstPerMachine);
		
		// submit deployment specs
		deployMemcached.sumbitDeploySpecs();
		
		// deploy onto the grid
		deployMemcached.deploy();
		
		// wait for PU
		ProcessingUnit test = null;
		int seconds = 0;
		while (test == null) {
			if (seconds >= 30) Assert.fail("admin wasnt able to capture pu"); 
			test = admin.getProcessingUnits().getProcessingUnit(puName);
			seconds++;
			Thread.sleep(1000);
		}
		ProcessingUnitUtils.waitForDeploymentStatus(test, DeploymentStatus.INTACT);
		
		// close the deployment window
		deployMemcached.closeWindow();
		
		PuTreeGrid puGrid = servicesTab.getPuTreeGrid();
		
		// assert pu exists in pu tree grid
		WebUIProcessingUnit memcached = puGrid.getProcessingUnit(puName);
		
		assertTrue(memcached != null);
		
	}
	
	@DataProvider(name = "TestParamProvider") 
    public String[][] testParamProvider() throws XMLStreamException, FactoryConfigurationError, IOException {
		Iterator<List<String>> iter = new WebUiUtils.TestParamIterator(WebUiUtils.xmlPath);
		return WebUiUtils.getData(iter, "MemcachedDeployTest");	
    }
	
	
	
	

}
