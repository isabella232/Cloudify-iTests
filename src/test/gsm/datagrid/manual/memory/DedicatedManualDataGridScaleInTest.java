package test.gsm.datagrid.manual.memory;

import java.util.concurrent.TimeUnit;

import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.elastic.config.ManualCapacityScaleConfigurer;
import org.openspaces.admin.space.ElasticSpaceDeployment;
import org.openspaces.core.util.MemoryUnit;
import org.testng.annotations.Test;

import test.gsm.AbstractGsmTest;
import test.gsm.GsmTestUtils;

public class DedicatedManualDataGridScaleInTest extends AbstractGsmTest {

    /*
     * GS-9284
     */
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1")
    public void testManualDataGridDeploymentScale() {
        admin.getGridServiceAgents().waitFor(1); 
        
        assertEquals(0, getNumberOfGSCsAdded());
        
        ElasticSpaceDeployment deployment = new ElasticSpaceDeployment("ESMTestingGrid")
              .addContextProperty("cluster-config.groups.group.fail-over-policy.active-election.yield-time","300")
              .addContextProperty("cluster-config.groups.group.fail-over-policy.active-election.fault-detector.invocation-delay","300")
              .addContextProperty("cluster-config.groups.group.fail-over-policy.active-election.fault-detector.retry-count","2")
              .addContextProperty("space-config.proxy-settings.connection-retries","5")
              .singleMachineDeployment()
              .maxMemoryCapacity(256, MemoryUnit.MEGABYTES)
              .memoryCapacityPerContainer(32,MemoryUnit.MEGABYTES)
              .scale(new ManualCapacityScaleConfigurer()
                  .memoryCapacity(256,MemoryUnit.MEGABYTES)
                  .create());
        
        final ProcessingUnit pu = gsm.deploy(deployment);

        GsmTestUtils.waitForScaleToComplete(pu, 8, OPERATION_TIMEOUT);
        assertEquals("Number of GSCs added", 8, getNumberOfGSCsAdded());
        assertEquals("Number of GSCs removed", 0, getNumberOfGSCsRemoved());
        
        pu.scaleAndWait(new ManualCapacityScaleConfigurer()
            .memoryCapacity(64,MemoryUnit.MEGABYTES)
            .create(), OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
        
        GsmTestUtils.assertScaleCompletedIgnoreCpuSla(pu, 2);
        assertEquals("Number of GSCs added", 8, getNumberOfGSCsAdded());
        assertEquals("Number of GSCs removed", 6, getNumberOfGSCsRemoved());
        
	}
    
}


