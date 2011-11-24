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
import org.openspaces.admin.alert.config.ReplicationChannelDisconnectedAlertConfiguration;
import org.openspaces.admin.alert.config.ReplicationChannelDisconnectedAlertConfigurer;
import org.openspaces.admin.alert.events.AlertTriggeredEventListener;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.DeploymentStatus;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitDeployment;
import org.openspaces.admin.space.Space;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.webui.AbstractSeleniumTest;
import test.webui.objects.LoginPage;
import test.webui.objects.topology.ApplicationMap;
import test.webui.objects.topology.ApplicationMap.ApplicationNode;
import test.webui.objects.topology.ApplicationMap.ApplicationNode.Connector;
import test.webui.objects.topology.TopologyTab;
import framework.utils.AssertUtils;
import framework.utils.AssertUtils.RepetitiveConditionProvider;
import framework.utils.DBUtils;
import framework.utils.DeploymentUtils;
import framework.utils.LogUtils;
import framework.utils.ProcessingUnitUtils;
import framework.utils.ToStringUtils;

public class ConnectorStatusClusterToMirrorTest extends AbstractSeleniumTest {

	private static final int HSQL_DB_PORT = 9876;
	private Machine machine;
	private int hsqlId;
	private GridServiceAgent gsa;
	private GridServiceContainer gsc;
	private ProcessingUnit mirror;
	ProcessingUnit loader;
	GridServiceManager gsm;
	Thread feeder;
	ProcessingUnit runtime;
	private static final String PU_NAME_MIRROR = "mirror";
	private static final String PU_NAME_RUNTIME = "runtime";
	private static final String PU_NAME_LOADER = "loader";
	private static final String STATIC_GSC_ZONES = PU_NAME_RUNTIME + "," + PU_NAME_LOADER;
	private static final String DYNAMIC_GSC_ZONES = PU_NAME_MIRROR;

	@BeforeMethod(alwaysRun = true)
	public void startSetUp() {

		log("waiting for 1 GSA");
		gsa = admin.getGridServiceAgents().waitForAtLeastOne();
		machine = gsa.getMachine();

		log("loading GSM");
		gsm = loadGSM(machine);

		log("loading 1 GSC [zones='" + STATIC_GSC_ZONES + "']");
		loadGSC(gsa, STATIC_GSC_ZONES);
 
		log("loading 1 GSC [zones='" + DYNAMIC_GSC_ZONES + "']");
		gsc = loadGSC(gsa, DYNAMIC_GSC_ZONES);

        log("load HSQL DB on machine - " + ToStringUtils.machineToString(machine));
        hsqlId = DBUtils.loadHSQLDB(machine, "ReplicationChannelDisconnectedAlertTest", HSQL_DB_PORT);
        LogUtils.log("Loaded HSQL successfully [id='" + hsqlId + "']");

        log("deploy mirror via GSM [zones='" + PU_NAME_MIRROR + "']");
        DeploymentUtils.prepareApp("MHEDS");
		mirror = gsm.deploy(new ProcessingUnitDeployment(
				DeploymentUtils.getProcessingUnit("MHEDS", PU_NAME_MIRROR))
				.maxInstancesPerVM(0)
				.addZone(PU_NAME_MIRROR)
				.setContextProperty("port", String.valueOf(HSQL_DB_PORT))
				.setContextProperty("host", machine.getHostAddress()));
		mirror.waitFor(mirror.getTotalNumberOfInstances());

        log("deploy runtime(1,1) via GSM [zones='" + PU_NAME_RUNTIME + "']");
        runtime = gsm.deploy(new ProcessingUnitDeployment(
        		DeploymentUtils.getProcessingUnit("MHEDS", PU_NAME_RUNTIME))
        		.numberOfInstances(1)
        		.numberOfBackups(1)
        		.addZone(PU_NAME_RUNTIME)
        		.setContextProperty("port", String.valueOf(HSQL_DB_PORT))
        		.setContextProperty("host", machine.getHostAddress())
        		.setContextProperty(DEPENDS_ON_CONTEXT_PROPERTY, PU_NAME_MIRROR));
        runtime.waitFor(runtime.getTotalNumberOfInstances());

		log("deploy loader via GSM [zones='" + PU_NAME_LOADER + "']");
		loader = gsm.deploy(new ProcessingUnitDeployment(
				DeploymentUtils.getProcessingUnit("MHEDS", PU_NAME_LOADER))
				.addZone(PU_NAME_LOADER)
				.setContextProperty(DEPENDS_ON_CONTEXT_PROPERTY, PU_NAME_RUNTIME)
				.setContextProperty("accounts", String.valueOf(10000))
				.setContextProperty("delay", "100"));
		loader.waitFor(loader.getTotalNumberOfInstances());

		ProcessingUnitUtils.waitForDeploymentStatus(loader, DeploymentStatus.INTACT);
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT)
	public void mirrorPersist() throws InterruptedException {
		
		// get new login page
		LoginPage loginPage = getLoginPage();
		
		// get new topology tab
		TopologyTab topologyTab = loginPage.login().switchToTopology();

		final ApplicationMap applicationMap = topologyTab.getApplicationMap();

		Thread.sleep(2000);

		RepetitiveConditionProvider condition = new RepetitiveConditionProvider() {

			public boolean getCondition() {
				ApplicationNode mirrorNode = applicationMap.getApplicationNode(PU_NAME_MIRROR);
				List<Connector> mirrorTargetedConnectors = mirrorNode.getTargeted();
				assertTrue(mirrorTargetedConnectors.size() == 1);
				Connector conn = mirrorTargetedConnectors.get(0);
				if (conn.getStatus().equals(ApplicationMap.CONN_STATUS_OK)) return true;
				return false;
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition,waitingTime);


		final List<Alert> adminAlerts = new ArrayList<Alert>();
		LogUtils.log("Register for Replication Channel Disconnected alerts");
		final AlertManager alertManager = admin.getAlertManager();
		alertManager.setConfig(new ReplicationChannelDisconnectedAlertConfigurer().create());       
		alertManager.enableAlert(ReplicationChannelDisconnectedAlertConfiguration.class);
		
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
		
		log("Killing GSC (zones='" + DYNAMIC_GSC_ZONES + "')");
		gsc.kill();
		
		LogUtils.log("Waiting for alert raised event");
		alertRaised.await(60, TimeUnit.SECONDS);
		assertTrue(alertRaised.getCount() == 0);
		
		condition = new RepetitiveConditionProvider() {

			public boolean getCondition() {
				ApplicationNode mirrorNode = applicationMap.getApplicationNode(PU_NAME_MIRROR);
				List<Connector> mirrorTargetedConnectors = mirrorNode.getTargeted();
				assertTrue(mirrorTargetedConnectors.size() == 1);
				Connector conn = mirrorTargetedConnectors.get(0);
				if (conn.getStatus().equals(ApplicationMap.CONN_STATUS_CRITICAL)) return true;
				return false;
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition,waitingTime);
		
		
		LogUtils.log("Reloading GSC (zones='" + DYNAMIC_GSC_ZONES + "')");
		loadGSC(gsa, DYNAMIC_GSC_ZONES);

        LogUtils.log("Waiting for alert resolved event");
        alertResolved.await(60, TimeUnit.SECONDS);
        assertTrue(alertResolved.getCount() == 0);
        
        condition = new RepetitiveConditionProvider() {

			public boolean getCondition() {
				ApplicationNode mirrorNode = applicationMap.getApplicationNode(PU_NAME_MIRROR);
				List<Connector> mirrorTargetedConnectors = mirrorNode.getTargeted();
				assertTrue(mirrorTargetedConnectors.size() == 1);
				Connector conn = mirrorTargetedConnectors.get(0);
				if (conn.getStatus().equals(ApplicationMap.CONN_STATUS_OK)) return true;
				return false;
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition,waitingTime);

	}
}
