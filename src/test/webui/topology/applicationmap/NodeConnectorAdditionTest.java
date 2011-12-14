package test.webui.topology.applicationmap;

import static framework.utils.AdminUtils.loadGSM;
import static framework.utils.LogUtils.log;

import java.util.List;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.DeploymentStatus;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitDeployment;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.webui.AbstractSeleniumTest;
import test.webui.objects.LoginPage;
import test.webui.objects.topology.TopologyTab;
import test.webui.objects.topology.applicationmap.ApplicationMap;
import test.webui.objects.topology.applicationmap.Connector;
import framework.utils.AdminUtils;
import framework.utils.AssertUtils.RepetitiveConditionProvider;
import framework.utils.DBUtils;
import framework.utils.DeploymentUtils;
import framework.utils.LogUtils;
import framework.utils.ProcessingUnitUtils;
import framework.utils.ToStringUtils;

public class NodeConnectorAdditionTest extends AbstractSeleniumTest {
	
	private Machine machine;
	private GridServiceAgent gsa;
	private GridServiceManager gsm;
	private ProcessingUnit mirror;
	private ProcessingUnit runtime;
	private ProcessingUnit loader;
	private int hsqlId;
	private static final int HSQL_DB_PORT = 9876;
	private final String CONTEXT_PROPERTY_KEY_APPLICATION = "com.gs.application";
	private final String CONTEXT_PROPERTY_DEPENDS_ON = "com.gs.application.dependsOn";
	private final String APPLICATION_NAME = "MyApp";


	@BeforeMethod(alwaysRun = true)
	public void startSetup() throws Exception {
		
		log("waiting for 1 GSA");
		gsa = admin.getGridServiceAgents().waitForAtLeastOne();
		machine = gsa.getMachine();
		
		log("loading GSM");
		gsm = loadGSM(machine);
		
		log("loading 1 GSC on 1 machine");
		AdminUtils.loadGSCs(machine, 2);
		
        log("load HSQL DB on machine - "+ToStringUtils.machineToString(machine));
        hsqlId = DBUtils.loadHSQLDB(machine, "NodeConnectorAdditionTest", HSQL_DB_PORT);
        LogUtils.log("Loaded HSQL successfully, id ["+hsqlId+"]");
        
        log("deploy mirror via GSM");
        DeploymentUtils.prepareApp("MHEDS");
		mirror = gsm.deploy(new ProcessingUnitDeployment(DeploymentUtils.getProcessingUnit("MHEDS", "mirror")).
                setContextProperty("port", String.valueOf(HSQL_DB_PORT)).setContextProperty("host", machine.getHostAddress()).setContextProperty(CONTEXT_PROPERTY_KEY_APPLICATION, APPLICATION_NAME));
		mirror.waitFor(mirror.getTotalNumberOfInstances());

	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT)
	public void connectorTest() throws InterruptedException {
		
		// get new login page
		LoginPage loginPage = getLoginPage();
		
		// get new topology tab
		TopologyTab topologyTab = loginPage.login().switchToTopology();
		
		final ApplicationMap applicationMap = topologyTab.getApplicationMap();
		
		applicationMap.selectApplication(APPLICATION_NAME);
		
		List<Connector> mirrorConnectors = applicationMap.getApplicationNode("mirror").getConnectors();
		assertTrue(mirrorConnectors.size() == 1);
		
        log("deploy runtime(1,1) via GSM");
        runtime = gsm.deploy(new ProcessingUnitDeployment(DeploymentUtils.getProcessingUnit("MHEDS", "runtime")).
                numberOfInstances(1).numberOfBackups(1).maxInstancesPerVM(0).setContextProperty("port", String.valueOf(HSQL_DB_PORT)).setContextProperty(CONTEXT_PROPERTY_DEPENDS_ON, "[mirror]").
                setContextProperty("host", machine.getHostAddress()).setContextProperty(CONTEXT_PROPERTY_KEY_APPLICATION, APPLICATION_NAME));
        runtime.waitFor(runtime.getTotalNumberOfInstances());
        ProcessingUnitUtils.waitForDeploymentStatus(runtime, DeploymentStatus.INTACT);
        
		RepetitiveConditionProvider condition = new RepetitiveConditionProvider() {

			@Override
			public boolean getCondition() {

				List<Connector> mirrorConnectors = applicationMap.getApplicationNode("mirror").getConnectors();
				if (mirrorConnectors.size() == 2) {
					for (Connector c : mirrorConnectors) {
						if (c.getSource().getName().contains("runtime")) {
							return (c.getTarget().getName().equals("mirror"));
						}
					}
					return false;
				}
				else return false;
			}
		};
		
		repetitiveAssertTrue(null, condition, 3000);
        
        log("deploy loader via GSM");
        loader = gsm.deploy(new ProcessingUnitDeployment(DeploymentUtils.getProcessingUnit("MHEDS", "loader")).setContextProperty(CONTEXT_PROPERTY_DEPENDS_ON, "[runtime]")
        .setContextProperty("accounts", String.valueOf(10000)).setContextProperty("delay", "100").setContextProperty(CONTEXT_PROPERTY_KEY_APPLICATION, APPLICATION_NAME));
        loader.waitFor(loader.getTotalNumberOfInstances());
        ProcessingUnitUtils.waitForDeploymentStatus(loader, DeploymentStatus.INTACT);
		
        
		condition = new RepetitiveConditionProvider() {

			@Override
			public boolean getCondition() {
				if (applicationMap.getApplicationNode("loader") == null) return false;
				List<Connector> loaderConnectors = applicationMap.getApplicationNode("loader").getConnectors();
				if (loaderConnectors.size() == 1) {
					Connector connector = loaderConnectors.get(0);
					return ((connector.getSource().getName().equals(loader.getName())) 
						&&(connector.getTarget().getName().equals(runtime.getName())));
				}
				else return false;
			}
		};
		
		repetitiveAssertTrue(null, condition, 3000);
	}

}
