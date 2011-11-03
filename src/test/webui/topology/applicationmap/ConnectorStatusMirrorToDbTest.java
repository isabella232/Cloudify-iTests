package test.webui.topology.applicationmap;

import static framework.utils.AdminUtils.loadGSC;
import static framework.utils.AdminUtils.loadGSM;
import static framework.utils.LogUtils.log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.openspaces.admin.alert.Alert;
import org.openspaces.admin.alert.AlertManager;
import org.openspaces.admin.alert.AlertStatus;
import org.openspaces.admin.alert.config.MirrorPersistenceFailureAlertConfiguration;
import org.openspaces.admin.alert.config.MirrorPersistenceFailureAlertConfigurer;
import org.openspaces.admin.alert.events.AlertTriggeredEventListener;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.DeploymentStatus;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitDeployment;
import org.openspaces.admin.space.SpaceInstance;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import framework.utils.AssertUtils;
import framework.utils.DBUtils;
import framework.utils.DeploymentUtils;
import framework.utils.LogUtils;
import framework.utils.ProcessingUnitUtils;
import framework.utils.ToStringUtils;
import framework.utils.AssertUtils.RepetitiveConditionProvider;

import test.webui.AbstractSeleniumTest;
import test.webui.objects.LoginPage;
import test.webui.objects.topology.ApplicationMap;
import test.webui.objects.topology.ApplicationMap.ApplicationNode;
import test.webui.objects.topology.ApplicationMap.ApplicationNode.Connector;
import test.webui.objects.topology.TopologyTab;

public class ConnectorStatusMirrorToDbTest extends AbstractSeleniumTest {
	
	private static final int HSQL_DB_PORT = 9876;
	private Machine machine;
	private int hsqlId;
	private GridServiceAgent gsa;
	private ProcessingUnit mirror;
	ProcessingUnit loader;
	GridServiceManager gsm;
	Thread feeder;
	ProcessingUnit runtime;
	
	@BeforeMethod(alwaysRun = true)
	public void startSetUp() {
		
		log("waiting for 1 GSA");
		gsa = admin.getGridServiceAgents().waitForAtLeastOne();
		machine = gsa.getMachine();
		
		log("loading GSM");
		gsm = loadGSM(machine);
		
		log("loading 1 GSC on 1 machine");
		loadGSC(machine);
		
        log("load HSQL DB on machine - "+ToStringUtils.machineToString(machine));
        hsqlId = DBUtils.loadHSQLDB(machine, "MirrorPersistFailureAlertTest", HSQL_DB_PORT);
        LogUtils.log("Loaded HSQL successfully, id ["+hsqlId+"]");
        
        log("deploy mirror via GSM");
        DeploymentUtils.prepareApp("MHEDS");
		mirror = gsm.deploy(new ProcessingUnitDeployment(DeploymentUtils.getProcessingUnit("MHEDS", "mirror")).
                setContextProperty("port", String.valueOf(HSQL_DB_PORT)).setContextProperty("host", machine.getHostAddress()));
		mirror.waitFor(mirror.getTotalNumberOfInstances());

        log("deploy runtime(1,1) via GSM");
        runtime = gsm.deploy(new ProcessingUnitDeployment(DeploymentUtils.getProcessingUnit("MHEDS", "runtime")).
                numberOfInstances(1).numberOfBackups(1).maxInstancesPerVM(0).setContextProperty("port", String.valueOf(HSQL_DB_PORT)).
                setContextProperty("host", machine.getHostAddress()));
        runtime.waitFor(runtime.getTotalNumberOfInstances());
        ProcessingUnitUtils.waitForDeploymentStatus(runtime, DeploymentStatus.INTACT);
        
  
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT)
	public void mirrorPersist() throws InterruptedException {
		
		// get new login page
		LoginPage loginPage = getLoginPage();
		
		// get new topology tab
		TopologyTab topologyTab = loginPage.login().switchToTopology();
		
		log("deploy loader via GSM");
		loader = gsm.deploy(new ProcessingUnitDeployment(DeploymentUtils.getProcessingUnit("MHEDS", "loader"))
		.setContextProperty("accounts", String.valueOf(10000)).setContextProperty("delay", "100"));
		loader.waitFor(loader.getTotalNumberOfInstances());
		
		final ApplicationMap applicationMap = topologyTab.getApplicationMap();
		
		RepetitiveConditionProvider condition = new RepetitiveConditionProvider() {

			public boolean getCondition() {
				ApplicationNode mirrorNode = applicationMap.getApplicationNode("mirror");
				List<Connector> mirrorConnectors = mirrorNode.getConnectors();
				assertTrue(mirrorConnectors.size() == 1);
				Connector conn = mirrorConnectors.get(0);
				System.out.println(conn.getStatus());
				if (conn.getStatus().equals(ApplicationMap.CONN_STATUS_OK)) return true;
				return false;
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition,waitingTime);
		
		LogUtils.log("Waiting for mirror to process at least 1000 accounts");
		mirror.getSpace().startStatisticsMonitor();
		final SpaceInstance mirrorInstance = mirror.getSpace().getInstances()[0];

		long mirrorStatisticsSuccessfulOperationCount = 0;
		while (mirrorStatisticsSuccessfulOperationCount < 500) {
			mirrorStatisticsSuccessfulOperationCount = mirrorInstance.getStatistics().getMirrorStatistics().getSuccessfulOperationCount();
			Thread.sleep(100);
		}
		
		LogUtils.log("Mirror processed " + mirrorStatisticsSuccessfulOperationCount + " accounts");
		
		final List<Alert> adminAlerts = new ArrayList<Alert>();
		LogUtils.log("Register for Mirror Persist Failure alerts");
		final AlertManager alertManager = admin.getAlertManager();
		alertManager.setConfig(new MirrorPersistenceFailureAlertConfigurer().create());       
		alertManager.enableAlert(MirrorPersistenceFailureAlertConfiguration.class);
		
		final CountDownLatch alertResolved = new CountDownLatch(1);
		final CountDownLatch alertRaised = new CountDownLatch(1);
		alertManager.getAlertTriggered().add(new AlertTriggeredEventListener() {
			public void alertTriggered(Alert alert) {
				adminAlerts.add(alert);
				if (alert.getStatus().equals(AlertStatus.RESOLVED)) alertResolved.countDown();
				if (alert.getStatus().equals(AlertStatus.RAISED)) alertRaised.countDown();
				log(alert.getDescription());
			}
		});
		
		LogUtils.log("Killing HSQL process");
		gsa.killByAgentId(hsqlId);
		
		Thread.sleep(10000);
		
		LogUtils.log("Waiting for alert raised event");
		alertRaised.await(60, TimeUnit.SECONDS);
		assertTrue(alertRaised.getCount() == 0);
		
		condition = new RepetitiveConditionProvider() {

			public boolean getCondition() {
				ApplicationNode mirrorNode = applicationMap.getApplicationNode("mirror");
				List<Connector> mirrorConnectors = mirrorNode.getConnectors();
				assertTrue(mirrorConnectors.size() == 1);
				Connector conn = mirrorConnectors.get(0);
				System.out.println(conn.getStatus());
				if (conn.getStatus().equals(ApplicationMap.CONN_STATUS_CRITICAL)) return true;
				return false;
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition,waitingTime);
		
		long lastSuccessfulOperationCount = mirrorInstance.getStatistics().getMirrorStatistics().getSuccessfulOperationCount();

		log("load HSQL DB on machine - "+ToStringUtils.machineToString(machine));
        hsqlId = DBUtils.loadHSQLDB(machine, "MirrorPersistFailureAlertTest", HSQL_DB_PORT);
        LogUtils.log("Loaded HSQL successfully, id ["+hsqlId+"]");
        
        LogUtils.log("Waiting for alert resolved event");
        alertResolved.await(60, TimeUnit.SECONDS);
        assertTrue(alertResolved.getCount() == 0);
        
        condition = new RepetitiveConditionProvider() {

			public boolean getCondition() {
				ApplicationNode mirrorNode = applicationMap.getApplicationNode("mirror");
				List<Connector> mirrorConnectors = mirrorNode.getConnectors();
				assertTrue(mirrorConnectors.size() == 1);
				Connector conn = mirrorConnectors.get(0);
				System.out.println(conn.getStatus());
				if (conn.getStatus().equals(ApplicationMap.CONN_STATUS_OK)) return true;
				return false;
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition,waitingTime);

		LogUtils.log("Mirror processed " + lastSuccessfulOperationCount + " accounts, waiting for replication to reconnect");
		mirrorStatisticsSuccessfulOperationCount = 0;
		while (mirrorStatisticsSuccessfulOperationCount < lastSuccessfulOperationCount+500) {
			mirrorStatisticsSuccessfulOperationCount = mirrorInstance.getStatistics().getMirrorStatistics().getSuccessfulOperationCount();
			Thread.sleep(100);
		}
		
		LogUtils.log("Mirror processed " + mirrorInstance.getStatistics().getMirrorStatistics().getSuccessfulOperationCount() + " accounts");
		LogUtils.log("OK.");	
	}
}
