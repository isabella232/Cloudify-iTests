package test.web;

import static test.utils.AdminUtils.loadGSC;
import static test.utils.AdminUtils.loadGSM;
import static test.utils.DeploymentUtils.getArchive;
import static test.utils.LogUtils.log;

import java.io.IOException;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitDeployment;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.gargoylesoftware.htmlunit.html.HtmlPage;

/***
 * 
 * Setup: 1 machine, 1 gsa, 1 gsm, 1 gsc
 * Tests:
 * 
 * - 4.6.1: Deploy 1 instances o servlet webapp. (plain mode) with property
 * web.port=8090.
 * Verify webapp is accessible by:
 * http://IP:8090/servlet
 * 
 * Verify webapp is not accessible by:
 * http://IP:8080/servlet
 * 
 * - 4.6.2: Undeploy the servlet webapp.
 * Verify webapp is not accessible by previous urls.
 * 
 */
public class BasicServletWithCustomPortTest extends AbstractWebTest {
    private Machine machine;
    private GridServiceManager gsm;
    private GridServiceContainer gsc;
    private HtmlPage page;
    private String ip;

    @BeforeMethod
    public void setup() throws IOException {
        super.beforeTest();
        log("waiting for 1 machine");
		assertTrue(admin.getMachines().waitFor(1));
        GridServiceAgent gsa = admin.getGridServiceAgents().waitForAtLeastOne();
        machine = gsa.getMachine();
        ip = machine.getHostAddress();
        gsm = loadGSM(machine);
        gsc = loadGSC(machine);
    }

    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1")
    public void test() throws IOException, InterruptedException {
        
        // 4.6.1 ####################################################
        
        ProcessingUnit pu = gsm.deploy(new ProcessingUnitDeployment(getArchive("servlet.war")).
                numberOfInstances(2).setContextProperty("web.port", "8090"));
        pu.waitFor(pu.getTotalNumberOfInstances());

        webClient.setThrowExceptionOnFailingStatusCode(false);

        page = assertPageExists("http://" + ip + ":8090/servlet");
        assertPageNotExists("http://" + ip + ":8080/servlet");
        
        // 4.6.2 ####################################################

        pu.undeploy();
        assertPageNotExists("http://" + ip + ":8090/servlet");
        assertPageNotExists("http://" + ip + ":8080/servlet");

    }
}

