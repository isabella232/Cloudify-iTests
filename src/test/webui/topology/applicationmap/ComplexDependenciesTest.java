package test.webui.topology.applicationmap;

import static framework.utils.AdminUtils.loadGSM;
import static framework.utils.LogUtils.log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.DeploymentStatus;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitDeployment;
import org.openspaces.admin.space.SpaceDeployment;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.usm.USMTestUtils;
import test.webui.AbstractSeleniumTest;
import test.webui.objects.LoginPage;
import test.webui.objects.topology.ApplicationMap;
import test.webui.objects.topology.TopologyTab;
import test.webui.objects.topology.ApplicationMap.ServiceTypes;
import test.webui.objects.topology.ApplicationMap.ApplicationNode.Connector;

import com.gigaspaces.cloudify.dsl.internal.packaging.PackagingException;

import framework.utils.AdminUtils;
import framework.utils.DBUtils;
import framework.utils.DeploymentUtils;
import framework.utils.LogUtils;
import framework.utils.ProcessingUnitUtils;
import framework.utils.ToStringUtils;

public class ComplexDependenciesTest extends AbstractSeleniumTest {
	
	private Machine machineA;
	private ProcessingUnit pu;
	private String appName = "MyApp";
	
	private static final int HSQL_DB_PORT = 9876;
	private int hsqlId;


	@BeforeMethod(alwaysRun = true)
	public void startSetup() throws IOException, PackagingException {
		log("waiting for 1 machine");
		admin.getMachines().waitFor(1);

		log("waiting for 1 GSA");
		admin.getGridServiceAgents().waitFor(1);

		GridServiceAgent[] agents = admin.getGridServiceAgents().getAgents();
		GridServiceAgent gsaA = agents[0];

		machineA = gsaA.getMachine();

		log("starting: 1 GSM and 7 GSC's on 1 machine");
		GridServiceManager gsmA = loadGSM(machineA); 
		for (int i = 0 ; i < 7 ; i ++) {
			AdminUtils.loadGSCWithSystemProperty(machineA, "-Xmx128m");
		}
		
		LogUtils.log("deploying cassandra mock");
		File usmJar = USMTestUtils.usmCreateJar("/apps/USM/usm/simplejavaprocess");
		ProcessingUnit deployment = gsmA.deploy(new ProcessingUnitDeployment(
				usmJar).setContextProperty(SUB_TYPE_CONTEXT_PROPERTY,
				ServiceTypes.NOSQL_DB.toString()).
				setContextProperty(APPLICATION_CONTEXT_PROPERY, appName).
				name("cassandra"));        
		ProcessingUnit simple = admin.getProcessingUnits().waitFor(deployment.getName());
        ProcessingUnitUtils.waitForDeploymentStatus(simple, DeploymentStatus.INTACT);

		
        log("load HSQL DB on machine - "+ToStringUtils.machineToString(machineA));
        hsqlId = DBUtils.loadHSQLDB(machineA, "MirrorPersistFailureAlertTest", HSQL_DB_PORT);
        LogUtils.log("Loaded HSQL successfully, id ["+hsqlId+"]");
        
        log("deploying mirror mock");
        DeploymentUtils.prepareApp("MHEDS");
		ProcessingUnit mirror = gsmA.deploy(new ProcessingUnitDeployment(DeploymentUtils.getProcessingUnit("MHEDS", "mirror")).
                setContextProperty("port", String.valueOf(HSQL_DB_PORT)).setContextProperty("host", machineA.getHostAddress()).
                setContextProperty(APPLICATION_CONTEXT_PROPERY, appName).
                setContextProperty(DEPENDS_ON_CONTEXT_PROPERTY, "cassandra")
                .name("stockAnalyticsMirror"));
		ProcessingUnitUtils.waitForDeploymentStatus(mirror, DeploymentStatus.INTACT);
		
		LogUtils.log("deploying stockAnalyticsSpace Mock unit...");
		SpaceDeployment spaceDeployment = new SpaceDeployment("stockAnalyticsSpace").partitioned(1, 0).maxInstancesPerVM(1);
		pu = gsmA.deploy(spaceDeployment.setContextProperty(APPLICATION_CONTEXT_PROPERY, appName).
				setContextProperty(DEPENDS_ON_CONTEXT_PROPERTY, "stockAnalyticsMirror"));
		ProcessingUnitUtils.waitForDeploymentStatus(pu, DeploymentStatus.INTACT);
		
		LogUtils.log("deploying stockAnalyticsProcessor Mock unit...");
		spaceDeployment = new SpaceDeployment("stockAnalyticsProcessor").partitioned(1, 0).maxInstancesPerVM(1);
		pu = gsmA.deploy(spaceDeployment.setContextProperty(APPLICATION_CONTEXT_PROPERY, appName).
				setContextProperty(DEPENDS_ON_CONTEXT_PROPERTY, "stockAnalyticsSpace"));
		ProcessingUnitUtils.waitForDeploymentStatus(pu, DeploymentStatus.INTACT);
		
		LogUtils.log("deploying StockDemo mock unit...");
		ProcessingUnit puSessionTest = gsmA.deploy(new ProcessingUnitDeployment(DeploymentUtils.getArchive("session-test-embedded.war")).
				setContextProperty(APPLICATION_CONTEXT_PROPERY, appName).
				setContextProperty(DEPENDS_ON_CONTEXT_PROPERTY, "[stockAnalyticsProcessor,stockAnalyticsSpace]").name("StockDemo"));
		ProcessingUnitUtils.waitForDeploymentStatus(puSessionTest, DeploymentStatus.INTACT);
		
		LogUtils.log("deploying stockAnalytics Mock unit...");
		spaceDeployment = new SpaceDeployment("stockAnalytics").partitioned(1, 0).maxInstancesPerVM(1);
		pu = gsmA.deploy(spaceDeployment.setContextProperty(APPLICATION_CONTEXT_PROPERY, appName).
				setContextProperty(DEPENDS_ON_CONTEXT_PROPERTY, "stockAnalyticsSpace"));
		ProcessingUnitUtils.waitForDeploymentStatus(pu, DeploymentStatus.INTACT);

		LogUtils.log("deploying stockAnalyticsFeeder Mock unit...");
		spaceDeployment = new SpaceDeployment("stockAnalyticsFeeder").partitioned(1, 0).maxInstancesPerVM(1);
		pu = gsmA.deploy(spaceDeployment.setContextProperty(APPLICATION_CONTEXT_PROPERY, appName).
				setContextProperty(DEPENDS_ON_CONTEXT_PROPERTY, "stockAnalyticsProcessor"));
		ProcessingUnitUtils.waitForDeploymentStatus(pu, DeploymentStatus.INTACT);
		
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = {"cloudify" , "xap"})
	public void antualInstancesTest() throws InterruptedException {
		
		// get new login page
		LoginPage loginPage = getLoginPage();
		
		TopologyTab topologyTab = loginPage.login().switchToTopology();
		
		final ApplicationMap appMap = topologyTab.getApplicationMap();
				
		appMap.selectApplication(appName);
		
		takeScreenShot(this.getClass(), "stockdemomock");
		
		List<Connector> cassandraConnectors = appMap.getApplicationNode("cassandra").getConnectors();
		assertTrue(cassandraConnectors.size() == 1);
		Connector connector = cassandraConnectors.get(0);
		assertTrue(connector.getSource().getName().equals("stockAnalyticsMirror"));
		
		List<Connector> stockAnalyticsMirrorConnectors = appMap.getApplicationNode("stockAnalyticsMirror").getConnectors();
		assertTrue(stockAnalyticsMirrorConnectors.size() == 2);
		for (Connector c : stockAnalyticsMirrorConnectors) {
			if (c.getSource().getName().equals("stockAnalyticsMirror")) {
				assertTrue(c.getTarget().getName().equals("cassandra"));
			}
			if (c.getTarget().getName().equals("stockAnalyticsMirror")) {
				assertTrue(c.getSource().getName().equals("stockAnalyticsSpace"));
				
			}
		}
		
		List<Connector> stockAnalyticsSpaceConnectors = appMap.getApplicationNode("stockAnalyticsSpace").getConnectors();
		assertTrue(stockAnalyticsSpaceConnectors.size() == 4);
		List<String> sourceNodeNames = new ArrayList<String>();
		for (Connector c : stockAnalyticsSpaceConnectors) {
			if (c.getSource().getName().equals("stockAnalyticsSpace")) {
				assertTrue(c.getTarget().getName().equals("stockAnalyticsMirror"));
			}
			if (c.getTarget().getName().equals("stockAnalyticsSpace")) {
				sourceNodeNames.add(c.getSource().getName());
			}
		}
		assertTrue(sourceNodeNames.size() == 3);
		assertTrue(sourceNodeNames.contains("stockAnalyticsProcessor"));
		assertTrue(sourceNodeNames.contains("StockDemo"));
		assertTrue(sourceNodeNames.contains("stockAnalytics"));
		
		List<Connector> stockAnalyticsProcessorConnectors = appMap.getApplicationNode("stockAnalyticsProcessor").getConnectors();
		assertTrue(stockAnalyticsProcessorConnectors.size() == 3);
		sourceNodeNames = new ArrayList<String>();
		for (Connector c : stockAnalyticsProcessorConnectors) {
			if (c.getSource().getName().equals("stockAnalyticsProcessor")) {
				assertTrue(c.getTarget().getName().equals("stockAnalyticsSpace"));
			}
			if (c.getTarget().getName().equals("stockAnalyticsProcessor")) {
				sourceNodeNames.add(c.getSource().getName());
			}
		}
		assertTrue(sourceNodeNames.size() == 2);
		assertTrue(sourceNodeNames.contains("stockAnalyticsFeeder"));
		assertTrue(sourceNodeNames.contains("StockDemo"));
		
		
		List<Connector> stockDemoConnectors = appMap.getApplicationNode("StockDemo").getConnectors();
		assertTrue(stockDemoConnectors.size() == 2);
		List<String> targetNodeNames = new ArrayList<String>();
		for (Connector c : stockDemoConnectors) {
			if (c.getSource().getName().equals("StockDemo")) {
				targetNodeNames.add(c.getTarget().getName());
			}
		}
		assertTrue(targetNodeNames.size() == 2);
		assertTrue(targetNodeNames.contains("stockAnalyticsProcessor"));
		assertTrue(targetNodeNames.contains("stockAnalyticsSpace"));
		
		List<Connector> stockAnalyticsConnectors = appMap.getApplicationNode("stockAnalytics").getConnectors();
		assertTrue(stockAnalyticsConnectors.size() == 1);
		connector = stockAnalyticsConnectors.get(0);
		assertTrue(connector.getTarget().getName().equals("stockAnalyticsSpace"));
		
		List<Connector> stockAnalyticsFeederConnectors = appMap.getApplicationNode("stockAnalyticsFeeder").getConnectors();
		assertTrue(stockAnalyticsFeederConnectors.size() == 1);
		connector = stockAnalyticsFeederConnectors.get(0);
		assertTrue(connector.getTarget().getName().equals("stockAnalyticsProcessor"));		
		
	}

}
