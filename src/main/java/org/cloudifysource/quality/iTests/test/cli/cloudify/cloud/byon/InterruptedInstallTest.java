package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.byon;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
import org.junit.Assert;
import org.openspaces.admin.application.Application;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.DeploymentStatus;
import org.openspaces.admin.pu.ProcessingUnit;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.cloudifysource.quality.iTests.framework.tools.SGTestHelper;
import org.cloudifysource.quality.iTests.framework.utils.AssertUtils;
import org.cloudifysource.quality.iTests.framework.utils.IRepetitiveRunnable;
import org.cloudifysource.quality.iTests.framework.utils.SSHUtils;

public class InterruptedInstallTest extends AbstractByonCloudTest {

    @BeforeClass(alwaysRun = true)
    protected void bootstrap() throws Exception {
        super.bootstrap();
    }

    @Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 3, enabled = false)
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
        AssertUtils.assertNotNull("petclinic.mongoConfig failed to be discovered after waiting for 5 minutes");
        AssertUtils.assertTrue("admin failed to discover at least 1 instance of " + processingUnit.getName() + " after waiting for 5 minutes", processingUnit.waitFor(1, 5, TimeUnit.MINUTES));
        Machine machine = processingUnit.getInstances()[0].getMachine();
        SSHUtils.runCommand(machine.getHostAddress(), 5000, "killall -9 java", "tgrid", "tgrid");

        try {
            Assert.assertTrue("install failed after killing a machine during installation", future.get());
        } catch (ExecutionException e) {
            AssertUtils.assertFail("install failed after killing a machine during installation", e);
            
        }

        final ProcessingUnit[] processingUnits = application.getProcessingUnits().getProcessingUnits();
        AssertUtils.repetitive(new IRepetitiveRunnable() {

            @Override
            public void run() throws Exception {
                for (ProcessingUnit pu : processingUnits) {
                    Assert.assertEquals(pu.getName() + " is not intact", DeploymentStatus.INTACT, pu.getStatus());
                }
            }
        }, 10 * 60 * 1000);

    }
    
	@AfterClass(alwaysRun = true)
	protected void teardown() throws Exception {
		super.teardown();
	}
}
