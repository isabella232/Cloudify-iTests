package test.gsm.datagrid.manual.advanced;

import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.elastic.config.ManualCapacityScaleConfigurer;
import org.openspaces.admin.space.ElasticSpaceDeployment;
import org.openspaces.core.util.MemoryUnit;
import org.testng.annotations.Test;

import test.gsm.AbstractGsmTest;
import test.gsm.GsmTestUtils;


/**
 * @author giladh
 */
public class NumberOfPartitionsTest extends AbstractGsmTest {
 
    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1")
    public void doTest_2() {
    	inner_doTest(2);
    }

    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1")
    public void doTest_4() {
    	inner_doTest(4);
    }
    
    
    private void inner_doTest(final int partitions) {
    	admin.getGridServiceAgents().waitFor(1);
   	    
    	assertEquals(0, getNumberOfGSCsAdded());
    	
    	final int NUM_CONTAINERS = 4;
    	final int MEM_PER_CONTAINER = 256;
    	       
        final ProcessingUnit pu = gsm.deploy(
                new ElasticSpaceDeployment("mygrid")
                .memoryCapacityPerContainer(MEM_PER_CONTAINER,MemoryUnit.MEGABYTES)
                .numberOfPartitions(partitions)
                .singleMachineDeployment());
        
        assertEquals("Number of pu partitions", pu.getNumberOfInstances(), partitions);
        
        pu.scale(new ManualCapacityScaleConfigurer()
                 .memoryCapacity(NUM_CONTAINERS*MEM_PER_CONTAINER,MemoryUnit.MEGABYTES)
                 .create());

		GsmTestUtils.waitForScaleToCompleteIgnoreCpuSla(pu, NUM_CONTAINERS, OPERATION_TIMEOUT);
		assertEquals("Number of GSCs added", NUM_CONTAINERS, getNumberOfGSCsAdded());
        assertEquals("Number of GSCs removed", 0, getNumberOfGSCsRemoved());        
    }

}
