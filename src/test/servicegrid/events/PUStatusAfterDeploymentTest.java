package test.servicegrid.events;

import static test.utils.AdminUtils.loadGSC;
import static test.utils.AdminUtils.loadGSM;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.pu.DeploymentStatus;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.space.SpaceDeployment;
import org.testng.annotations.Test;

import test.AbstractTest;
import test.utils.AssertUtils.RepetitiveConditionProvider;

public class PUStatusAfterDeploymentTest extends AbstractTest
{
    private static final String SPACE_NAME = "testSpace";

    @Test(timeOut = DEFAULT_TEST_TIMEOUT)
    public void test(){
        GridServiceAgent gsa = admin.getGridServiceAgents().waitForAtLeastOne();
        GridServiceManager gsm = loadGSM(gsa);
        loadGSC(gsa);
        
        gsm.deploy(new SpaceDeployment(SPACE_NAME));
        final ProcessingUnit processingUnit = admin.getProcessingUnits().waitFor(SPACE_NAME);
        
        processingUnit.waitFor(processingUnit.getTotalNumberOfInstances());
        
        RepetitiveConditionProvider condition = new RepetitiveConditionProvider() {
            public boolean getCondition() {
                return processingUnit.getStatus() == DeploymentStatus.INTACT;
            }
        };
        
        repetitiveAssertTrue("Expected PU deployment status", condition, OPERATION_TIMEOUT);
        
    }
}
