package test.esm.datagrid.manual.memory;

import java.util.concurrent.TimeUnit;

import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.elastic.config.ManualCapacityScaleConfigurer;
import org.openspaces.admin.space.ElasticSpaceDeployment;
import org.openspaces.admin.space.Space;
import org.openspaces.cloud.xenserver.XenServerException;
import org.openspaces.core.util.MemoryUnit;
import org.openspaces.grid.gsm.machines.plugins.events.MachineStartRequestedEvent;
import org.openspaces.grid.gsm.machines.plugins.events.MachineStartedEvent;
import org.openspaces.grid.gsm.machines.plugins.events.MachineStopRequestedEvent;
import org.openspaces.grid.gsm.machines.plugins.events.MachineStoppedEvent;
import org.testng.annotations.Test;

import test.esm.AbstractFromXenToByonGSMTest;
import framework.utils.GsmTestUtils;

public class DedicatedManualXenDataGridCancelScaleOutTest extends AbstractFromXenToByonGSMTest{ 

    private static final int CONTAINER_MEMORY_IN_MEGABYTES = 1024;
    private static final int INITIAL_NUMBER_OF_CONTAINERS = 2;
    private static final int INITIAL_MEMORY_IN_MEGABYTES = CONTAINER_MEMORY_IN_MEGABYTES * INITIAL_NUMBER_OF_CONTAINERS;
	private static final int SCALEOUT_NUMBER_OF_CONTAINERS = 4;
	private static final int SCALEOUT_MEMORY_IN_MEGABYTES = CONTAINER_MEMORY_IN_MEGABYTES * SCALEOUT_NUMBER_OF_CONTAINERS;
	private final String gridName = "myspace";
	
    /**
     * This test should reproduce a case where scale-out is canceled 
     * and then it is re-applied again.
     * @author itaif
     * @throws InterruptedException 
     * @throws XenServerException 
     */
    
    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups="1", enabled = true, invocationCount=1)
    public void rebalancingAfterScaleInTest() throws XenServerException, InterruptedException {

		repetitiveAssertNumberOfMachineEvents(MachineStartRequestedEvent.class, 0, OPERATION_TIMEOUT);
        repetitiveAssertNumberOfMachineEvents(MachineStartedEvent.class, 0, OPERATION_TIMEOUT);
        repetitiveAssertNumberOfMachineEvents(MachineStopRequestedEvent.class, 0, OPERATION_TIMEOUT);
        repetitiveAssertNumberOfMachineEvents(MachineStoppedEvent.class, 0, OPERATION_TIMEOUT);
        
    	repetitiveAssertNumberOfGSCsAdded(0, OPERATION_TIMEOUT);
    	repetitiveAssertNumberOfGSAsAdded(1, OPERATION_TIMEOUT);
        
        final ProcessingUnit pu = super.deploy(
        		new ElasticSpaceDeployment(gridName)
                .maxMemoryCapacity(SCALEOUT_MEMORY_IN_MEGABYTES, MemoryUnit.MEGABYTES)
                .memoryCapacityPerContainer(CONTAINER_MEMORY_IN_MEGABYTES,MemoryUnit.MEGABYTES)
                .scale(new ManualCapacityScaleConfigurer()
                       .memoryCapacity(INITIAL_MEMORY_IN_MEGABYTES,MemoryUnit.MEGABYTES)
                       .atMostOneContainerPerMachine()
                       .create())
                .dedicatedMachineProvisioning(getMachineProvisioningConfig())
        );

        GsmTestUtils.waitForScaleToComplete(pu, INITIAL_NUMBER_OF_CONTAINERS, 2, OPERATION_TIMEOUT*4);
		
        repetitiveAssertNumberOfMachineEvents(MachineStartRequestedEvent.class, 1, OPERATION_TIMEOUT);
        repetitiveAssertNumberOfMachineEvents(MachineStartedEvent.class, 1, OPERATION_TIMEOUT);
        repetitiveAssertNumberOfMachineEvents(MachineStopRequestedEvent.class, 0, OPERATION_TIMEOUT);
        repetitiveAssertNumberOfMachineEvents(MachineStoppedEvent.class, 0, OPERATION_TIMEOUT);
        
        Space space = pu.waitForSpace(OPERATION_TIMEOUT*4, TimeUnit.MILLISECONDS);
        
        assertNotNull("Failed getting space instance", space);
        
        assertTrue("Failed waiting for space instances", 
                space.waitFor(space.getTotalNumberOfInstances(), 
                        OPERATION_TIMEOUT*4, TimeUnit.MILLISECONDS));
        
        pu.scale(new ManualCapacityScaleConfigurer()
                 .memoryCapacity(SCALEOUT_MEMORY_IN_MEGABYTES, MemoryUnit.MEGABYTES)
                 .atMostOneContainerPerMachine()
                 .create());
        
        repetitiveAssertNumberOfMachineEvents(MachineStartRequestedEvent.class, 3, OPERATION_TIMEOUT);
        repetitiveAssertNumberOfMachineEvents(MachineStartedEvent.class, 3, OPERATION_TIMEOUT);
        repetitiveAssertNumberOfMachineEvents(MachineStopRequestedEvent.class, 0, OPERATION_TIMEOUT);
        repetitiveAssertNumberOfMachineEvents(MachineStoppedEvent.class, 0, OPERATION_TIMEOUT);
        
        // cancel scale out
        pu.scale(new ManualCapacityScaleConfigurer()
                 .memoryCapacity(INITIAL_MEMORY_IN_MEGABYTES, MemoryUnit.MEGABYTES)
                 .create());
    
        GsmTestUtils.waitForScaleToComplete(pu, INITIAL_NUMBER_OF_CONTAINERS, 2 ,OPERATION_TIMEOUT*4);

        assertTrue("Failed waiting for space instances", 
                space.waitFor(space.getTotalNumberOfInstances(), 
                        OPERATION_TIMEOUT*4, TimeUnit.MILLISECONDS));
        
        repetitiveAssertNumberOfMachineEvents(MachineStartRequestedEvent.class, 3, OPERATION_TIMEOUT);
        repetitiveAssertNumberOfMachineEvents(MachineStartedEvent.class, 3, OPERATION_TIMEOUT);
        repetitiveAssertNumberOfMachineEvents(MachineStopRequestedEvent.class, 2, OPERATION_TIMEOUT);
        repetitiveAssertNumberOfMachineEvents(MachineStoppedEvent.class, 2, OPERATION_TIMEOUT);
        
        assertUndeployAndWait(pu);
        
        repetitiveAssertNumberOfMachineEvents(MachineStartRequestedEvent.class, 3, OPERATION_TIMEOUT);
        repetitiveAssertNumberOfMachineEvents(MachineStartedEvent.class, 3, OPERATION_TIMEOUT);
        repetitiveAssertNumberOfMachineEvents(MachineStopRequestedEvent.class, 3, OPERATION_TIMEOUT);
        repetitiveAssertNumberOfMachineEvents(MachineStoppedEvent.class, 3, OPERATION_TIMEOUT);        
    }
}
