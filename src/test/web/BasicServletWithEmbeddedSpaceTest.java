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

import test.data.Person;

import com.gargoylesoftware.htmlunit.html.HtmlPage;

/***
 * 
 * Setup: 1 machine, 1 gsa, 1 gsm, 1 gsc
 * Tests:
 * - 4.5.1: Deploy two instances of servlet webapp. (shared mode)
 * Verify webapp is accessible by:
 * http://IP:8080/servlet-space_1
 * http://IP:8080/servlet-space_2
 * 
 * Verify that there are two spaces:
 * servlet_container1:servlet
 * servlet_container2:servlet
 * 
 * Verify that jsp page displays value for <space> & <gigaSpace> beans.
 * 
 * Verify the app's space can be read.
 * 
 * - 4.5.2: Undeploy the servlet webapp.
 * Verify webapp is not accessible by previous urls.
 * 
 * - 4.5.3: Deploy two instances of servlet webapp. (plain mode)
 * Verify webapp is accessible by:
 * http://IP:8080/servlet-space
 * http://IP:8081/servlet-space
 * 
 * Verify that there are two spaces:
 * servlet_container1:servlet
 * servlet_container2:servlet
 * 
 * Verify that jsp page displays value for <space> & <gigaSpace> beans.
 * 
 * Verify the app's space can be read.
 * 
 * - 4.5.4: Undeploy the petclinic webapp.
 * Verify webapp is not accessible by previous urls.
 * 
 */
public class BasicServletWithEmbeddedSpaceTest extends AbstractWebTest {
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

        // 4.5.1 ####################################################
        
        ProcessingUnit pu = gsm.deploy(new ProcessingUnitDeployment(getArchive("servlet-space.war")).
                numberOfInstances(2).setContextProperty("jetty.instance", "shared"));
        pu.waitFor(pu.getTotalNumberOfInstances());

        webClient.setThrowExceptionOnFailingStatusCode(false);

        page = assertPageExists("http://" + ip + ":8080/servlet-space_1");
     
        assertHtmlContainsText(page,"bean space = servlet_container1:servlet");
        assertHtmlContainsText(page,"bean gigaSpace = servlet_container1:servlet");
        
        page = assertPageExists("http://" + ip + ":8080/servlet-space_2");
        assertHtmlContainsText(page,"bean space = servlet_container2:servlet");
        assertHtmlContainsText(page,"bean gigaSpace = servlet_container2:servlet");
     
        assertEquals("servlet_container" + pu.getInstances()[0].getInstanceId() + ":servlet",
                pu.getInstances()[0].getEmbeddedSpaceDetails().getLongDescription());
        assertEquals("servlet_container" + pu.getInstances()[1].getInstanceId() + ":servlet",
                pu.getInstances()[1].getEmbeddedSpaceDetails().getLongDescription());

        
        // 4.5.1(2) ####################################################
        
        // servlet.war writes Person object to space
        Person p = pu.getSpace().getGigaSpace().read(null);
        assertEquals("John Doe", p.getName());
        assertHtmlContainsText(page, "John Doe");
        
        // 4.5.2 ####################################################
        
        pu.undeploy();
        assertPageNotExists("http://" + ip + ":8080/servlet-space_1");
        assertPageNotExists("http://" + ip + ":8080/servlet-space_2");

        // 4.5.3 ####################################################
        
        pu = gsm.deploy(new ProcessingUnitDeployment(getArchive("servlet-space.war")).numberOfInstances(2));
        pu.waitFor(pu.getTotalNumberOfInstances());

        page = assertPageExists("http://" + ip + ":8080/servlet-space");
        assertHtmlContainsText(page,"bean space = servlet_container1:servlet");
        assertHtmlContainsText(page,"bean gigaSpace = servlet_container1:servlet");
      
        page = assertPageExists("http://" + ip + ":8081/servlet-space");
        assertHtmlContainsText(page,"bean space = servlet_container2:servlet");
        assertHtmlContainsText(page,"bean gigaSpace = servlet_container2:servlet");
      
        assertEquals("servlet_container" + pu.getInstances()[0].getInstanceId() + ":servlet",
                pu.getInstances()[0].getEmbeddedSpaceDetails().getLongDescription());
        assertEquals("servlet_container" + pu.getInstances()[1].getInstanceId() + ":servlet",
                pu.getInstances()[1].getEmbeddedSpaceDetails().getLongDescription());

        // 4.5.3(2) ####################################################
        
        // servlet.war writes Person object to space
        Person p2 = pu.getSpace().getGigaSpace().read(null);
        assertEquals("John Doe", p2.getName());
        assertHtmlContainsText(page, "John Doe");
        
        // 4.5.4 ####################################################
        
        pu.undeploy();
        assertPageNotExists("http://" + ip + ":8080/servlet-space");
        assertPageNotExists("http://" + ip + ":8081/servlet-space");
        
    }
}

