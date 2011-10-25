package test.servicegrid.cleanup;

import static test.utils.AdminUtils.loadGSCs;
import static test.utils.LogUtils.log;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitDeployment;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.openspaces.admin.pu.events.ProcessingUnitInstanceRemovedEventListener;
import org.testng.annotations.Test;

import test.AbstractTest;
import test.utils.AdminUtils;
import test.utils.DeploymentUtils;

/**
 * Tests decrement instance
 * @author itaif
 *
 */
public class DecrementInstanceTest extends AbstractTest {

    @Test(timeOut=DEFAULT_TEST_TIMEOUT, groups = "1")
    public void failoverTest() throws InterruptedException {
                
        GridServiceAgent gsa = admin.getGridServiceAgents().waitForAtLeastOne();
        Machine machine = gsa.getMachine();
        
        log("loading 1 GSM, 2 GSCs on " + machine.getHostName());
        AdminUtils.loadGSM(machine.getGridServiceAgent());
        loadGSCs(machine, 2);
        admin.getGridServiceContainers().waitFor(2);
        assertEquals(2,admin.getGridServiceContainers().getSize());
        assertEquals(1,admin.getGridServiceManagers().getSize());
        
    
        log("deploy PU when 2 GSMs and 2GSCs already running");
        File archive = DeploymentUtils.getArchive("servlet.war");
        ProcessingUnit pu = deploy2InstancesOn2Containers(archive);
        pu.waitForManaged();
        
        ProcessingUnitInstance instance = pu.getInstances()[0];
        final CountDownLatch latch = new CountDownLatch(1);
        ProcessingUnitInstanceRemovedEventListener eventListener = new ProcessingUnitInstanceRemovedEventListener() {
            
            public void processingUnitInstanceRemoved(
                    ProcessingUnitInstance processingUnitInstance) {
                latch.countDown();
            }
        };
        admin.getProcessingUnits().getProcessingUnitInstanceRemoved().add(eventListener);
        try {
            instance.decrement();
            latch.await(30,TimeUnit.SECONDS);
        }
        finally {
            admin.getProcessingUnits().getProcessingUnitInstanceRemoved().remove(eventListener);
        }
    }

    private ProcessingUnit deploy2InstancesOn2Containers(File archive) {
        // requesting 3 instances, although we have only 2 GSCs
        final ProcessingUnit pu = admin.getGridServiceManagers().deploy(
                new ProcessingUnitDeployment(archive)
                .numberOfInstances(2)
                .maxInstancesPerVM(1));
        
        pu.waitFor(2);
        return pu;
    }

    }
