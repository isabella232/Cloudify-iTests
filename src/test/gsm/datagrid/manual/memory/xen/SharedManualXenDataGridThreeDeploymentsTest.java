package test.gsm.datagrid.manual.memory.xen;


import java.util.concurrent.TimeUnit;

import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.elastic.ElasticMachineProvisioningConfig;
import org.openspaces.admin.pu.elastic.config.DiscoveredMachineProvisioningConfig;
import org.openspaces.admin.pu.elastic.config.ManualCapacityScaleConfigurer;
import org.openspaces.admin.space.ElasticSpaceDeployment;
import org.openspaces.core.util.MemoryUnit;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import test.gsm.AbstractXenGSMTest;

public class SharedManualXenDataGridThreeDeploymentsTest extends AbstractXenGSMTest {

    private static final String SHARING_ID = "sharedGroup";
    
    private boolean discoveredMachineProvisioning = false;

    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "2", enabled = false)
    public void manualXenDataGridDeploymentTwoIndenpendentXenMachineProvisioningTest() {
        manualXenDataGridDeploymentTwoIndenpendentTest(getMachineProvisioningConfig());
    }
    
    
    // TODO: Finish writing test.. Committed to serve as reference for GS-8810
    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "2", enabled = false)
    public void manualXenDataGridDeploymentTwoIndenpendentDiscoveredsMachineProvisioningTest() {
        manualXenDataGridDeploymentTwoIndenpendentTest(getDiscoveredMachineProvisioningConfig());
    }

    public void manualXenDataGridDeploymentTwoIndenpendentTest(ElasticMachineProvisioningConfig machineProvisioningConfig) {

        if (machineProvisioningConfig instanceof DiscoveredMachineProvisioningConfig) {
            setupDiscovereMachineProvisioningEnvironment();
        } 
        
        assertEquals(0, getNumberOfGSCsAdded());
        
        final ProcessingUnit pu = gsm.deploy(new ElasticSpaceDeployment("myGrid")
                .maxMemoryCapacity(2744, MemoryUnit.MEGABYTES)
                .memoryCapacityPerContainer(343,MemoryUnit.MEGABYTES)
                .highlyAvailable(false)
                .scale(new ManualCapacityScaleConfigurer()
                       .memoryCapacity(1372,MemoryUnit.MEGABYTES)
                       .create())
                .sharedMachineProvisioning(SHARING_ID, machineProvisioningConfig)
        );
        
        final ProcessingUnit pu2 = gsm.deploy(new ElasticSpaceDeployment("myGrid2")
            .maxMemoryCapacity(2744, MemoryUnit.MEGABYTES)
            .memoryCapacityPerContainer(343,MemoryUnit.MEGABYTES)
            .highlyAvailable(false)
            .scale(new ManualCapacityScaleConfigurer()
                .memoryCapacity(1372,MemoryUnit.MEGABYTES)
                .create())
            .sharedMachineProvisioning(SHARING_ID, machineProvisioningConfig)
        );
        
        final ProcessingUnit pu3 = gsm.deploy(new ElasticSpaceDeployment("myGrid3")
            .maxMemoryCapacity(2744, MemoryUnit.MEGABYTES)
            .memoryCapacityPerContainer(343,MemoryUnit.MEGABYTES)
            .highlyAvailable(false)
            .scale(new ManualCapacityScaleConfigurer()
                .memoryCapacity(1372,MemoryUnit.MEGABYTES)
                .create())
            .sharedMachineProvisioning(SHARING_ID, machineProvisioningConfig)
        );
        
        
        // wait and asserts
        if (isDiscoveredMachineProvisioning()) {
            startNewVMs(3, OPERATION_TIMEOUT*2, TimeUnit.MILLISECONDS);
        }
        
        ///
        
        final ProcessingUnit pu4 = gsm.deploy(new ElasticSpaceDeployment("myGrid4")
            .maxMemoryCapacity(2744, MemoryUnit.MEGABYTES)
            .memoryCapacityPerContainer(343,MemoryUnit.MEGABYTES)
            .highlyAvailable(false)
            .scale(new ManualCapacityScaleConfigurer()
                .memoryCapacity(2058,MemoryUnit.MEGABYTES)
                .create())
            .sharedMachineProvisioning(SHARING_ID, machineProvisioningConfig)
        );
        
        pu.scale(new ManualCapacityScaleConfigurer().memoryCapacity(2058, MemoryUnit.MEGABYTES).create());
        pu2.scale(new ManualCapacityScaleConfigurer().memoryCapacity(2058, MemoryUnit.MEGABYTES).create());
        pu3.scale(new ManualCapacityScaleConfigurer().memoryCapacity(2058, MemoryUnit.MEGABYTES).create());
        
        // maybe wait some
        pu2.getInstances()[0].restartAndWait();

        
        // some tests
        
        
        pu.scale(new ManualCapacityScaleConfigurer().memoryCapacity(1372, MemoryUnit.MEGABYTES).create());
        pu2.scale(new ManualCapacityScaleConfigurer().memoryCapacity(1372, MemoryUnit.MEGABYTES).create());
        pu3.scale(new ManualCapacityScaleConfigurer().memoryCapacity(1372, MemoryUnit.MEGABYTES).create());
        
        
        // wait
        
        
        pu.scale(new ManualCapacityScaleConfigurer().memoryCapacity(2744, MemoryUnit.MEGABYTES).create());
        pu2.scale(new ManualCapacityScaleConfigurer().memoryCapacity(2744, MemoryUnit.MEGABYTES).create());
        pu3.scale(new ManualCapacityScaleConfigurer().memoryCapacity(2744, MemoryUnit.MEGABYTES).create());
        
        pu4.undeploy();
        
        // wait some time
        
        
        
    }
    
    public void setupDiscovereMachineProvisioningEnvironment() {
        discoveredMachineProvisioning = true;
        startNewVMs(2, OPERATION_TIMEOUT*2, TimeUnit.MILLISECONDS);
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


