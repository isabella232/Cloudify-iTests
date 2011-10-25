package test.gsm.component.rebalancing;

import java.io.File;

import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitDeployment;
import org.testng.Assert;
import org.testng.annotations.Test;

import test.utils.AdminUtils;
import test.utils.DeploymentUtils;

public class StatelessRebalancingTest extends AbstractRebalancingSlaEnforcementTest {

    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1")
    public void statelessRebalancingTest() throws InterruptedException {
               
        //deploy stateless pu on 4 GSCs
        Assert.assertEquals(admin.getGridServiceContainers().getSize(), 0);
        GridServiceContainer[] containers = AdminUtils.loadGSCs(gsa, 4, ZONE);
        admin.getGridServiceContainers().waitFor(4);
        Assert.assertEquals(admin.getGridServiceContainers().getSize(), 4);
		//    File archive = DeploymentUtils.getArchive("servlet.war");
		File archive = DeploymentUtils.getArchive("simpleStatelessPu.jar");
		final ProcessingUnit pu = gsm.deploy(
		        new ProcessingUnitDeployment(archive)
		        .numberOfInstances(0)
		        .maxInstancesPerVM(1)
		        .addZone(ZONE));
		
        RebalancingTestUtils.enforceStatelessSlaAndWait(rebalancing, pu, containers);    
        
        RebalancingTestUtils.assertStatelessDeployment( pu, containers);
        
        // scale in - 2 containers
        GridServiceContainer[] scaleInContainers = new GridServiceContainer[] { containers[0] , containers[1] };
        RebalancingTestUtils.enforceStatelessSlaAndWait(rebalancing, pu, scaleInContainers);
        RebalancingTestUtils.assertStatelessDeployment( pu, scaleInContainers);
        
        // scale out - 4 containers
        RebalancingTestUtils.enforceStatelessSlaAndWait(rebalancing, pu, containers);
        RebalancingTestUtils.assertStatelessDeployment( pu, containers);
    }
}