package test.remoting;

import com.gigaspaces.example.IMyTestService;
import net.jini.core.transaction.CannotCommitException;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitDeployment;
import org.openspaces.core.GigaSpace;
import org.openspaces.remoting.EventDrivenRemotingProxyConfigurer;
import org.openspaces.remoting.ExecutorRemotingProxyConfigurer;
import org.springframework.transaction.TransactionSystemException;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import test.AbstractTestSuite;
import test.utils.AdminUtils;
import test.utils.DeploymentUtils;

import java.io.File;

public class RemotingExceptionHandlingTest extends AbstractTestSuite {

    private GigaSpace gigaSpace;
    private IMyTestService testService;

    @Override
    @BeforeClass
    public void beforeClass() {
        super.beforeClass();
        GridServiceAgent gsa = admin.getGridServiceAgents().waitForAtLeastOne();
        GridServiceManager gsm = AdminUtils.loadGSM(gsa);
        AdminUtils.loadGSC(gsa);
        File puFile = DeploymentUtils.getArchive("RemotingExceptionHandling.jar");
        ProcessingUnit pu = gsm.deploy(new ProcessingUnitDeployment(puFile));
        gigaSpace = pu.waitForSpace().getGigaSpace();
    }

    @Test(timeOut = DEFAULT_TEST_TIMEOUT)
    public void testUsingEventDrivenProxyHandleJiniTransaction() throws Exception {
        testService = new EventDrivenRemotingProxyConfigurer<IMyTestService>(gigaSpace, IMyTestService.class).proxy();
        try {
            testService.throwJiniException();
            Assert.fail("CannotCommitException should be thrown");
        } catch (TransactionSystemException e) {
            Assert.assertTrue(e.getCause() instanceof CannotCommitException);
        }
    }

    @Test(timeOut = DEFAULT_TEST_TIMEOUT)
    public void testUsingExecutorProxyHandleJiniTransaction() throws Exception {
        testService = new ExecutorRemotingProxyConfigurer<IMyTestService>(gigaSpace, IMyTestService.class).proxy();
        try {
            testService.throwJiniException();
            Assert.fail("CannotCommitException should be thrown");
        } catch (TransactionSystemException e) {
            Assert.assertTrue(e.getCause() instanceof CannotCommitException);
        }
    }


}
