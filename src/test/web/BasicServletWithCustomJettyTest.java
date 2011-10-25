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
 * 
 * - 4.7.1: Deploy 1 instances o servlet-jetty-config webapp. (plain mode) .
 * Verify webapp is accessible by:
 * http://IP:8095/servlet-jetty-config
 * 
 * Verify webapp is not accessible by:
 * http://IP:8080/servlet-jetty-config
 * 
 * - 4.7.2: Undeploy the servlet webapp.
 * Verify webapp is not accessible by previous urls.
 * 
 */
public class BasicServletWithCustomJettyTest extends AbstractWebTest {
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
        
        // 4.7.1 ####################################################
        
        ProcessingUnit pu = gsm.deploy(new ProcessingUnitDeployment(getArchive("servlet-jetty-config.war")));
        pu.waitFor(pu.getTotalNumberOfInstances());

        webClient.setThrowExceptionOnFailingStatusCode(false);

        // servlet-jetty-config.war, has in file /META-INF/spring/jetty.pu.xml  a custom setting:
        // <prop key="web.port">8095</prop>

        page = assertPageExists("http://" + ip + ":8095/servlet-jetty-config");
        assertPageNotExists("http://" + ip + ":8080/servlet-jetty-config");
        
        // 4.7.2 ####################################################
        
        pu.undeploy();
        assertPageNotExists("http://" + ip + ":8095/servlet-jetty-config");

    }
}

