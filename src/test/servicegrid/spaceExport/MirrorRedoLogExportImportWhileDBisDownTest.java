package test.servicegrid.spaceExport;

import static test.utils.LogUtils.log;
import static test.utils.ToStringUtils.machineToString;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitDeployment;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.openspaces.admin.pu.ProcessingUnitPartition;
import org.openspaces.core.GigaSpace;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.DataProvider;

import test.AbstractTest;
import test.utils.AdminUtils;
import test.utils.DBUtils;
import test.utils.DeploymentUtils;
import test.utils.DumpUtils;

import com.gigaspaces.ps.hibernateimpl.common.Account;

/**
 * Tests the mirror redo log export/import functionality.
 * 	 
 * Before running this test locally open a command prompt and run:
 * set LOOKUPGROUPS=mylookupgroupname
 * set JSHOMEDIR=latestunzippedxapfolder
 * start cmd /c "%JSHOMEDIR%\bin\gs-agent.bat gsa.global.esm 0 gsa.gsc 0 gsa.global.gsm 0 gsa.global.lus 1"
 * copy eclipse_workspace\SGTest\config\hsqldb.xml %JSHOMEDIR%\config\gsa\
 * 
 * Also make sure that the Eclipse "Run Configuration" includes the following Program argument "-listener framework.testng.SGTestNGListener"
 * 
 * @author itaif
 */
public class MirrorRedoLogExportImportWhileDBisDownTest extends AbstractTest {
	
	private Machine machine;
	private GridServiceManager gsm;
	private int hsqlId;
	private int dbPort;
	private ProcessingUnit pu;
	private ProcessingUnit mirror;
	private File exportDirectory;
	private File workDirectory;
	
	static class TestConfiguration {
		
		int numberOfBackups;
		int numberOfGscs;
		int numberOfInstances;
		int numberOfAccounts;
		final long randomNumber;
		
		public TestConfiguration() {
			numberOfBackups=1;
			numberOfGscs=2;
			numberOfInstances=1;
			numberOfAccounts = 100;
			randomNumber = UUID.randomUUID().getLeastSignificantBits();
		}

		public int getNumberOfBackups() {
			return numberOfBackups;
		}

		public int getNumberOfGscs() {
			return numberOfGscs;
		}

		public int getNumberOfInstances() {
			return numberOfInstances;
		}

		public int getNumberOfAccounts() {
			return numberOfAccounts;
		}
		
		public TestConfiguration setNumberOfBackups(int n) {
			this.numberOfBackups = n;
			return this;
		}
		
		public String getTestUniqueId() {
			return String.valueOf(Math.abs(this.randomNumber));
		}
		
		public TestConfiguration setNumberOfGscs(int n) {
			this.numberOfGscs = n;
			return this;
		}
		
		public TestConfiguration setNumberOfInstances(int n) {
			this.numberOfInstances = n;
			return this;
		}
		
		public TestConfiguration setNumberOfAccounts(int n) {
			this.numberOfAccounts = n;
			return this;
		}

	}

	@DataProvider(name="Data-Provider-Function")
	public Object[][] testConfigurationProvider() {
		return new Object[][]{
				{new TestConfiguration()},
		/*
				{new TestConfiguration()
					.setNumberOfInstances(2)}*/
		};
	}

	

	/**
	 * Closes the database when test ends
	 */
    @AfterMethod
	public void afterTest() {   	
    	stopDatabase();
    	super.afterTest();
    }

   
    /*
	 * Closes databases and writes to space.
	 * Exports redo-log to disk and uses a dedicated mirror instance to import file back to database.
	 * TEST DISABLED SINCE FEATURE DID NOT MAKE IT TO VERSION 8.0

    @Test(timeOut=DEFAULT_TEST_TIMEOUT, 
    	  groups = "1",
    	  dataProvider = "Data-Provider-Function")
    	  	 */
    public void test(TestConfiguration config) throws InterruptedException, IOException, SQLException {
    	try {
	    	setup(config);
	    	
	    	stopDatabase();
	    	writeData(config);

	    	// since database is down, this would cause the primaries to export the mirror redo log to file.
	    	restartPrimaries();

	    	undeployMirror();

	    	startDatabase();
	    	assertEquals("Database is not empty.",0, countDatabaseData(config));
	    	mirror = deployMirrorWithDifferentNameAndImportFromDumpFile(config);
	    	mirror.waitFor(mirror.getTotalNumberOfInstances());
	    	assertEquals("Database does not contain the expexted number of accounts",config.numberOfAccounts, countDatabaseData(config));
    	} 
    	finally {
    		undeployMirror();
    		undeployProcessingUnit();
    	}

	}

    private void restartPrimaries() {
		// before restarting, find all primaries 
    	//(once we start restarting, then backup become primaries
    	List<ProcessingUnitInstance> primaries = new ArrayList<ProcessingUnitInstance>();
    	for (ProcessingUnitPartition partition : pu.getPartitions()) {
			ProcessingUnitInstance primary = partition.getPrimary(); 
    		Assert.assertNotNull(primary);
    		primaries.add(primary);			
    	}
    	
		for (ProcessingUnitInstance primary : primaries) {
			primary.restartAndWait();
		}
		
	}

	/**
     * Starts a runtime pu + mirror + database.
     * Call this method before the test starts.
	 * @throws SQLException 
	 * @throws IOException 
     */
	public void setup(TestConfiguration config) throws SQLException, IOException {
		
		workDirectory = new File(DumpUtils.getTestFolder(),config.getTestUniqueId());
		workDirectory.delete();
    	workDirectory.mkdirs();
	
    	exportDirectory = new File(workDirectory,"export");
    	exportDirectory.mkdir();
    	
		assertTrue(admin.getMachines().waitFor(1));
		assertTrue(admin.getGridServiceAgents().waitFor(1));
		
		GridServiceAgent gsa = admin.getGridServiceAgents().waitForAtLeastOne();
		machine = gsa.getMachine();

		log("load 1 GSM on  "+machineToString(machine));
		gsm = AdminUtils.loadGSM(machine);
		
		log("load " + config.numberOfGscs + " GSCs on  "+machineToString(machine));
		AdminUtils.loadGSCs(machine,config.numberOfGscs);
        
    	dbPort = DBUtils.getAvailablePort(machine);
    	startDatabase();

    	mirror = deployMirror(config);

		deployProcessingUnit(config);
        pu.waitFor(pu.getTotalNumberOfInstances());
        mirror.waitFor(mirror.getTotalNumberOfInstances());
	}

	private ProcessingUnit deployMirror(TestConfiguration config) {
		log("deploying mirror");
		Assert.assertNull(mirror);
        DeploymentUtils.prepareApp("MHEDS");
		ProcessingUnit mirror = gsm.deploy(createMirrorProcessingUnitDeployment(config));
		return mirror;
	}

	private ProcessingUnit deployMirrorWithDifferentNameAndImportFromDumpFile(TestConfiguration config) {
		log("deploying mirror and importing from file.");
		Assert.assertNull(mirror);
		ProcessingUnit mirror = gsm.deploy(
				createMirrorProcessingUnitDeployment(config)
				.name("mirror-import-from-file")
				.setContextProperty("spacename", "mirror-service-import-from-file")
				.setContextProperty("space-config.mirror-service.import-directory",  exportDirectory.getAbsolutePath()));
		return mirror;
	}
    
	private ProcessingUnitDeployment createMirrorProcessingUnitDeployment(TestConfiguration config) {
		ProcessingUnitDeployment deployment = new ProcessingUnitDeployment(
				DeploymentUtils.getProcessingUnit("MHEDS", "mirror"))
				.setContextProperty("port", String.valueOf(dbPort))
				.setContextProperty("host", machine.getHostAddress());
		return deployment;		
	}

	private void deployProcessingUnit(TestConfiguration config) {
		log("deploying runtime space ("+config.numberOfInstances+","+config.numberOfBackups+")");
        pu = gsm.deploy(new ProcessingUnitDeployment(
        		DeploymentUtils.getProcessingUnit("MHEDS", "runtime"))
        		.numberOfInstances(config.numberOfInstances)
                .numberOfBackups(config.numberOfBackups)
                .maxInstancesPerVM(1)
                // database port
                .setContextProperty("port", String.valueOf(dbPort))
                // database dns name
                .setContextProperty("host", machine.getHostAddress())                
                // When PU is undeployed, don't wait for the async mirror replication to complete
                // The database is down anyway so the replication will never end
                .setContextProperty("cluster-config.groups.group.repl-policy.async-replication.async-channel-shutdown-timeout","5000")
                //temp folder for exportig redo log
                .setContextProperty("space-config.export.export-directory", exportDirectory.getAbsolutePath())
        );
	}
	
	private long countDatabaseData(TestConfiguration config) throws SQLException {
		ResultSet rs = DBUtils.runSQLQuery("select count(*) from Account", machine.getHostAddress(), dbPort);
        rs.next();
        return rs.getLong(1);
	}
	
	/**
	 * Writes accounts to the space.
	 * @throws InterruptedException
	 */
	private void writeData(TestConfiguration config)
			throws InterruptedException {

		log("deploying loader space - write "+config.numberOfAccounts +" accounts");
        ProcessingUnit feeder = gsm.deploy(
        		new ProcessingUnitDeployment(
        				DeploymentUtils.getProcessingUnit("MHEDS", "loader"))
                		.setContextProperty("accounts", String.valueOf(config.numberOfAccounts))
                		.setContextProperty("delay", "0"));
		feeder.waitFor(feeder.getTotalNumberOfInstances());

		//waits for loader to start writing
		log("count check : while count < 1");
		GigaSpace gigaSpace = pu.waitForSpace().getGigaSpace();
		int count =0;
		log("count check : while count < "+ config.numberOfAccounts);
		
		do {
			count = gigaSpace.count(new Account());
			Thread.sleep(1000);
		} while (count < config.numberOfAccounts);
		
		log("undeploying space " + feeder.getName());
        feeder.undeploy();
	}

	private void startDatabase() {
		Assert.assertEquals(hsqlId, 0);
        hsqlId = DBUtils.loadHSQLDB(machine, "myDB", dbPort, workDirectory);
        log("started HSQL DB on machine - "+machineToString(machine) + 
        	" process id=" + hsqlId+
        	" dbPort=" + dbPort +
        	" workDir=" + workDirectory);
        
        waitForDatabaseToStart();

	}



	private void waitForDatabaseToStart() {
		long start = System.currentTimeMillis();
        while(System.currentTimeMillis() - start < 60 * 1000) {
        	Socket dbClient = null;
        	try {
	        	dbClient = new Socket(machine.getHostAddress(), dbPort);
	            break;
	        }
	        catch (IOException e) {
	        	log("Sleeping for 10 seconds, before attempting to connect with database.");
	        	try {
					Thread.sleep(10*1000);
				} catch (InterruptedException e1) {
					Thread.interrupted();
					log("Attempt to connect with database was interrupted");
					break;
				}
	        }
	        finally {
	        	if (dbClient != null) {
	        		try {
						dbClient.close();
					} catch (IOException e) {
						log("Failed to disconnect TCP socket from database ",e);
					}
	        		dbClient = null;
	        	}
	        }
        }
	}
	
	private void stopDatabase() {
		machine.getGridServiceAgent().killByAgentId(hsqlId);
		log("stopped HSQL DB on machine - "+machineToString(machine) +" process id="+hsqlId);
		hsqlId = 0;
	}
	
	private void undeployMirror() {
		if (mirror != null) {
			log("Undeploying space " + mirror.getName());
			mirror.undeploy();
			mirror = null;
		}
	}

	private void undeployProcessingUnit() {
		if (pu != null) {
			log("Undeploying space " + pu.getName());
			pu.undeploy();
		}
	}
}

