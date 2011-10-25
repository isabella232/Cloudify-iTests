package test.gsm.datagrid.manual.advanced.xen;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.elastic.config.ManualCapacityScaleConfigurer;
import org.openspaces.admin.space.ElasticSpaceDeployment;
import org.openspaces.core.util.MemoryUnit;
import org.testng.annotations.Test;

import test.gsm.AbstractXenGSMTest;
import test.gsm.GsmTestUtils;

/**
 * @author itaif
 */
public class ZeroBackupsFailoverXenTest extends AbstractXenGSMTest {
    
    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1") //TWO BACKUPS ESM IS NOT SUPPORTED DUE TO PRIMARY REBALANCING ISSUES
    public void doTest() {
        assertEquals(0, getNumberOfGSCsAdded());
        assertEquals(1, getNumberOfGSAsAdded());

        final int NUM_INSTANCES_PER_CONTAINER=2;
        final int NUM_CONTAINERS = 6;
        final int MEM_PER_CONTAINER = 256;
        final int NUM_MACHINES = 3;
        
        final ProcessingUnit pu = gsm.deploy(
                new ElasticSpaceDeployment("nobackup_space")
                .numberOfBackupsPerPartition(0)                
                .maxMemoryCapacity(NUM_INSTANCES_PER_CONTAINER*NUM_CONTAINERS*MEM_PER_CONTAINER, MemoryUnit.MEGABYTES)
                .maxNumberOfCpuCores(NUM_MACHINES*4)
                .memoryCapacityPerContainer(MEM_PER_CONTAINER,MemoryUnit.MEGABYTES)
                .dedicatedMachineProvisioning(getMachineProvisioningConfig())
                .scale(new ManualCapacityScaleConfigurer()
                       .memoryCapacity(NUM_CONTAINERS*MEM_PER_CONTAINER, MemoryUnit.MEGABYTES)
                       .numberOfCpuCores(NUM_MACHINES*getMachineProvisioningConfig().getNumberOfCpuCoresPerMachine())
                       .create())
        );
        
        assertEquals("Number of pu backups", pu.getNumberOfBackups(), 0);
        assertEquals("Number of pu instances", pu.getTotalNumberOfInstances(), NUM_INSTANCES_PER_CONTAINER*NUM_CONTAINERS);
        
        GsmTestUtils.waitForScaleToComplete(pu, NUM_CONTAINERS, NUM_MACHINES, OPERATION_TIMEOUT);
        
        assertEquals("Number of GSCs added", NUM_CONTAINERS, getNumberOfGSCsAdded());
        assertEquals("Number of GSCs removed", 0, getNumberOfGSCsRemoved());
        
        GridServiceAgent[] gsas = admin.getGridServiceAgents().getAgents();
        Machine managerMachine = admin.getGridServiceManagers().getManagers()[0].getMachine();
        int ctr = 0;
        for (int i = 0; i < gsas.length; i++) {
            Machine curMachine = gsas[i].getMachine();
            if (!curMachine.equals(managerMachine)) {
                GsmTestUtils.shutdownMachine(curMachine, super.getMachineProvisioningConfig(), OPERATION_TIMEOUT);
                if (++ctr == 2) break;
            }
        }
        assertEquals("Two machines killed", ctr, 2);
                        
        GsmTestUtils.waitForScaleToComplete(pu, NUM_CONTAINERS, NUM_MACHINES, OPERATION_TIMEOUT*2);                
    }

}
    


