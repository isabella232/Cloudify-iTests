package test.web;

import static test.utils.AdminUtils.loadGSC;
import static test.utils.AdminUtils.loadGSM;
import static test.utils.DeploymentUtils.getArchive;

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
 * - 4.4.1: Deploy two instances of servlet webapp. (shared mode)
 * Verify webapp is accessible by:
 * http://IP:8080/servlet_1
 * http://IP:8080/servlet_2
 * 
 * - 4.4.2: Undeploy the servlet webapp.
 * Verify webapp is not accessible by previous urls.
 * 
 * - 4.4.3: Deploy two instances of servlet webapp. (plain mode)
 * Verify webapp is accessible by:
 * http://IP:8080/servlet
 * http://IP:8081/servlet
 * 
 * - 4.4.4: Undeploy the servlet webapp.
 * Verify webapp is not accessible by previous urls.
 * 
 */
public class BasicServletTest extends AbstractWebTest {
    private Machine machine;
    private GridServiceManager gsm;
    private GridServiceContainer gsc;
    private HtmlPage page;
    private String ip;

    @BeforeMethod
    public void setup() throws IOException {
        super.beforeTest();
        GridServiceAgent gsa = admin.getGridServiceAgents().waitForAtLeastOne();
        machine = gsa.getMachine();
        ip = machine.getHostAddress();
        gsm = loadGSM(machine);
        gsc = loadGSC(machine);
    }

    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1")
    public void test() throws IOException, InterruptedException {

        // 4.4.1 ####################################################
        
        ProcessingUnit pu = gsm.deploy(new ProcessingUnitDeployment(getArchive("servlet.war")).
                numberOfInstances(2).setContextProperty("jetty.instance", "shared"));
        pu.waitFor(pu.getTotalNumberOfInstances());

        webClient.setThrowExceptionOnFailingStatusCode(false);

        page = assertPageExists("http://" + ip + ":8080/servlet_1");
        assertHtmlContainsText(page, "Hello, World.");
        page = assertPageExists("http://" + ip + ":8080/servlet_2");
        assertHtmlContainsText(page, "Hello, World.");

        // 4.4.2 ####################################################
        
        pu.undeploy();
        assertPageNotExists("http://" + ip + ":8080/servlet_1");
        assertPageNotExists("http://" + ip + ":8080/servlet_2");

        // 4.4.3 ####################################################
        
        pu = gsm.deploy(new ProcessingUnitDeployment(getArchive("servlet.war")).numberOfInstances(2));
        pu.waitFor(pu.getTotalNumberOfInstances());

        page = assertPageExists("http://" + ip + ":8080/servlet");
        assertHtmlContainsText(page, "Hello, World.");
        page = assertPageExists("http://" + ip + ":8081/servlet");
        assertHtmlContainsText(page, "Hello, World.");
    
        // 4.4.4 ####################################################
        
        pu.undeploy();
        assertPageNotExists("http://" + ip + ":8080/servlet");
        assertPageNotExists("http://" + ip + ":8081/servlet");


    }
}

