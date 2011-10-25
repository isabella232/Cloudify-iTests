package test.alerts;

import static test.utils.AdminUtils.loadGSC;
import static test.utils.AdminUtils.loadGSM;
import static test.utils.LogUtils.log;

import java.util.concurrent.CountDownLatch;

import org.openspaces.admin.alert.Alert;
import org.openspaces.admin.alert.AlertManager;
import org.openspaces.admin.alert.AlertStatus;
import org.openspaces.admin.alert.alerts.MirrorPersistenceFailureAlert;
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
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.AbstractTest;
import test.utils.DBUtils;
import test.utils.DeploymentUtils;
import test.utils.LogUtils;
import test.utils.ProcessingUnitUtils;
import test.utils.ToStringUtils;

/**
 * Tests the {@link MirrorPersistenceFailureAlert}
 * 1. load GSM, GSC, load hsql, and deploy mirror, cluster(1,1), loader
 * 2. kill hsql
 * 3. wait for raised alert
 * 4. load hsql
 * 5. wait for resolved alert
 * 
 * @author Moran Avigdor
 * @since 8.0
 */
public class MirrorPersistFailureAlertTest extends AbstractTest {
	
	private static final int HSQL_DB_PORT = 9876;
	private Machine machine;
	private int hsqlId;
	private GridServiceAgent gsa;
	private ProcessingUnit mirror;
	private ProcessingUnit loader;
    private ProcessingUnit runtime;

	@BeforeMethod
	public void startSetUp() {
		
		log("waiting for 1 GSA");
		gsa = admin.getGridServiceAgents().waitForAtLeastOne();
		machine = gsa.getMachine();
		
		log("loading GSM");
		GridServiceManager gsm = loadGSM(machine);
		
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
        
        log("deploy loader via GSM");
        loader = gsm.deploy(new ProcessingUnitDeployment(DeploymentUtils.getProcessingUnit("MHEDS", "loader"))
        .setContextProperty("accounts", String.valueOf(10000)).setContextProperty("delay", "100"));
        loader.waitFor(loader.getTotalNumberOfInstances());
	}
	
    @Override
    @AfterMethod
    public void afterTest() {
        loader.undeploy();
        runtime.undeploy();
        mirror.undeploy();
    	gsa.killByAgentId(hsqlId);
        super.afterTest(); 
    }
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT , groups = "1")
	public void mirrorPersistFailure() throws InterruptedException {
		LogUtils.log("Waiting for mirror to process at least 1000 accounts");
		mirror.getSpace().startStatisticsMonitor();
		final SpaceInstance mirrorInstance = mirror.getSpace().getInstances()[0];

		long mirrorStatisticsSuccessfulOperationCount = 0;
		while (mirrorStatisticsSuccessfulOperationCount < 500) {
			mirrorStatisticsSuccessfulOperationCount = mirrorInstance.getStatistics().getMirrorStatistics().getSuccessfulOperationCount();
			Thread.sleep(100);
		}
		
		LogUtils.log("Mirror processed " + mirrorStatisticsSuccessfulOperationCount + " accounts");
		
		LogUtils.log("Register for Mirror Persist Failure alerts");
		final AlertManager alertManager = admin.getAlertManager();
		alertManager.setConfig(new MirrorPersistenceFailureAlertConfigurer().create());       
		alertManager.enableAlert(MirrorPersistenceFailureAlertConfiguration.class);
		
		final CountDownLatch alertRaised = new CountDownLatch(1);
		final CountDownLatch alertResolved = new CountDownLatch(1);
		alertManager.getAlertTriggered().add(new AlertTriggeredEventListener() {
			public void alertTriggered(Alert alert) {
				LogUtils.log("--> alert: " + alert.getDescription());
				if (alert instanceof MirrorPersistenceFailureAlert) {
					if (AlertStatus.RAISED.equals(alert.getStatus())) {
						assertEquals(mirrorInstance.getMachine().getHostName(), ((MirrorPersistenceFailureAlert)alert).getHostName());
						assertEquals(mirrorInstance.getMachine().getHostAddress(), ((MirrorPersistenceFailureAlert)alert).getHostAddress());
						assertEquals(mirrorInstance.getVirtualMachine().getUid(), ((MirrorPersistenceFailureAlert)alert).getVirtualMachineUid());
						alertRaised.countDown();
					} else if (AlertStatus.RESOLVED.equals(alert.getStatus())) {
						assertEquals(mirrorInstance.getMachine().getHostName(), ((MirrorPersistenceFailureAlert)alert).getHostName());
						assertEquals(mirrorInstance.getMachine().getHostAddress(), ((MirrorPersistenceFailureAlert)alert).getHostAddress());
						assertEquals(mirrorInstance.getVirtualMachine().getUid(), ((MirrorPersistenceFailureAlert)alert).getVirtualMachineUid());
						alertResolved.countDown();
					}
				}
			}
		});
		
		
		LogUtils.log("Killing HSQL process");
		gsa.killByAgentId(hsqlId);
		
		LogUtils.log("Waiting for alert raised event");
		alertRaised.await();
		
		
		long lastSuccessfulOperationCount = mirrorInstance.getStatistics().getMirrorStatistics().getSuccessfulOperationCount();
		
		log("load HSQL DB on machine - "+ToStringUtils.machineToString(machine));
        hsqlId = DBUtils.loadHSQLDB(machine, "MirrorPersistFailureAlertTest", HSQL_DB_PORT);
        LogUtils.log("Loaded HSQL successfully, id ["+hsqlId+"]");
        
        LogUtils.log("Waiting for alert resolved event");
		alertResolved.await();
		
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
