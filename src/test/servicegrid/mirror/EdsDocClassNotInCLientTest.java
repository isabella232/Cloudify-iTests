package test.servicegrid.mirror;

import static test.utils.AdminUtils.loadGSM;
import static test.utils.DeploymentUtils.getArchive;
import static test.utils.LogUtils.log;
import static test.utils.ToStringUtils.machineToString;

import java.sql.ResultSet;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import net.jini.core.lease.Lease;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitDeployment;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.AbstractTest;
import test.utils.AdminUtils;
import test.utils.DBUtils;

import com.gigaspaces.document.SpaceDocument;
import java.util.Arrays;

/**
 * Tests document API client with a POJO based space with initial load.
 * The problem occurs when the database is empty, no initial load - thus space does not load the POJO from classpath
 * and when document client writes the document, the space cannot find its type.
 * 
 * According to Niv 02-jun-2011 this feature is not implemented. Ester - Requested by various customers. For example see GS-7547.
 * Same bug appears also for .NET and C++
 * @author ester
 */
public class EdsDocClassNotInCLientTest extends AbstractTest{
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
        gscs[0] = AdminUtils.loadGSC(machine);
        gscs[1] = AdminUtils.loadGSC(machine);
	}
    
    
    private static SpaceDocument getExternalEntry(String idValue, String routingValue) {		
		SpaceDocument doc = new  SpaceDocument("tst.PojoData");
		Map<String, Object> properties = new HashMap<String,Object>();
		properties.put("id", idValue);
		properties.put("routing", routingValue);
		doc.addProperties(properties);
		return doc;
	}

    @Test(timeOut=DEFAULT_TEST_TIMEOUT, groups = "1")
	public void test() throws Exception{
		//int dbPort = 11112;
    	int dbPort=9008;
    	log("load HSQL DB on machine - "+machineToString(machine));
        hsqlId = DBUtils.loadHSQLDB(machine, "EdsDocClassNotInCLientTest", dbPort);
    	
    	 ProcessingUnit pu = gsm.deploy(
	                new ProcessingUnitDeployment(getArchive("case5978.zip")).
	                numberOfInstances(1).
	                numberOfBackups(1).
	                clusterSchema("partitioned-sync2backup").
	                name("case5978"));
	     pu.waitFor(pu.getTotalNumberOfInstances());
		
		
		
	     ProcessingUnit mirror = gsm.deploy(
	                new ProcessingUnitDeployment(getArchive("case5978-mirror.zip")).
	                numberOfInstances(1).
	                numberOfBackups(0).
	                clusterSchema("partitioned-sync2backup").
	                name("case5978-mirror"));	     
	     mirror.waitFor(mirror.getTotalNumberOfInstances());
	     
	     
	     
	     Random rand = new Random((new Date()).getTime());
		 String id = "id-" + String.valueOf(rand.nextInt());
		 String routing = "routing-" + String.valueOf(rand.nextInt());
		 
		
			
			
			// --------- External Entry ---------
		SpaceDocument dataExternalEntry = getExternalEntry(id, routing);
		SpaceDocument[] externalEntries = new SpaceDocument[1];
		externalEntries[0] = dataExternalEntry;
		long[] leases = new long[externalEntries.length];
		Arrays.fill(leases, Lease.FOREVER);
		pu.getSpace().getGigaSpace().writeMultiple(externalEntries, Lease.FOREVER);
        
             
		ResultSet rs = DBUtils.runSQLQuery("select count(*) from Object", machine.getHostAddress(), dbPort);
        rs.next();
        Assert.assertEquals(1, rs.getLong(1));

        
	}
    
    @AfterMethod
    public void afterTest() {
    	machine.getGridServiceAgent().killByAgentId(hsqlId);
    	super.afterTest();
    }

}
