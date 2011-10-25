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

import com.gargoylesoftware.htmlunit.html.HtmlPage;

/***
 * 
 * Setup: 1 machine, 1 gsa, 1 gsm, 1 gsc
 * Tests:
 * - 4.3.1.1: Deploy a space named petclinic partitioned 2,0 . 
 *
 * - 4.3.1.2: Deploy one instance of petclinic webapp (plain mode) .
 * Verify webapp is accessible by:
 * http://IP:8080/petclinic-space-remote
 * 
 * - 4.3.1.3: Deploy two instances of petclinic webapp (in plain mode). 
 * Verify webapp is not accessible by previous urls.
 * 
 */ 
public class PureSpringMVCWithRemoteSpaceWebAppTest extends AbstractWebTest {
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

        // 4.3.1.1 ####################################################

        ProcessingUnit space = gsm.deploy(new SpaceDeployment("petclinic").numberOfInstances(2).
                clusterSchema("partitioned"));
        
        space.waitFor(space.getTotalNumberOfInstances());
        
        // 4.3.1.2 ####################################################      
        
        ProcessingUnit pu = gsm.deploy(new ProcessingUnitDeployment(getArchive("petclinic-space-remote.war")).
                numberOfInstances(2));
        pu.waitFor(pu.getTotalNumberOfInstances());

        page = assertPageExists("http://" + ip + ":8080/petclinic-space-remote");
        assertNotNull(((HtmlPage) page.getAnchorByHref("/petclinic-space-remote/findOwners.do").click()).
                getAnchorByHref("/petclinic-space-remote/addOwner.do").click());

        page = assertPageExists("http://" + ip + ":8081/petclinic-space-remote");
        assertNotNull(((HtmlPage) page.getAnchorByHref("/petclinic-space-remote/findOwners.do").click()).
                getAnchorByHref("/petclinic-space-remote/addOwner.do").click());

        // 4.3.1.3 ####################################################
        
        pu.undeploy();
        assertPageNotExists("http://" + ip + ":8080/petclinic-space-remote");
        assertPageNotExists("http://" + ip + ":8081/petclinic-space-remote");

    }
}


