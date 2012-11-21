package test.usm.cli;

import framework.tools.SGTestHelper;
import framework.utils.AdminUtils;

import org.apache.sshd.ClientChannel;
import org.apache.sshd.ClientSession;
import org.apache.sshd.SshClient;
import org.apache.sshd.client.future.ConnectFuture;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitDeployment;
import org.testng.annotations.BeforeMethod;
import test.AbstractTest;

import java.io.File;
import java.text.MessageFormat;

public class USMCliRestDeploymentTest extends AbstractTest {

    private GridServiceAgent gsa;
    private GridServiceManager gsm;
    private GridServiceContainer gsc;
    private ProcessingUnit restful;
    protected static final String REST_WAR_PATH = SGTestHelper.getBuildDir()
			+ MessageFormat.format("{0}tools{0}rest{0}rest.war", File.separatorChar);
    @BeforeMethod
    @Override
    public void beforeTest() {
        super.beforeTest();
        gsa = admin.getGridServiceAgents().waitForAtLeastOne();
        gsm = AdminUtils.loadGSM(gsa);
        gsc = AdminUtils.loadGSC(gsa);
        restful = gsm.deploy(new ProcessingUnitDeployment(REST_WAR_PATH));
    }
    
//    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1")
    public void test() throws Exception {
        SshClient client = SshClient.setUpDefaultClient();
        client.start();
        ConnectFuture future = client.connect(gsc.getMachine().getHostAddress(), 8080);
        future.await();
        ClientSession session = future.getSession();
        session.authPassword("dank", "dan1324");
        ClientChannel channel = session.createChannel("shell");
        channel.setIn(System.in);
        channel.setOut(System.out);
        channel.setErr(System.err);
        channel.open();
        channel.waitFor(ClientChannel.CLOSED, 0);
    }
    
}
