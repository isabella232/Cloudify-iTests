package test.servicegrid.mirror;

import static test.utils.AdminUtils.loadGSM;
import static test.utils.LogUtils.log;
import static test.utils.ToStringUtils.machineToString;

import java.sql.ResultSet;
import java.util.concurrent.TimeUnit;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitDeployment;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.openspaces.core.GigaSpace;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.AbstractTest;
import test.utils.AdminUtils;
import test.utils.DBUtils;
import test.utils.DeploymentUtils;

import com.gigaspaces.cluster.activeelection.SpaceMode;
import com.gigaspaces.ps.hibernateimpl.common.ProcessedAccount;

public class ClusterAndMirrorDeploymentWhileDBisDownTest extends AbstractTest {

	private static final int ACCOUNTS = 100;
	private Machine machine;
	private GridServiceManager gsm;
	private GridServiceContainer[] gscs = new GridServiceContainer[2];
	private int hsqlId;

    @BeforeMethod
	public void setup() {
		assertTrue(admin.getMachines().waitFor(1));
		assertTrue(admin.getGridServiceAgents().waitFor(1));

		GridServiceAgent gsa = admin.getGridServiceAgents().waitForAtLeastOne();
		
		machine = gsa.getMachine();

		log("load 1 GSM and 2 GCSs on  "+machineToString(machine));
		gsm=loadGSM(machine);
        gscs[0] = AdminUtils.loadGSCWithSystemProperty(machine, "-Dorg.jini.rio.monitor.pendingRequestDelay=120000");
        gscs[1] = AdminUtils.loadGSCWithSystemProperty(machine, "-Dorg.jini.rio.monitor.pendingRequestDelay=120000");
	}

    @Test(timeOut=DEFAULT_TEST_TIMEOUT, groups = "1")
	public void test() throws Exception{
		int dbPort = 11112;

        log("deploy mirror via GSM");
        DeploymentUtils.prepareApp("MHEDS");
		ProcessingUnit mirror = gsm.deploy(new ProcessingUnitDeployment(DeploymentUtils.getProcessingUnit("MHEDS", "mirror")).
                setContextProperty("port", String.valueOf(dbPort)).setContextProperty("host", machine.getHostAddress()));

		log("deploy runtime(2,1) via GSM");
        ProcessingUnit runtime = gsm.deploy(new ProcessingUnitDeployment(DeploymentUtils.getProcessingUnit("MHEDS", "runtime")).
                numberOfInstances(2).numberOfBackups(1).maxInstancesPerVM(1).setContextProperty("port", String.valueOf(dbPort)).
                setContextProperty("host", machine.getHostAddress()));

        Thread.sleep(70000);

        log("load HSQL DB on machine - "+machineToString(machine));
        hsqlId = DBUtils.loadHSQLDB(machine, "ClusterAndMirrorDeploymentWhileDBisDownTest", dbPort);

        mirror.waitFor(mirror.getTotalNumberOfInstances());
        runtime.waitFor(runtime.getTotalNumberOfInstances());

        log("deploy loader via GSM - write "+ACCOUNTS +" accounts");
        ProcessingUnit loader = gsm.deploy(new ProcessingUnitDeployment(DeploymentUtils.getProcessingUnit("MHEDS", "loader"))
                .setContextProperty("accounts", String.valueOf(ACCOUNTS)).setContextProperty("delay", "1000"));
		loader.waitFor(loader.getTotalNumberOfInstances());

        log("get primary space");
        GigaSpace primary=waitForMode(runtime, SpaceMode.PRIMARY);

		//waits for loader to start writing
		log("count check : while count < 1");
		while (primary.count(new ProcessedAccount()) < 1){
			Thread.sleep(100);
		}

		log("count check : while count < "+ACCOUNTS);
		while (runtime.getInstances()[0].getProcessingUnit().getSpace().getGigaSpace().count(new ProcessedAccount()) < ACCOUNTS){
			Thread.sleep(2000);
		}

        loader.undeploy();
        
		ResultSet rs = DBUtils.runSQLQuery("select count(*) from Account", machine.getHostAddress(), dbPort);
        rs.next();
        assertEquals(ACCOUNTS, rs.getLong(1));

        machine.getGridServiceAgent().killByAgentId(hsqlId);
	}

	private GigaSpace waitForMode(ProcessingUnit pu, SpaceMode mode) {
		for (ProcessingUnitInstance puInstance : pu) {
			if(puInstance.getSpaceInstance().waitForMode(mode, 60, TimeUnit.SECONDS))
				return puInstance.getSpaceInstance().getGigaSpace();
		}
		return null;
	}
}

