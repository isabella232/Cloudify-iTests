package test.gsm.stateless.manual.memory.xen;


import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.openspaces.admin.pu.elastic.ElasticMachineProvisioningConfig;
import org.openspaces.admin.pu.elastic.ElasticStatelessProcessingUnitDeployment;
import org.openspaces.admin.pu.elastic.config.DiscoveredMachineProvisioningConfig;
import org.openspaces.admin.pu.elastic.config.ManualCapacityScaleConfigurer;
import org.openspaces.core.util.MemoryUnit;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import test.gsm.AbstractXenGSMTest;
import test.utils.DeploymentUtils;
import test.utils.LogUtils;
import test.utils.AssertUtils.RepetitiveConditionProvider;
/**
 * Tests deployment of four different spaces with different memory sizes. 
 *  *
 * Setup: (on two machines)
 * A: 330m total, 165m per container
 * B: 330m total, 165m per container
 * C: 660m total, 330m per container
 * D: 1320m total, 660m per container
 *  
 * After scale up: (third machine)
 * A: m total
 * D: m total
 * 
 *  there are two versions: one with DiscoveredMachineProvisioningConfig and the other with
 *  XenServerMachineProvisioningConfig. 
 * 
 * @author dank
 */
public class SharedStatelessManualXenFourMixedDeploymentsTest extends AbstractXenGSMTest {

    private static final String SHARING_ID = "sharedGroup";
    
    private boolean discoveredMachineProvisioning = false;

    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = false)
    public void manualXenStatelessDeploymentFourMixedDeploymentsXenMachineProvisioningTest() {
        manualXenStatelessDeploymentFourMixedDeploymentsTest(getMachineProvisioningConfig());
    }
    
    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
    public void manualXenStatelessDeploymentFourMixedDeploymentsDiscoveredsMachineProvisioningTest() {
        manualXenStatelessDeploymentFourMixedDeploymentsTest(getDiscoveredMachineProvisioningConfig());
    }
    
    public void manualXenStatelessDeploymentFourMixedDeploymentsTest(ElasticMachineProvisioningConfig machineProvisioningConfig) {
   	    
        if (machineProvisioningConfig instanceof DiscoveredMachineProvisioningConfig) {
            setupDiscovereMachineProvisioningEnvironment();
        } 
        
        File archive = DeploymentUtils.getArchive("servlet.war");
        
        /* make sure we have a clean environment */
    	assertEquals(0, getNumberOfGSCsAdded());
        
    	/* deploy 4 stateless PUs according to setup described above */
        final ProcessingUnit puA = gsm.deploy(new ElasticStatelessProcessingUnitDeployment(archive)
                .name("A")
                .memoryCapacityPerContainer(165,MemoryUnit.MEGABYTES)
                .scale(new ManualCapacityScaleConfigurer()
                       .memoryCapacity(330,MemoryUnit.MEGABYTES)
                       .create())
                .sharedMachineProvisioning(SHARING_ID, machineProvisioningConfig)
        );
        final ProcessingUnit puB = gsm.deploy(new ElasticStatelessProcessingUnitDeployment(archive)
                .name("B")
                .memoryCapacityPerContainer(165,MemoryUnit.MEGABYTES)
                .scale(new ManualCapacityScaleConfigurer()
                       .memoryCapacity(330,MemoryUnit.MEGABYTES)
                       .create())
                .sharedMachineProvisioning(SHARING_ID, machineProvisioningConfig)
        );
        final ProcessingUnit puC = gsm.deploy(new ElasticStatelessProcessingUnitDeployment(archive)
                .name("C")
                .memoryCapacityPerContainer(330,MemoryUnit.MEGABYTES)
                .scale(new ManualCapacityScaleConfigurer()
                       .memoryCapacity(660,MemoryUnit.MEGABYTES)
                       .create())
                .sharedMachineProvisioning(SHARING_ID, machineProvisioningConfig)
        );
        final ProcessingUnit puD = gsm.deploy(new ElasticStatelessProcessingUnitDeployment(archive)
                .name("D")
                .memoryCapacityPerContainer(660,MemoryUnit.MEGABYTES)
                .scale(new ManualCapacityScaleConfigurer()
                       .memoryCapacity(1320,MemoryUnit.MEGABYTES)
                       .create())
                .sharedMachineProvisioning(SHARING_ID, machineProvisioningConfig)
        );
        
        assertTrue("Failed waiting for pu instances", puA.waitFor(2, OPERATION_TIMEOUT, TimeUnit.MILLISECONDS));
        assertTrue("Failed waiting for pu instances", puB.waitFor(2, OPERATION_TIMEOUT, TimeUnit.MILLISECONDS));
        assertTrue("Failed waiting for pu instances", puC.waitFor(2, OPERATION_TIMEOUT, TimeUnit.MILLISECONDS));
        assertTrue("Failed waiting for pu instances", puD.waitFor(2, OPERATION_TIMEOUT, TimeUnit.MILLISECONDS));
        
        assertEquals("Number of GSAs added", 2, getNumberOfGSAsAdded());
        
        assertEquals("Number of GSCs added", 8, getNumberOfGSCsAdded());
        assertEquals("Number of GSCs removed", 0, getNumberOfGSCsRemoved());
       
        Set<Machine> machines = new HashSet<Machine>();

        /* prepare machines set */
		machines.add(puA.getInstances()[0].getMachine());
		machines.add(puA.getInstances()[1].getMachine());
		machines.add(puB.getInstances()[0].getMachine());
		machines.add(puB.getInstances()[1].getMachine());
		machines.add(puC.getInstances()[0].getMachine());
		machines.add(puC.getInstances()[1].getMachine());
		machines.add(puD.getInstances()[0].getMachine());
		machines.add(puD.getInstances()[1].getMachine());
		
		assertEquals("PUs should be shared on two machines", 2, machines.size());
		
		/* scale up 'A' and 'D' */
		
		if (isDiscoveredMachineProvisioning()) {
		    startNewVM(OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
		}
		
		/*
		 * TODO: GS-8808 - verify with Itai.
		 * If we request to scale A before we request to scale D, we might run into a situation that the rebalancing of A,
		 * would cause all instances of A to occupy  a full machine. Thus leaving us with no more room for scaling D without requesting a new machine.
		 */
		
		//add 1 more container for D
		puD.scale(new ManualCapacityScaleConfigurer().memoryCapacity(1980, MemoryUnit.MEGABYTES).create());

		//add 4 more containers for A
		puA.scale(new ManualCapacityScaleConfigurer().memoryCapacity(990, MemoryUnit.MEGABYTES).create());

		RepetitiveConditionProvider condition = new RepetitiveConditionProvider() {
            public boolean getCondition() {
            	LogUtils.log("Expected: 13 GSCs, Actual: " + getNumberOfGSCsAdded()+ " GSCs.");
                return getNumberOfGSCsAdded() == 13;
            }
        };
		
        //total containers 8+4+1=13
        repetitiveAssertTrue("Expecting 13 GSCs at this point", condition, OPERATION_TIMEOUT);
        
        assertTrue("Failed waiting for pu instances", puA.waitFor(6, OPERATION_TIMEOUT, TimeUnit.MILLISECONDS));
        assertTrue("Failed waiting for pu instances", puB.waitFor(2, OPERATION_TIMEOUT, TimeUnit.MILLISECONDS));
        assertTrue("Failed waiting for pu instances", puC.waitFor(2, OPERATION_TIMEOUT, TimeUnit.MILLISECONDS));
        assertTrue("Failed waiting for pu instances", puD.waitFor(3, OPERATION_TIMEOUT, TimeUnit.MILLISECONDS));
        
        assertEquals("Number of GSAs added", 3, getNumberOfGSAsAdded());
        
        assertEquals("Number of GSCs removed", 0, getNumberOfGSCsRemoved());
		
        /* make sure only A and D grew */
        assertEquals(6, puA.getTotalNumberOfInstances());
        assertEquals(3, puD.getTotalNumberOfInstances());

        asserInstanceOnNewMachine(puA, machines);
        asserInstanceOnNewMachine(puD, machines);
        
    }
    
    public void asserInstanceOnNewMachine(ProcessingUnit pu,  Set<Machine> previousMachines) {
        Set<Machine> machines = new HashSet<Machine>();
        for (ProcessingUnitInstance instance : pu.getInstances()) {
            machines.add(instance.getMachine());
        }
        
        machines.removeAll(previousMachines);
        assertEquals("Expecting an instance on the new machine", 1, machines.size());
    }
    
    public void setupDiscovereMachineProvisioningEnvironment() {
        discoveredMachineProvisioning = true;
        startNewVM(OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
    }

    public boolean isDiscoveredMachineProvisioning() {
        return discoveredMachineProvisioning;
    }
    
    public boolean isXenMachineProvisioning() {
        return !isDiscoveredMachineProvisioning();
    }
    
    @Override
    @AfterMethod
    public void afterTest() {
        discoveredMachineProvisioning = false;
        super.afterTest();
    }
    
}


