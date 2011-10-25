package test.servicegrid.mirror;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static test.utils.AdminUtils.loadGSC;
import static test.utils.AdminUtils.loadGSCs;
import static test.utils.AdminUtils.loadGSM;
import static test.utils.LogUtils.log;
import static test.utils.ProcessingUnitUtils.getProcessingUnitInstanceName;
import static test.utils.ToStringUtils.machineToString;

import java.sql.ResultSet;
import java.util.concurrent.CountDownLatch;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitDeployment;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.openspaces.admin.pu.events.ProcessingUnitInstanceAddedEventListener;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.AbstractTest;
import test.utils.DBUtils;
import test.utils.DeploymentUtils;
import test.utils.ProcessingUnitUtils;
import test.utils.ToStringUtils;

import com.gigaspaces.cluster.activeelection.SpaceMode;
import com.gigaspaces.ps.hibernateimpl.common.Account;
import com.gigaspaces.ps.hibernateimpl.common.ProcessedAccount;

public class MirrorRelocationTest extends AbstractTest {

    private Machine machineA;
	private Machine machineB;
	private GridServiceManager gsmA, gsmB;
	private GridServiceContainer[] gscsOnA, gscsOnB;
    private int hsqlId;
    private static final int ACCOUNTS = 10000;

    @BeforeMethod
	public void setup() {
		assertTrue(admin.getMachines().waitFor(2));
		assertTrue(admin.getGridServiceAgents().waitFor(2));

		GridServiceAgent[] agents = admin.getGridServiceAgents().getAgents();
        GridServiceAgent gsaA = agents[0];
        GridServiceAgent gsaB = agents[1];
		
		machineA = gsaA.getMachine();
		machineB = gsaB.getMachine();

		log("load 1 GSM and 2 GSCs on " + machineToString(machineA));
		gsmA = loadGSM(machineA);
		gscsOnA = loadGSCs(machineA, 2);

		log("load 1 GSM and 2 GSCs on " + machineToString(machineB));
		gsmB = loadGSM(machineB);
		gscsOnB = loadGSCs(machineB, 2);
	}

    @Test(timeOut=DEFAULT_TEST_TIMEOUT, groups = "2")
	public void test() throws Exception{
        int dbPort = 9911;//DBUtils.getAvailablePort(machineA);
        log("load HSQL DB on machine - "+ToStringUtils.machineToString(machineA));
        hsqlId = DBUtils.loadHSQLDB(machineA, "MirrorRelocationTest", dbPort);

        log("deploy mirror via GSM A");
        DeploymentUtils.prepareApp("MHEDS");
		ProcessingUnit mirror = gsmA.deploy(new ProcessingUnitDeployment(DeploymentUtils.getProcessingUnit("MHEDS", "mirror")).
                setContextProperty("port", String.valueOf(dbPort)).setContextProperty("host", machineA.getHostAddress()));
		mirror.waitFor(mirror.getTotalNumberOfInstances());

        log("deploy runtime(2,1) via GSM A");
        ProcessingUnit runtime = gsmA.deploy(new ProcessingUnitDeployment(DeploymentUtils.getProcessingUnit("MHEDS", "runtime")).
                numberOfInstances(2).numberOfBackups(1).maxInstancesPerVM(1).setContextProperty("port", String.valueOf(dbPort)).
                setContextProperty("host", machineA.getHostAddress()));
        runtime.waitFor(runtime.getTotalNumberOfInstances());
        
        ProcessingUnitUtils.waitForActiveElection(runtime);
        for(ProcessingUnitInstance puInstance : gscsOnA[0].getProcessingUnitInstances("runtime")){
            if(puInstance.getSpaceInstance().getMode().equals(SpaceMode.PRIMARY)){
                puInstance.restartAndWait();
                break;
            }
        }
        ProcessingUnitUtils.waitForActiveElection(runtime);

        log("deploy loader via GSM A - write "+ACCOUNTS +" accounts");
        ProcessingUnit loader = gsmA.deploy(new ProcessingUnitDeployment(DeploymentUtils.getProcessingUnit("MHEDS", "loader"))
                .setContextProperty("accounts", String.valueOf(ACCOUNTS)).setContextProperty("delay", "10"));
		loader.waitFor(loader.getTotalNumberOfInstances());

        log("load new GSC at machine - "+ ToStringUtils.machineToString(machineA));
        GridServiceContainer newGsc = loadGSC(machineA);

        log("relocate mirror to new GSC - "+ ToStringUtils.gscToString(newGsc));
        mirror.getInstances()[0].relocateAndWait(newGsc);
        Thread.sleep(10000);

        log("relocate mirror to new GSC - "+ ToStringUtils.gscToString(gscsOnA[1]));
        mirror.getInstances()[0].relocateAndWait(gscsOnB[1]);
        Thread.sleep(10000);

        log("relocate mirror to new GSC - "+ ToStringUtils.gscToString(newGsc));
        mirror.getInstances()[0].relocateAndWait(newGsc);
        Thread.sleep(10000);

        final CountDownLatch puInstanceAddedLatch = new CountDownLatch(1);
        mirror.getProcessingUnitInstanceAdded().add(new ProcessingUnitInstanceAddedEventListener() {
            public void processingUnitInstanceAdded(ProcessingUnitInstance processingUnitInstance) {
                log("event: " + getProcessingUnitInstanceName(processingUnitInstance) + " instantiated");
                puInstanceAddedLatch.countDown();
            }
        });
        log("kill new GSC - "+ ToStringUtils.gscToString(newGsc));
        newGsc.kill();
        puInstanceAddedLatch.await();



        long timeToWait = 5 * 60 * 1000;  //5 min
        long current = System.currentTimeMillis();
        while(System.currentTimeMillis() < current + timeToWait){
            if(runtime.getSpace().getGigaSpace().count(new Account()) == ACCOUNTS){
                break;
            }else{
                Thread.sleep(1000);
            }
        }

        assertThat(runtime.getSpace().getGigaSpace().count(new Account()), equalTo(ACCOUNTS));
        assertThat(runtime.getSpace().getGigaSpace().count(new ProcessedAccount()), equalTo(ACCOUNTS));
        Thread.sleep(30000);
        ResultSet rs = DBUtils.runSQLQuery("select count(*) from Account", machineA.getHostAddress(), dbPort);
        rs.next();
        Assert.assertEquals(ACCOUNTS, rs.getLong(1));
    }

    @Override
    @AfterMethod
    public void afterTest() {
        machineA.getGridServiceAgent().killByAgentId(hsqlId);
        super.afterTest(); 
    }
}
