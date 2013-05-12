package org.cloudifysource.quality.iTests.test.esm.datagrid.manual.advanced;

import java.util.concurrent.TimeUnit;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.elastic.config.ManualCapacityScaleConfigurer;
import org.openspaces.admin.space.ElasticSpaceDeployment;
import org.openspaces.core.util.MemoryUnit;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.cloudifysource.quality.iTests.test.esm.AbstractFromXenToByonGSMTest;
import iTests.framework.utils.GsmTestUtils;

/**
 * @author itaif
 * GS-10332 - undeployAndWait of elastic pu may timeout
 */
public class ZeroBackupsFailoverByonTest extends AbstractFromXenToByonGSMTest {
    
	@BeforeMethod
    public void beforeTest() {
		super.beforeTestInit();
	}
	
	@BeforeClass
	protected void bootstrap() throws Exception {
		super.bootstrapBeforeClass();
	}
	
	@AfterMethod
    public void afterTest() {
		super.afterTest();
	}
	
	@AfterClass(alwaysRun = true)
	protected void teardownAfterClass() throws Exception {
		super.teardownAfterClass();
	}
	
    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = SUSPECTED) //TWO BACKUPS ESM IS NOT SUPPORTED DUE TO PRIMARY REBALANCING ISSUES
    public void doTest() throws Exception {
        repetitiveAssertNumberOfGSCsAdded(0, OPERATION_TIMEOUT);
        repetitiveAssertNumberOfGSAsAdded(1, OPERATION_TIMEOUT);

        final int NUM_INSTANCES_PER_CONTAINER=2;
        final int NUM_CONTAINERS = 6;
        final int MEM_PER_CONTAINER = 256;
        final int NUM_MACHINES = 3;
        
        final ProcessingUnit pu = super.deploy(
                new ElasticSpaceDeployment("nobackup_space")
                .numberOfBackupsPerPartition(0)                
                .maxMemoryCapacity(NUM_INSTANCES_PER_CONTAINER*NUM_CONTAINERS*MEM_PER_CONTAINER, MemoryUnit.MEGABYTES)
                .maxNumberOfCpuCores(NUM_MACHINES*4)
                .memoryCapacityPerContainer(MEM_PER_CONTAINER,MemoryUnit.MEGABYTES)
                .dedicatedMachineProvisioning(getMachineProvisioningConfig())
                .scale(new ManualCapacityScaleConfigurer()
                       .memoryCapacity(NUM_CONTAINERS*MEM_PER_CONTAINER, MemoryUnit.MEGABYTES)
                       .numberOfCpuCores(NUM_MACHINES*getMachineProvisioningConfig().getMinimumNumberOfCpuCoresPerMachine())
                       .create())
        );
        
        assertEquals("Number of pu backups", pu.getNumberOfBackups(), 0);
        assertEquals("Number of pu instances", pu.getTotalNumberOfInstances(), NUM_INSTANCES_PER_CONTAINER*NUM_CONTAINERS);
        
        GsmTestUtils.waitForScaleToComplete(pu, NUM_CONTAINERS, NUM_MACHINES, OPERATION_TIMEOUT);
        
        repetitiveAssertNumberOfGSCsAdded(NUM_CONTAINERS, OPERATION_TIMEOUT);
        repetitiveAssertNumberOfGSCsRemoved(0, OPERATION_TIMEOUT);
        
        GridServiceAgent[] gsas = admin.getGridServiceAgents().getAgents();
        Machine managerMachine = admin.getGridServiceManagers().getManagers()[0].getMachine();
        int ctr = 0;
        for (int i = 0; i < gsas.length; i++) {
            Machine curMachine = gsas[i].getMachine();
            if (!curMachine.equals(managerMachine)) {
                stopByonMachine(getElasticMachineProvisioningCloudifyAdapter(), gsas[i], OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
                if (++ctr == 2) break;
            }
        }
        assertEquals("Two machines killed", ctr, 2);
                        
        GsmTestUtils.waitForScaleToComplete(pu, NUM_CONTAINERS, NUM_MACHINES, OPERATION_TIMEOUT*2);
        
        assertUndeployAndWait(pu);
    }

}
    


