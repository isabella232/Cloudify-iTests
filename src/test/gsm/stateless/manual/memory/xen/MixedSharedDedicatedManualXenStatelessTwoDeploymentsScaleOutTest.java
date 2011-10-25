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
/**
 * 
 *  Setup:
 *  
 *     
 *  A: Shared stateless deployment, total 1320m, per container 440m - expecting 3 containers on a single machine
 *  B: Dedicated stateless deployment, total 880m, per container 880m - expecting 1 container on 1 machine (different
 *  than A's machine)
 *  
 *  After Scale out:
 *  A: 1320m --> 1760m
 *  total 1760m, per container 440m - expecting 4 containers on 2 machines
 *  
 *  B: 880m --> 1760m
 *  total 1760m, per container 880m - expecting 2 containers on 2 machines (different than A's machins)
 *
 *  There are two versions: one with DiscoveredMachineProvisioningConfig and the other with
 *  XenServerMachineProvisioningConfig. 
 *
 * @author dank
 */
public class MixedSharedDedicatedManualXenStatelessTwoDeploymentsScaleOutTest extends AbstractXenGSMTest {

    private boolean discoveredMachineProvisioning = false;

    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = false)
    public void mixedSharedDedicatedManualXenStatelessDeploymentTwoIndenpendentIsolationXenMachineProvisioningTest() {
        mixedSharedDedicatedManualXenStatelessDeploymentTwoIndenpendentIsolationTest(
                getMachineProvisioningConfig());
    }
    
    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
    public void mixedSharedDedicatedManualXenStatelessDeploymentTwoIndenpendentIsolationDiscoveredsMachineProvisioningTest() {
        mixedSharedDedicatedManualXenStatelessDeploymentTwoIndenpendentIsolationTest(
                getDiscoveredMachineProvisioningConfig());
    }

    public void mixedSharedDedicatedManualXenStatelessDeploymentTwoIndenpendentIsolationTest(
            ElasticMachineProvisioningConfig machineProvisioningConfig) {

        if (machineProvisioningConfig instanceof DiscoveredMachineProvisioningConfig) {
            setupDiscovereMachineProvisioningEnvironment();
        } 
        
        File archive = DeploymentUtils.getArchive("servlet.war");
        
      	assertEquals(0, getNumberOfGSCsAdded());
        
        final ProcessingUnit puA = gsm.deploy(new ElasticStatelessProcessingUnitDeployment(archive)
                .name("A")
                .memoryCapacityPerContainer(440,MemoryUnit.MEGABYTES)
                .scale(new ManualCapacityScaleConfigurer()
                       .memoryCapacity(1320,MemoryUnit.MEGABYTES)
                       .create())
                .sharedMachineProvisioning("AShare", machineProvisioningConfig)
        );
        
        final ProcessingUnit puB = gsm.deploy(new ElasticStatelessProcessingUnitDeployment(archive)
                .name("B")
                .memoryCapacityPerContainer(880,MemoryUnit.MEGABYTES)
                .scale(new ManualCapacityScaleConfigurer()
                       .memoryCapacity(880,MemoryUnit.MEGABYTES)
                       .create())
                .dedicatedMachineProvisioning(machineProvisioningConfig)
        );
        
        assertTrue("Failed waiting for pu instances", puA.waitFor(3, OPERATION_TIMEOUT, TimeUnit.MILLISECONDS));
        assertTrue("Failed waiting for pu instances", puB.waitFor(1, OPERATION_TIMEOUT, TimeUnit.MILLISECONDS));
        
        assertEquals("Number of GSAs added", 2, getNumberOfGSAsAdded());
        
        assertEquals("Number of GSCs added", 4, getNumberOfGSCsAdded());
        assertEquals("Number of GSCs removed", 0, getNumberOfGSCsRemoved());
        
        assertEquals("Expecting deployment on 1 machine", 1, getNumberOfMachinesWithPUInstance(puA));
        assertEquals("Expecting deployment on 1 machine", 1, getNumberOfMachinesWithPUInstance(puB));
        
        assertEquals("Number of instances", 3, puA.getInstances().length);
        assertEquals("Number of instances", 1, puB.getInstances().length);
        
        assertDeploymentOnDifferentMachines(puA, puB);
    
        /* scale out */
        if (isDiscoveredMachineProvisioning()) {
            startNewVM(OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
            startNewVM(OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
        }
        
        puA.scale(new ManualCapacityScaleConfigurer().memoryCapacity(1760, MemoryUnit.MEGABYTES).create());
        puB.scale(new ManualCapacityScaleConfigurer().memoryCapacity(1760, MemoryUnit.MEGABYTES).create());
        
        assertTrue("Failed waiting for pu instances", puA.waitFor(4, OPERATION_TIMEOUT, TimeUnit.MILLISECONDS));
        assertTrue("Failed waiting for pu instances", puB.waitFor(2, OPERATION_TIMEOUT, TimeUnit.MILLISECONDS));
        
        assertEquals("Number of GSAs added", 4, getNumberOfGSAsAdded());
        
        assertEquals("Expecting deployment on 2 machines", 2, getNumberOfMachinesWithPUInstance(puA));
        assertEquals("Expecting deployment on 2 machines", 2, getNumberOfMachinesWithPUInstance(puB));
        
        assertDeploymentOnDifferentMachines(puA, puB);
        
    }
	
    private int getNumberOfMachinesWithPUInstance(ProcessingUnit pu) {
        Set<Machine> puMachines = new HashSet<Machine>();
        for (ProcessingUnitInstance instance : pu.getInstances()) {
            puMachines.add(instance.getMachine());
        }
        
        return puMachines.size();
    }
    
    private void assertDeploymentOnDifferentMachines(ProcessingUnit puA, ProcessingUnit puB) {
        
        Set<Machine> puAMachines = new HashSet<Machine>();
        for (ProcessingUnitInstance instance : puA.getInstances()) {
            puAMachines.add(instance.getMachine());
        }
        
        Set<Machine> puBMachines = new HashSet<Machine>();
        for (ProcessingUnitInstance instance : puB.getInstances()) {
            puBMachines.add(instance.getMachine());
        }

        boolean isolated = true;
        
        for (Machine machine : puAMachines) {
            if (puBMachines.contains(machine)) {
                isolated = false;
            }
        }
        
        for (Machine machine : puBMachines) {
            if (puAMachines.contains(machine)) {
                isolated = false;
            }
        }
        
        assertTrue("Expecting isolation between two PUs", isolated);
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


