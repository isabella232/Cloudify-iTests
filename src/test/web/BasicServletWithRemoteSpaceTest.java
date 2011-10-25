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
import org.openspaces.admin.space.SpaceDeployment;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.data.Person;

import com.gargoylesoftware.htmlunit.html.HtmlPage;

/***
 * 
 * Setup: 1 machine, 1 gsa, 1 gsm, 1 gsc
 * Tests:
 * 
 * - 4.5.1.1: Deploy a space named servlet partitioned 2,0
 * Verify that there are two spaces:
 * servlet_container1:servlet
 * servlet_container2:servlet
 * 
 * - 4.5.1.2: Deploy 1 instance of servlet webapp. (plain mode)
 * Verify webapp is accessible by:
 * http://IP:8080/servlet-space-remote
 * 
 * Verify that jsp page displays value for <space> & <gigaSpace> beans.
 * 
 * Verify the app's space can be read.
 * 
 * - 4.5.1.3: Undeploy the servlet webapp.
 * Verify webapp is not accessible by previous urls.
 * 
 */
public class BasicServletWithRemoteSpaceTest extends AbstractWebTest {
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

        // 4.5.1.1 ####################################################
        
        ProcessingUnit space = gsm.deploy(new SpaceDeployment("servlet").numberOfInstances(2).
                clusterSchema("partitioned").setContextProperty("dataGridName", "servlet"));
        space.waitFor(space.getTotalNumberOfInstances());
        
        assertEquals("servlet_container" + space.getInstances()[0].getInstanceId() + ":servlet",
                     space.getInstances()[0].getEmbeddedSpaceDetails().getLongDescription());
        assertEquals("servlet_container" + space.getInstances()[1].getInstanceId() + ":servlet",
                     space.getInstances()[1].getEmbeddedSpaceDetails().getLongDescription());

        // 4.5.1.2 ####################################################
        
        ProcessingUnit pu = gsm.deploy(new ProcessingUnitDeployment(getArchive("servlet-space-remote.war")).
                numberOfInstances(1));
        pu.waitFor(pu.getTotalNumberOfInstances());
        
        page = assertPageExists("http://" + ip + ":8080/servlet-space-remote");
      
        assertHtmlContainsText(page, new String[] { 
                "bean space = servlet_container1:servlet" ,
                "bean space = servlet_container2:servlet" 
        });
        
        assertHtmlContainsText(page, new String[] { 
                "bean gigaSpace = servlet_container1:servlet", 
                "bean gigaSpace = servlet_container2:servlet"
        });
       
        // 4.5.1.2(2) ####################################################
       
        // servlet.war writes Person object to space
        Person p = space.getSpace().getGigaSpace().read(null);
        assertEquals("John Doe", p.getName());
        assertHtmlContainsText(page, "John Doe");
        
        // 4.5.1.3 ####################################################
        
        pu.undeploy();
        assertPageNotExists("http://" + ip + ":8080/servlet-space-remote");
       
    }
}


