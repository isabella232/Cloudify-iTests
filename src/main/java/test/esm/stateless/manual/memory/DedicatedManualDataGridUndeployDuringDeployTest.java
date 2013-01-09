package test.gsm.stateless.manual.memory.xen;

import java.io.File;
import java.util.concurrent.TimeUnit;

import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.elastic.ElasticStatelessProcessingUnitDeployment;
import org.openspaces.admin.pu.elastic.config.ManualCapacityScaleConfigurer;
import org.openspaces.core.util.MemoryUnit;
import org.testng.annotations.Test;

import test.gsm.AbstractXenGSMTest;
import test.gsm.GsmTestUtils;
import framework.utils.DeploymentUtils;
import framework.utils.LogUtils;


public class DedicatedManualDataGridUndeployDuringDeployTest extends AbstractXenGSMTest {

	private static final int REDEPLOY_ITERATIONS = 5;

	/**
	 * @see GS-10644
	 */
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled=false)
    public void testManualDataGridDeploymentScale() {
        
		final File archive = DeploymentUtils.getArchive("servlet.war");
	    
        ProcessingUnit pu = null;
        for (int i = 0 ; i < REDEPLOY_ITERATIONS; i++) {
        	LogUtils.log("Deploying iteration "+ i);
        	final ElasticStatelessProcessingUnitDeployment deployment =
        			new ElasticStatelessProcessingUnitDeployment(archive)
        	.memoryCapacityPerContainer(1, MemoryUnit.GIGABYTES)
        	.dedicatedMachineProvisioning(getMachineProvisioningConfigWithMachineZone(new String[] {"iter"+i}))
        	.scale(new ManualCapacityScaleConfigurer()
        	.memoryCapacity(2, MemoryUnit.GIGABYTES)
        	.create());
	        pu = super.deploy(deployment);
	
	        GsmTestUtils.waitForScaleToComplete(pu, 2, OPERATION_TIMEOUT);
	        LogUtils.log("Undeploying iteration "+ i);
	        pu.undeploy();
        }
        
        LogUtils.log("UndeployAndWait");
        GsmTestUtils.assertUndeployAndWait(pu, OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);        
	}
}