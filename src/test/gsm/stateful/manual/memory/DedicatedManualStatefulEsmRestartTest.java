package test.gsm.stateful.manual.memory;


import java.io.File;
import java.util.concurrent.TimeUnit;

import org.openspaces.admin.esm.ElasticServiceManager;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.elastic.ElasticStatefulProcessingUnitDeployment;
import org.openspaces.admin.pu.elastic.config.ManualCapacityScaleConfigurer;
import org.openspaces.core.util.MemoryUnit;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.gsm.AbstractGsmTest;
import test.gsm.GsmTestUtils;
import test.utils.AdminUtils;
import test.utils.DeploymentUtils;
import test.utils.LogUtils;
import test.utils.ProcessingUnitUtils;

public class DedicatedManualStatefulEsmRestartTest extends AbstractGsmTest {
    
    private File puDir ;
    
    private static final int EXPECTED_NUMBER_OF_CONTAINERS_AFTER_DEPLOYMENT = 2;
    private static final int EXPECTED_NUMBER_OF_CONTAINERS_AFTER_SCALE_OUT = 3;
    
    private static final int _512_MB_ = 512;
    private static final int _256_MB_ = 256;
    
    @Override 
    @BeforeMethod
    public void beforeTest() {
        super.beforeTest();

        LogUtils.log("Starting 1 GSM");
        AdminUtils.loadGSM(gsa); 
        
        DeploymentUtils.prepareApp("simpledata");
        puDir = DeploymentUtils.getProcessingUnit("simpledata", "processor");
    }
    
    /**
     * Checks that after ESM is restarted, and pu is redeployed, then it can recover from a container failover.
     * GS-9483
     */
    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = false)
    public void testElasticStatefulProcessingUnitDeployment() {

        admin.getGridServiceAgents().waitFor(1);

        assertEquals(0, getNumberOfGSCsAdded());
        assertEquals(0, getNumberOfGSCsRemoved());
                
        final ProcessingUnit pu = deployElasticProcessingUnit(gsm);       
       
        GsmTestUtils.waitForScaleToCompleteIgnoreCpuSla(pu, EXPECTED_NUMBER_OF_CONTAINERS_AFTER_DEPLOYMENT, OPERATION_TIMEOUT);
        
        assertEquals("Number of GSCs added", EXPECTED_NUMBER_OF_CONTAINERS_AFTER_DEPLOYMENT, getNumberOfGSCsAdded());
        assertEquals("Number of GSCs removed", 0, getNumberOfGSCsRemoved());
        
        restartTwoGsms(pu);        
        
        LogUtils.log("Restarting ESM");
        ElasticServiceManager esm = admin.getElasticServiceManagers().getManagers()[0];
        GsmTestUtils.restartElasticServiceManager(esm);
        
        int numberOfPojos = 1000;
        GsmTestUtils.writeData(pu, numberOfPojos);

        GridServiceContainer[] containers = admin.getGridServiceContainers().getContainers();
        GsmTestUtils.killContainer(containers[0]);
        
        // If the ESM has the elastic properties than it would maintain the number of containers SLA
        GsmTestUtils.waitForScaleToCompleteIgnoreCpuSla(pu, EXPECTED_NUMBER_OF_CONTAINERS_AFTER_DEPLOYMENT, OPERATION_TIMEOUT);
        
        assertEquals("Number of GSCs added + 1 failover recovery", EXPECTED_NUMBER_OF_CONTAINERS_AFTER_DEPLOYMENT+1, getNumberOfGSCsAdded());
        assertEquals("Only one new container is brought up", 1, getNumberOfGSCsRemoved());
        
        // make sure primary and backup not killed both
        assertEquals("Number of Person Pojos in space", numberOfPojos, GsmTestUtils.countData(pu));
        
        scaleOutProcessingUnit(pu);
        
        GsmTestUtils.waitForScaleToCompleteIgnoreCpuSla(pu, EXPECTED_NUMBER_OF_CONTAINERS_AFTER_SCALE_OUT, OPERATION_TIMEOUT);
        
        assertEquals("Number of GSCs added + 1 failover recovery", EXPECTED_NUMBER_OF_CONTAINERS_AFTER_SCALE_OUT + 1, getNumberOfGSCsAdded());
        
        restartTwoGsms(pu);   
        
        LogUtils.log("Restarting ESM");
        esm = admin.getElasticServiceManagers().getManagers()[0];
        GsmTestUtils.restartElasticServiceManager(esm);
        
        containers = admin.getGridServiceContainers().getContainers();
        GsmTestUtils.killContainer(containers[0]);
        GsmTestUtils.killContainer(containers[1]);
        
        assertTrue(admin.getGridServiceContainers().waitFor(EXPECTED_NUMBER_OF_CONTAINERS_AFTER_SCALE_OUT, OPERATION_TIMEOUT, TimeUnit.MILLISECONDS));
        
        assertEquals("Number of GSCs added + 3 failover recovery", EXPECTED_NUMBER_OF_CONTAINERS_AFTER_SCALE_OUT+3, getNumberOfGSCsAdded());
        
        assertEquals("Total number of GSCs removed", 3, getNumberOfGSCsRemoved());
        
   }

    private ProcessingUnit deployElasticProcessingUnit(GridServiceManager gsm) {
        final ProcessingUnit pu = gsm.deploy(
                new ElasticStatefulProcessingUnitDeployment(puDir)
                .maxMemoryCapacity(_256_MB_ * 3, MemoryUnit.MEGABYTES)
                .memoryCapacityPerContainer(_256_MB_, MemoryUnit.MEGABYTES)
                .scale(new ManualCapacityScaleConfigurer()
                       .memoryCapacity(_512_MB_,MemoryUnit.MEGABYTES)
                       .create())
                .singleMachineDeployment());
        return pu;
    }    
    
    private void scaleOutProcessingUnit(ProcessingUnit pu) {
        pu.scale(new ManualCapacityScaleConfigurer()
            .memoryCapacity(_256_MB_ * 3, MemoryUnit.MEGABYTES)
            .create());
    }
    
    private void restartTwoGsms(ProcessingUnit pu) {
        
        LogUtils.log("Restarting 2 GSMs");
        
        GridServiceManager gsm = pu.waitForManaged();
        GridServiceManager[] gsms = admin.getGridServiceManagers().getManagers();
        GridServiceManager backupGsm = gsms[0].equals(gsm) ? gsms[1] : gsms[0];
        
        GsmTestUtils.killGsm(gsm);
        ProcessingUnitUtils.waitForManaged(pu, backupGsm);
        GridServiceManager newGsm = AdminUtils.loadGSM(gsa);
        ProcessingUnitUtils.waitForBackupGsm(pu, newGsm);
        GsmTestUtils.killGsm(backupGsm);
        ProcessingUnitUtils.waitForManaged(pu, newGsm);
        GridServiceManager newBackupGsm = AdminUtils.loadGSM(gsa);
        ProcessingUnitUtils.waitForBackupGsm(pu, newBackupGsm);
        
    }
    
}


