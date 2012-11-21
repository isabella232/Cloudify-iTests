package test.usm.xen;

import org.apache.cassandra.thrift.Column;
import org.apache.cassandra.thrift.ConsistencyLevel;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.openspaces.admin.pu.elastic.ElasticStatelessProcessingUnitDeployment;
import org.openspaces.admin.pu.elastic.config.ManualCapacityScaleConfigurer;
import org.openspaces.core.util.MemoryUnit;
import org.scale7.cassandra.pelops.*;
import org.testng.annotations.Test;

import framework.tools.SGTestHelper;
import framework.utils.LogUtils;
import framework.utils.SSHUtils;
import framework.utils.xen.AbstractXenGSMTest;
import framework.utils.xen.GsmTestUtils;
import test.usm.USMTestUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * This test deploys a ring of two cassandra instances using an
 * elastic processing unit running the USM with a cassandra service DSL.
 * 
 * 1) Deploy cassandra service on two machines. (ring)
 * 2) Write entry with ConsistencyLevel = 2
 * 3) Test a) Restart one of the GSCs
 *    Test b) Kill the cassandra process with 'kill -9'
 * 4) Read previously written entry with ConsistencyLevel = 2
 * 
 * @author itaif - ESM integration
 * @author dank - Cassandra client integration
 *
 */
public class CassandraXenContainerFailoverTest extends AbstractXenGSMTest {

	static final String CASSANDRA = "/apps/USM/usm/cassandra";
	
	// total number of CPU cores allocated to the cassandra cluster
	// by default each VM has 2 cores, so 4 cores means 2 machines 
	static final int numberOfCpuCores =4;
	
	// credentials for SSHing into the machine. This is required 
	// 1. To kill -9 the cassandra process
	// 2. To create the data schema (only once, not per USM deployment) 
	private static final String SSH_USERNAME = "root";
    private static final String SSH_PASSWORD = "123456";
    
    // The port that the client connects to cassandra with
    private static final int CASSANDRA_PORT = 9160;
    
    // The read and write consistency level (how many replicas)
    // Writing requires two copies and reading requires two copies
    private static final ConsistencyLevel READ_WRITE_CL = ConsistencyLevel.TWO;
    
    // Cassandra data related properties
    private static final String POOL = "pool";
    private static final String KEYSPACE = "mykeyspace";
    private static final String COLFAMILY = "users";
    private static final String ROWKEY = "abc123";
    private static final String COLNAME1 = "name";
    private static final String COLVALUE1 = "Dan";
    private static final String COLNAME2 = "age";
    private static final int COLVALUE2 = 25;

    @Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = false)
    public void testCassandraFailover() throws Exception {
        doTest(true);
    }
    
    @Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = false)
    public void testGSCFailover() throws Exception {
    	doTest(false);
    }
    
    public void doTest(boolean killCassandra) throws InterruptedException, IOException {
	  
		 File cassandraJar = USMTestUtils.usmCreateJar(CASSANDRA);

	    // make sure no gscs yet created
	    repetitiveAssertNumberOfGSCsAdded(0, OPERATION_TIMEOUT);
	    repetitiveAssertNumberOfGSAsAdded(1, OPERATION_TIMEOUT);	    
	    
	    final int expectedNumberOfMachines = (int)  
    		Math.ceil(numberOfCpuCores/super.getMachineProvisioningConfig().getNumberOfCpuCoresPerMachine());

	    int expectedNumberOfContainers = (int) Math.ceil(numberOfCpuCores/super.getMachineProvisioningConfig().getNumberOfCpuCoresPerMachine());
	    
		final ProcessingUnit pu = gsm.deploy(
				new ElasticStatelessProcessingUnitDeployment(cassandraJar)
	            .memoryCapacityPerContainer(1, MemoryUnit.GIGABYTES)
	            .dedicatedMachineProvisioning(getMachineProvisioningConfig()).

	            scale(new ManualCapacityScaleConfigurer()
                      .numberOfCpuCores(numberOfCpuCores)
                      .create())
	    );
	    
	    pu.waitFor(expectedNumberOfContainers);
	    	    
	    repetitiveAssertNumberOfGSCsRemoved(0, OPERATION_TIMEOUT);	    	
	    repetitiveAssertNumberOfGSCsAdded(expectedNumberOfContainers, OPERATION_TIMEOUT);
	    repetitiveAssertNumberOfGSAsAdded(expectedNumberOfMachines, OPERATION_TIMEOUT);
	    repetitiveAssertNumberOfGSAsRemoved(0, OPERATION_TIMEOUT);
	    
	    createSchema();
        
	    Machine[] machines = admin.getMachines().getMachines();
        Cluster cluster = new Cluster(machines[0].getHostAddress(), CASSANDRA_PORT);
        try {
            Pelops.addPool(POOL, cluster, KEYSPACE);
    
            LogUtils.log("Writing entry to cassandra at ConsistencyLevel = 2");
            Mutator mutator = Pelops.createMutator(POOL);
            mutator.writeColumns(COLFAMILY, ROWKEY, 
                mutator.newColumnList(
                    mutator.newColumn(COLNAME1, COLVALUE1), 
                    mutator.newColumn(COLNAME2, Bytes.fromInt(COLVALUE2))
                )
            );
            // This is needed because the cluster may not be ready yet
            Thread.sleep(1000);
            mutator.execute(READ_WRITE_CL);
            
            ProcessingUnitInstance instanceOnMachineB = machines[1].getProcessingUnitInstances()[0];
            long pid = USMTestUtils.getActualPID(instanceOnMachineB);
            if (killCassandra) {
                LogUtils.log("Killing the cassandra process at " + machines[1].getHostAddress() + " : " + pid);
                String command = "kill -9 " + pid;
                SSHUtils.runCommand(machines[1].getHostAddress(), OPERATION_TIMEOUT, command, SSH_USERNAME, SSH_PASSWORD);
            } else {
                LogUtils.log("Restarting GSC at " + machines[1].getHostAddress());
                GridServiceContainer container = admin.getGridServiceContainers().getContainers()[0];
                GsmTestUtils.killContainer(container);
                pu.waitFor(expectedNumberOfContainers);
                
                repetitiveAssertNumberOfGSCsRemoved(1, OPERATION_TIMEOUT);	    	
        	    repetitiveAssertNumberOfGSCsAdded(expectedNumberOfContainers+1,  OPERATION_TIMEOUT);
        	    repetitiveAssertNumberOfGSAsAdded(expectedNumberOfMachines, OPERATION_TIMEOUT);
        	    repetitiveAssertNumberOfGSAsRemoved(0, OPERATION_TIMEOUT);
        	    
            }
            
            LogUtils.log("Wait for cassandra to finish failover");
            Thread.sleep(10000); 
            Selector selector = Pelops.createSelector(POOL);
            
            LogUtils.log("Reading previously written entry from cassandra at ConsistencyLevel = 2");
            List<Column> columns = selector.getColumnsFromRow(COLFAMILY, ROWKEY, false, READ_WRITE_CL);
    
            assertEquals(COLVALUE1, Selector.getColumnStringValue(columns, COLNAME1));
            assertEquals(COLVALUE2, Selector.getColumnValue(columns, COLNAME2).toInt());
        } finally {
            Pelops.shutdown();
        }
	}
    

    // This method assumes NFS (i.e. XAP path on client == XAP path on server)
    private void createSchema() throws IOException, InterruptedException {
        String cassandraDir = "/opt/gigaspaces/" + SGTestHelper.getWorkDirName() + "/processing-units/cassandra-service_1/ext/";
        String pathToCassandraCli = cassandraDir + "apache-cassandra-0.7.5/bin/cassandra-cli";
        String pathToCreateSchema = cassandraDir + "createSchema.cassandra";
        String command = pathToCassandraCli + " -f " + pathToCreateSchema;
        SSHUtils.runCommand(admin.getMachines().getMachines()[0].getHostAddress(), OPERATION_TIMEOUT, command, SSH_USERNAME, SSH_PASSWORD);
    }
}
