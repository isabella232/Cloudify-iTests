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
 * - 4.3.1: Deploy two instances of petclinic webapp (in shared mode). 
 * Verify webapp is accessible by:
 * http://IP:8080/petclinic_1
 * http://IP:8080/petclinic_2
 * 
 * Verify the following spaces exist:
 * petclinic_container1:petclinic
 * petclinic_container2:petclinic
 *
 * - 4.3.2: Undeploy the petclinic webapp.
 * Verify webapp is not accessible by previous urls and no spaces exist
 * 
 * - 4.3.3: Deploy two instances of petclinic webapp (in plain mode). 
 * Verify webapp is accessible by:
 * http://IP:8080/petclinic
 * http://IP:8081/petclinic
 * 
 * Verify the following spaces exist:
 * petclinic_container1:petclinic
 * petclinic_container2:petclinic
 *
 * - 4.3.4: Undeploy the petclinic webapp.
 * Verify webapp is not accessible by previous urls and no spaces exist
 * 
 */ 
public class PureSpringMVCWithEmbeddedSpaceWebAppTest extends AbstractWebTest {
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

        // 4.3.1 ####################################################
        
        ProcessingUnit pu = gsm.deploy(new ProcessingUnitDeployment(getArchive("petclinic-space.war")).
                numberOfInstances(2).setContextProperty("jetty.instance", "shared"));
        pu.waitFor(pu.getTotalNumberOfInstances());

        webClient.setThrowExceptionOnFailingStatusCode(false);

        page = assertPageExists("http://" + ip + ":8080/petclinic-space_1");
        assertNotNull(((HtmlPage) page.getAnchorByHref("/petclinic-space_1/findOwners.do").click()).
                getAnchorByHref("/petclinic-space_1/addOwner.do").click());

        page = assertPageExists("http://" + ip + ":8080/petclinic-space_2");
        assertNotNull(((HtmlPage) page.getAnchorByHref("/petclinic-space_2/findOwners.do").click()).
                getAnchorByHref("/petclinic-space_2/addOwner.do").click());

        assertEquals("petclinic_container" + pu.getInstances()[0].getInstanceId() + ":petclinic",
                pu.getInstances()[0].getEmbeddedSpaceDetails().getLongDescription());
        assertEquals("petclinic_container" + pu.getInstances()[1].getInstanceId() + ":petclinic",
                pu.getInstances()[1].getEmbeddedSpaceDetails().getLongDescription());

        // 4.3.2 ####################################################
        
        pu.undeploy();
        assertPageNotExists("http://" + ip + ":8080/petclinic-space_1");
        assertPageNotExists("http://" + ip + ":8080/petclinic-space_2");

        for (int i = 0; i < pu.getInstances().length; i++) {
            assertEquals("checking number of space instances", 0, pu.getInstances()[i].getSpaceInstances().length);
        }
        
        // 4.3.3 ####################################################
        
        pu = gsm.deploy(new ProcessingUnitDeployment(getArchive("petclinic-space.war")).numberOfInstances(2));
        pu.waitFor(pu.getTotalNumberOfInstances());

        page = assertPageExists("http://" + ip + ":8080/petclinic-space");
        assertNotNull(((HtmlPage) page.getAnchorByHref("/petclinic-space/findOwners.do").click()).
                getAnchorByHref("/petclinic-space/addOwner.do").click());

        page = assertPageExists("http://" + ip + ":8081/petclinic-space");
        assertNotNull(((HtmlPage) page.getAnchorByHref("/petclinic-space/findOwners.do").click()).
                getAnchorByHref("/petclinic-space/addOwner.do").click());

        assertEquals("petclinic_container" + pu.getInstances()[0].getInstanceId() + ":petclinic",
                pu.getInstances()[0].getEmbeddedSpaceDetails().getLongDescription());
        assertEquals("petclinic_container" + pu.getInstances()[1].getInstanceId() + ":petclinic",
                pu.getInstances()[1].getEmbeddedSpaceDetails().getLongDescription());

        
        // 4.3.4 ####################################################
        
        pu.undeploy();
        assertPageNotExists("http://" + ip + ":8080/petclinic-space");
        assertPageNotExists("http://" + ip + ":8081/petclinic-space");

        for (int i = 0; i < pu.getInstances().length; i++) {
            assertEquals("checking number of space instances", 0, pu.getInstances()[i].getSpaceInstances().length);
        }
        
    }
}

