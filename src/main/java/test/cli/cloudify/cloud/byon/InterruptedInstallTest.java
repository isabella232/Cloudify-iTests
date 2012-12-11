package test.cli.cloudify.cloud.byon;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.openspaces.admin.application.Application;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.DeploymentStatus;
import org.openspaces.admin.pu.ProcessingUnit;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import framework.tools.SGTestHelper;
import framework.utils.AssertUtils;
import framework.utils.IRepetitiveRunnable;
import framework.utils.SSHUtils;

public class InterruptedInstallTest extends AbstractByonCloudTest {

    @BeforeClass(alwaysRun = true)
    protected void bootstrap() throws Exception {
        super.bootstrap();
    }

    @Test(timeOut = DEFAULT_TEST_TIMEOUT * 3, enabled = true)
    public void testPetclinic() throws IOException, InterruptedException{
        Future<Boolean> future = Executors.newSingleThreadExecutor().submit(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                installApplicationAndWait(SGTestHelper.getBuildDir() + "/recipes/apps/petclinic", "petclinic");
                return true;
            }
        });

        Application application = admin.getApplications().waitFor("petclinic", 5, TimeUnit.MINUTES);
        final ProcessingUnit processingUnit = application.getProcessingUnits().waitFor("petclinic.mongoConfig", 5, TimeUnit.MINUTES);
        processingUnit.waitFor(1, 5, TimeUnit.MINUTES);
        Machine machine = processingUnit.getInstances()[0].getMachine();
        SSHUtils.runCommand(machine.getHostAddress(), 5000, "killall -9 java", "tgrid", "tgrid");
        
        try {
            Assert.assertTrue("install failed", future.get());
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        AssertUtils.repetitive(new IRepetitiveRunnable() {
            
            @Override
            public void run() throws Exception {
                Assert.assertEquals("mongoConfig is not intact", DeploymentStatus.INTACT, processingUnit.getStatus());
            }
        }, 10 * 60 * 1000);
    }
    
}
