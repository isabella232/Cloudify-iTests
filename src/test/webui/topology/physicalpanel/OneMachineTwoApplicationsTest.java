package test.webui.topology.physicalpanel;

import static framework.utils.AdminUtils.loadGSM;
import static framework.utils.LogUtils.log;

import org.openspaces.admin.application.Application;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.DeploymentStatus;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitDeployment;
import org.openspaces.admin.space.SpaceDeployment;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import framework.utils.AdminUtils;
import framework.utils.DBUtils;
import framework.utils.DeploymentUtils;
import framework.utils.LogUtils;
import framework.utils.ProcessingUnitUtils;
import framework.utils.ToStringUtils;

import test.webui.AbstractSeleniumTest;
import test.webui.objects.LoginPage;
import test.webui.objects.topology.ApplicationMap;
import test.webui.objects.topology.PhysicalPanel;
import test.webui.objects.topology.PhysicalPanel.Host;
import test.webui.objects.topology.PhysicalPanel.Host.PuIBox;
import test.webui.objects.topology.PhysicalPanel.Host.PuIBoxes;
import test.webui.objects.topology.PhysicalPanel.OS;
import test.webui.objects.topology.TopologyTab;

/**
 * Deploys two different applications on a single machine. 
 * test asserts that the physical view of the host is showing correct view of pu's of both applications
 * @author elip
 *
 */

public class OneMachineTwoApplicationsTest extends AbstractSeleniumTest {
	
	private static final int HSQL_DB_PORT = 9876;
	private Machine machine;
	private int hsqlId;
	private GridServiceAgent gsa;
	private ProcessingUnit mirror;
	ProcessingUnit loader;
	ProcessingUnit runtime;
	ProcessingUnit puSessionTest;
	ProcessingUnit mySpacePu;
	ProcessingUnit test;
	private String mirrorApp = "MirrorApp";
	private String webApp = "WebRemoteSpaceApp";
	
	@BeforeMethod(alwaysRun = true)
	public void startSetUp() {
		
		log("waiting for 1 GSA");
		gsa = admin.getGridServiceAgents().waitForAtLeastOne();
		machine = gsa.getMachine();
		
		log("loading GSM");
		GridServiceManager gsm = loadGSM(machine);
		
		log("loading 1 GSC on 1 machine");
		AdminUtils.loadGSCs(machine, 4);
		
		LogUtils.log("Deploying application with mirror : " + mirrorApp);
		
        log("load HSQL DB on machine - "+ToStringUtils.machineToString(machine));
        hsqlId = DBUtils.loadHSQLDB(machine, "MirrorPersistFailureAlertTest", HSQL_DB_PORT);
        LogUtils.log("Loaded HSQL successfully, id ["+hsqlId+"]");
        
        log("deploy mirror via GSM");
        DeploymentUtils.prepareApp("MHEDS");
		mirror = gsm.deploy(new ProcessingUnitDeployment(DeploymentUtils.getProcessingUnit("MHEDS", "mirror")).
                setContextProperty("port", String.valueOf(HSQL_DB_PORT)).setContextProperty("host", machine.getHostAddress()).setContextProperty("com.gs.application", mirrorApp));
		mirror.waitFor(mirror.getTotalNumberOfInstances());

        log("deploy runtime(1,1) via GSM");
        runtime = gsm.deploy(new ProcessingUnitDeployment(DeploymentUtils.getProcessingUnit("MHEDS", "runtime")).
                numberOfInstances(1).numberOfBackups(1).maxInstancesPerVM(0).setContextProperty("port", String.valueOf(HSQL_DB_PORT)).
                setContextProperty("host", machine.getHostAddress()).setContextProperty("com.gs.application", mirrorApp));
        runtime.waitFor(runtime.getTotalNumberOfInstances());
        ProcessingUnitUtils.waitForDeploymentStatus(runtime, DeploymentStatus.INTACT);
        
        log("deploy loader via GSM");
        loader = gsm.deploy(new ProcessingUnitDeployment(DeploymentUtils.getProcessingUnit("MHEDS", "loader"))
        .setContextProperty("accounts", String.valueOf(10000)).setContextProperty("delay", "100").setContextProperty("com.gs.application", mirrorApp));
        loader.waitFor(loader.getTotalNumberOfInstances());
        
        LogUtils.log("Deploying web application with remote space : " + webApp);
        
        LogUtils.log("deploying mySpace");
		SpaceDeployment deployment = new SpaceDeployment("mySpace").partitioned(1, 1).maxInstancesPerVM(1).setContextProperty("com.gs.application", webApp);
		mySpacePu = gsm.deploy(deployment);
		ProcessingUnitUtils.waitForDeploymentStatus(mySpacePu, DeploymentStatus.INTACT);
    	
		LogUtils.log("deploying web app remote");
		puSessionTest = gsm.deploy(new ProcessingUnitDeployment(DeploymentUtils.getArchive("session-test-remote.war")).setContextProperty("com.gs.application", webApp));
		ProcessingUnitUtils.waitForDeploymentStatus(puSessionTest, DeploymentStatus.INTACT);

	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT)
	public void test() throws InterruptedException {
		
		// get new login page
		LoginPage loginPage = getLoginPage();

		// get new topology tab
		TopologyTab topologyTab = loginPage.login().switchToTopology();
		
		Application webApplication = admin.getApplications().getApplication(webApp);
		Application mirroApplication = admin.getApplications().getApplication(mirrorApp);
		
		ApplicationMap appMap = topologyTab.getApplicationMap();
		
		appMap.selectApplication(mirrorApp);
		
		PhysicalPanel physicalPanel = topologyTab.getTopologySubPanel().switchToPhysicalPanel();
		
		Host me = physicalPanel.getHost(machine.getHostName());
		
		assertTrue(me.getGSMCount() == admin.getGridServiceManagers().getManagers().length);
		assertTrue(me.getGSCCount() == admin.getGridServiceContainers().getContainers().length);
		assertTrue(me.getOS().equals(OS.WINDOWS32));
		
		PuIBoxes puis = me.getPUIs();		
		
		assertTrue(puis.getPuIBoxesOfAdifferentApplication().size() == webApplication.getProcessingUnits().getProcessingUnits().length);
		
		int totalInstancesWebFromUI = 0;
		for (PuIBox p : puis.getPuIBoxesOfAdifferentApplication()) {
			totalInstancesWebFromUI += p.getNumberOfInstances();
		}
		int totalInstancesWebFromAdmin = 0;
		for (ProcessingUnit p : webApplication.getProcessingUnits()) {
			totalInstancesWebFromAdmin += p.getInstances().length;
		}
		assertTrue(totalInstancesWebFromAdmin == totalInstancesWebFromUI);
		
		appMap.selectApplication(webApp);
		
		me = physicalPanel.getHost(machine.getHostName());
		
		assertTrue(me.getGSMCount() == admin.getGridServiceManagers().getManagers().length);
		assertTrue(me.getGSCCount() == admin.getGridServiceContainers().getContainers().length);
		assertTrue(me.getOS().equals(OS.WINDOWS32));
		
		puis = me.getPUIs();
		
		assertTrue(puis.getPuIBoxesOfAdifferentApplication().size() == mirroApplication.getProcessingUnits().getProcessingUnits().length);
		totalInstancesWebFromUI = 0;
		for (PuIBox p : puis.getPuIBoxesOfAdifferentApplication()) {
			totalInstancesWebFromUI += p.getNumberOfInstances();
		}
		totalInstancesWebFromAdmin = 0;
		for (ProcessingUnit p : mirroApplication.getProcessingUnits()) {
			totalInstancesWebFromAdmin += p.getInstances().length;
		}
		assertTrue(totalInstancesWebFromAdmin == totalInstancesWebFromUI);
		
		// TODO eli - after eliran adds id's to pu boxes. change the assertions to compare each pu boxes
		// number of instances to the cooresponding pu.getInstances().length
	}

}
