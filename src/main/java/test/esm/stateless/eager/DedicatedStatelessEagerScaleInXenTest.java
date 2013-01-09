package test.gsm.stateless.eager.xen;

import java.io.File;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.openspaces.admin.pu.elastic.ElasticStatelessProcessingUnitDeployment;
import org.openspaces.admin.pu.elastic.config.DiscoveredMachineProvisioningConfigurer;
import org.openspaces.admin.pu.elastic.config.EagerScaleConfig;
import org.openspaces.admin.pu.events.ProcessingUnitInstanceLifecycleEventListener;
import org.openspaces.core.util.MemoryUnit;
import org.testng.annotations.Test;

import test.gsm.AbstractXenGSMTest;
import framework.utils.AssertUtils.RepetitiveConditionProvider;
import framework.utils.DeploymentUtils;
import framework.utils.LogUtils;

public class DedicatedStatelessEagerScaleInXenTest extends AbstractXenGSMTest {

    @Test(timeOut=DEFAULT_TEST_TIMEOUT, groups="1")
    public void doTest() {
        
        startNewVM(OPERATION_TIMEOUT,TimeUnit.MILLISECONDS);
        GridServiceAgent gsa3 = startNewVM(OPERATION_TIMEOUT,TimeUnit.MILLISECONDS);
        GridServiceAgent gsa4 = startNewVM(OPERATION_TIMEOUT,TimeUnit.MILLISECONDS);
        
        File archive = DeploymentUtils.getArchive("servlet.war");
        
        final ProcessingUnit pu = super.deploy(
                new ElasticStatelessProcessingUnitDeployment(archive)
                .memoryCapacityPerContainer(1, MemoryUnit.GIGABYTES)
 
                .dedicatedMachineProvisioning(
						new DiscoveredMachineProvisioningConfigurer()
						.reservedMemoryCapacityPerMachine(128, MemoryUnit.MEGABYTES)
						.create())
				.scale(new EagerScaleConfig())
        );
        
        final AtomicInteger removed = new AtomicInteger(0);
        final AtomicInteger added = new AtomicInteger(0);
        pu.addLifecycleListener(new ProcessingUnitInstanceLifecycleEventListener() {
			
			@Override
			public void processingUnitInstanceRemoved(
					ProcessingUnitInstance processingUnitInstance) {
				removed.incrementAndGet();
				
			}
			
			@Override
			public void processingUnitInstanceAdded(
					ProcessingUnitInstance processingUnitInstance) {
				added.incrementAndGet();
			}
		});
        
        pu.waitFor(admin.getGridServiceAgents().getSize());
        assertEquals(0,removed.get());
        assertEquals(4,added.get());
        
        shutdownMachine(gsa3.getMachine(), super.getMachineProvisioningConfig(), OPERATION_TIMEOUT);
        assertNumberOfInstances(3,pu);
        assertEquals(1,removed.get());
        assertEquals(4,added.get());
        
        shutdownMachine(gsa4.getMachine(), super.getMachineProvisioningConfig(), OPERATION_TIMEOUT);
        assertNumberOfInstances(2,pu);
        assertEquals(2,removed.get());
        assertEquals(4,added.get());
        
        assertUndeployAndWait(pu);

    }

    private void assertNumberOfInstances(final int expectedNumberOfInstances, final ProcessingUnit pu) {
        super.repetitiveAssertTrue(
                "number of instances", 
                new RepetitiveConditionProvider() {
                    
                    public boolean getCondition() {
                    	boolean condition = true;
                    	if (pu.getNumberOfInstances() != expectedNumberOfInstances) {
                    		LogUtils.log("Expected numberOfInstances " + expectedNumberOfInstances + " instead numberOfInstances is " + pu.getNumberOfInstances());
                    		condition = false;
                    	}
                    	
                    	if (pu.getInstances().length  != expectedNumberOfInstances) {
                    		LogUtils.log("Expected actual numberOfInstances " + expectedNumberOfInstances + " instead pu.getInstances().length is " + pu.getInstances().length);
                    		condition = false;
                    	}
                    	
                    	return condition;
                        
                    }
                },
                OPERATION_TIMEOUT);
    }
}
