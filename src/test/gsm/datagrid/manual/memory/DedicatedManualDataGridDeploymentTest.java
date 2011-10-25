package test.gsm.datagrid.manual.memory;

import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.elastic.config.DiscoveredMachineProvisioningConfigurer;
import org.openspaces.admin.pu.elastic.config.ManualCapacityScaleConfig;
import org.openspaces.admin.pu.elastic.config.ManualCapacityScaleConfigurer;
import org.openspaces.admin.space.ElasticSpaceDeployment;
import org.openspaces.core.util.MemoryUnit;
import org.testng.annotations.Test;

import test.gsm.AbstractGsmTest;
import test.gsm.GsmTestUtils;


/*
 * Tests Elastic Processing Unit deployment on SGTest agents
 *
 * Before running this test locally open a command prompt and run:
 * set LOOKUPGROUPS=itaif-laptop
 * set JSHOMEDIR=D:\eclipse_workspace\SGTest\tools\gigaspaces
 * start cmd /c "%JSHOMEDIR%\bin\gs-agent.bat gsa.global.esm 0 gsa.gsc 0 gsa.global.gsm 0 gsa.global.lus 1"
 *
 * on linux:
export LOOKUPGROUPS=gilad
export JSHOMEDIR=~/gigaspaces
nohup ${JSHOMEDIR}/bin/gs-agent.sh gsa.global.esm 0 gsa.gsc 0 gsa.global.gsm 0 gsa.global.lus 1 &
 * 
 * @author gilad
 */

public class DedicatedManualDataGridDeploymentTest extends AbstractGsmTest {

    
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "4")
    public void testManualDataGridDeploymentScale() {
		manualDataGridDeployment(true);
	}
	
    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "4")
    public void testManualDataGridDeployment() {
    	manualDataGridDeployment(false);
    }
    
    /**
     * @param scaleCommand - if true issues a separate scale command, otherwise merges with deploy command
     */
    void manualDataGridDeployment(boolean scaleCommand) {
    	
    	admin.getGridServiceAgents().waitFor(4); //2 management + 2 empty machines
   	    
    	assertEquals(0, getNumberOfGSCsAdded());
    	
    	int numberOfContainers = 4;
    	
        ElasticSpaceDeployment deployment = new ElasticSpaceDeployment("mygrid")
                .maxMemoryCapacity(256*numberOfContainers, MemoryUnit.MEGABYTES)
                .memoryCapacityPerContainer(256,MemoryUnit.MEGABYTES)
                .dedicatedMachineProvisioning(
                		new DiscoveredMachineProvisioningConfigurer()
                		.dedicatedManagementMachines()
                		.create());
        
        ManualCapacityScaleConfig manualCapacityScaleConfig = 
        	new ManualCapacityScaleConfigurer()
			.memoryCapacity(256*numberOfContainers,MemoryUnit.MEGABYTES)
			.create();
        
        if (!scaleCommand) {
        	deployment.scale(manualCapacityScaleConfig);
        }
        
		final ProcessingUnit pu = gsm.deploy(deployment);

		if (scaleCommand) {
			pu.scale(manualCapacityScaleConfig);
		}
    	
		GsmTestUtils.waitForScaleToCompleteIgnoreCpuSla(pu, numberOfContainers, OPERATION_TIMEOUT);
		assertEquals("Number of GSCs added", numberOfContainers, getNumberOfGSCsAdded());
        assertEquals("Number of GSCs removed", 0, getNumberOfGSCsRemoved());
    }
}


