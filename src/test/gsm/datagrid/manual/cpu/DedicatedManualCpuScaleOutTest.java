package test.gsm.datagrid.manual.cpu;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.elastic.config.ManualCapacityScaleConfigurer;
import org.openspaces.admin.space.ElasticSpaceDeployment;
import org.openspaces.core.internal.commons.math.fraction.Fraction;
import org.openspaces.core.util.MemoryUnit;
import org.openspaces.grid.gsm.capacity.CpuCapacityRequirement;
import org.openspaces.grid.gsm.capacity.MachineCapacityRequirements;
import org.openspaces.grid.gsm.machines.MachinesSlaUtils;
import org.testng.annotations.Test;

import test.gsm.AbstractGsmTest;
import test.gsm.GsmTestUtils;
import test.utils.LogUtils;

public class DedicatedManualCpuScaleOutTest  extends AbstractGsmTest {

	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "4")
    public void testManualDataGridDeployment() {
   	    
    	admin.getGridServiceAgents().waitFor(4);
    	GridServiceAgent[] agents = admin.getGridServiceAgents().getAgents();
    	Fraction cores = Fraction.ZERO;
    	Fraction cpu0 = getCpu(agents[0].getMachine());
        cores = cores.add(cpu0);
    	LogUtils.log(MachinesSlaUtils.machineToString(agents[0].getMachine()) + " has " + cpu0 + " cores");
    	Fraction cpu1 = getCpu(agents[1].getMachine());
        cores = cores.add(cpu1);
    	LogUtils.log(MachinesSlaUtils.machineToString(agents[1].getMachine()) + " has " + cpu1 + " cores");
    	Fraction cpu2 = getCpu(agents[2].getMachine());
        cores = cores.add(cpu2);
    	LogUtils.log(MachinesSlaUtils.machineToString(agents[2].getMachine()) + " has " + cpu2 + " cores");
    	Fraction cpu3 = getCpu(agents[3].getMachine());
        cores = cores.add(cpu3);
    	LogUtils.log(MachinesSlaUtils.machineToString(agents[3].getMachine()) + " has " + cpu3 + " cores");
    	
    	assertEquals(0, getNumberOfGSCsAdded());
    	        
        final ProcessingUnit pu = gsm.deploy(new ElasticSpaceDeployment("mygrid")
                .maxMemoryCapacity(512, MemoryUnit.MEGABYTES)
                .memoryCapacityPerContainer(256, MemoryUnit.MEGABYTES)
                .maxNumberOfCpuCores((int)cores.doubleValue())
                .scale(
                		new ManualCapacityScaleConfigurer()
                		.numberOfCpuCores(cores.doubleValue())
                		.create())
        );
                
        int expectedNumberOfContainers = 4;
        assertEquals(expectedNumberOfContainers,pu.getNumberOfInstances());
         
        GsmTestUtils.waitForScaleToComplete(pu, expectedNumberOfContainers, OPERATION_TIMEOUT);
        
        assertEquals("Number of GSCs added", expectedNumberOfContainers, getNumberOfGSCsAdded());
        assertEquals("Number of GSCs removed", 0, getNumberOfGSCsRemoved());
        
        for (GridServiceAgent agent : agents) {
        	assertTrue("machine " + agent.getMachine() + " is not used",agent.getMachine().getGridServiceContainers().getSize() > 0);
        }
    }
	
	private Fraction getCpu(Machine machine) {
		return new MachineCapacityRequirements(machine).getRequirement(new CpuCapacityRequirement().getType()).getCpu();
	}

}
