package test.servicegrid.mirror;

import static test.utils.AdminUtils.loadGSCs;
import static test.utils.AdminUtils.loadGSM;
import static test.utils.LogUtils.log;
import static test.utils.ToStringUtils.machineToString;

import java.sql.ResultSet;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitDeployment;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.openspaces.core.GigaSpace;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.AbstractTest;
import test.utils.DBUtils;
import test.utils.DeploymentUtils;
import test.utils.ProcessingUnitUtils;

import com.gigaspaces.cluster.activeelection.SpaceMode;
import com.gigaspaces.ps.hibernateimpl.common.ProcessedAccount;

public class DBShutdownTest extends AbstractTest {

	private static final long ACCOUNTS = 10000;
	private Machine machine;
	private GridServiceManager gsm;
	private GridServiceContainer[] gscs;
	private int hsqlId;
	private GridServiceAgent gsa;
	
	@BeforeMethod
	public void setup() {
		assertTrue(admin.getMachines().waitFor(1));
		assertTrue(admin.getGridServiceAgents().waitFor(1));
		gsa = admin.getGridServiceAgents().waitForAtLeastOne();
		
		machine = gsa.getMachine();
		
		log("load 1 GSM and 2 GCSs on  "+machineToString(machine));
		gsm=loadGSM(machine);
		gscs=loadGSCs(machine, 2);
	}
	 
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups="1")
	public void test() throws Exception{		
		int dbPort = 9922; //DBUtils.getAvailablePort(machine);
        log("load HSQL DB on machine - "+machineToString(machine));
        hsqlId = DBUtils.loadHSQLDB(machine, "DBShutdownTest", dbPort);
        
        Thread.sleep(10000);
        //empty database table
        log("emptying Account, ProcessedAccount table at database");
        DBUtils.runSQLQuery("delete from Account", machine.getHostAddress(), dbPort);
        DBUtils.runSQLQuery("delete from ProcessedAccount", machine.getHostAddress(), dbPort);
        
        log("deploy mirror via GSM");
        DeploymentUtils.prepareApp("MHEDS");
		ProcessingUnit mirror = gsm.deploy(new ProcessingUnitDeployment(DeploymentUtils.getProcessingUnit("MHEDS", "mirror")).
                setContextProperty("port", String.valueOf(dbPort)).setContextProperty("host", machine.getHostAddress()));
		mirror.waitFor(mirror.getTotalNumberOfInstances());
		
		log("deploy runtime(2,1) via GSM");
        ProcessingUnit runtime = gsm.deploy(new ProcessingUnitDeployment(DeploymentUtils.getProcessingUnit("MHEDS", "runtime")).
                numberOfInstances(2).numberOfBackups(1).maxInstancesPerVM(1).setContextProperty("port", String.valueOf(dbPort)).
                setContextProperty("host", machine.getHostAddress()));
        runtime.waitFor(runtime.getTotalNumberOfInstances());
        ProcessingUnitUtils.waitForActiveElection(runtime);
        for(ProcessingUnitInstance puInstance : gscs[0].getProcessingUnitInstances("runtime")){
            if(puInstance.getSpaceInstance().getMode().equals(SpaceMode.PRIMARY)){
                puInstance.restartAndWait();
                break;
            }
        }
        ProcessingUnitUtils.waitForActiveElection(runtime);
        
        GigaSpace gigaSpace = runtime.getSpace().getGigaSpace();
       
        
        
        
        log("deploy loader via GSM - write "+ACCOUNTS +" accounts");
        ProcessingUnit loader = gsm.deploy(new ProcessingUnitDeployment(DeploymentUtils.getProcessingUnit("MHEDS", "loader"))
                .setContextProperty("accounts", String.valueOf(ACCOUNTS)).setContextProperty("delay", "10"));
		loader.waitFor(loader.getTotalNumberOfInstances());
        
		//waits for loader to start writing
		
		log("count check : while count < 50");
		ProcessedAccount template = new ProcessedAccount();
		while (gigaSpace.count(template) < 50){ //runtime.getInstances()[0].getProcessingUnit().getSpace().getGigaSpace().count(new ProcessedAccount())
			Thread.sleep(100);
		}
		
		log("killing db");
		machine.getGridServiceAgent().killByAgentId(hsqlId);
		
		log("count check : while count < "+ACCOUNTS);
		while (gigaSpace.count(template) < ACCOUNTS){
			Thread.sleep(2000);
		}
		
		log("loading new db");
		hsqlId = DBUtils.loadHSQLDB(machine, "DBShutdownTest", dbPort);
		Thread.sleep(30000);
		
		int numOfLoopsToTry = 20;
		for (int i = 0; i < numOfLoopsToTry+1; i++) {
			ResultSet rs = DBUtils.runSQLQuery("select count(*) from Account", machine.getHostAddress(), dbPort);
	        rs.next();
	        long objectsInDatabase = rs.getLong(1);
	        log("object count in database: "+objectsInDatabase+", loop: "+i);
	        if (ACCOUNTS == objectsInDatabase){
	        	gsa.killByAgentId(hsqlId);
	        	break;
	        }else if(i == numOfLoopsToTry){
	        	gsa.killByAgentId(hsqlId);
	        	Assert.fail("number of objects written is: "+ACCOUNTS+" while object in database is: "+objectsInDatabase);	
	        }
	        Thread.sleep(30000);
		}	        
	        
	        
	}
	
}
