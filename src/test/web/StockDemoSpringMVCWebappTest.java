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
 * Setup: 1 machine, 1 gsa, 1 gsm, 2 gsc
 * Tests:
 * - 4.3.2.1: Deploy processorPU.jar, feederPU.jar, StockDemo.war 
 * 
 * Verify webapp is accessible by:
 * http://IP:8080/stockdemo
 *
 * - 4.3.2.4: Undeploy the stockdemo webapp.
 * Verify webapp is not accessible by previous url.
 * 
 */ 
public class StockDemoSpringMVCWebappTest extends AbstractWebTest {
    private Machine machine;
    private GridServiceManager gsm;
    private GridServiceContainer gsc1;
    private GridServiceContainer gsc2;
    private HtmlPage page;
    private String ip;

    @BeforeMethod
    public void setup() throws IOException {
        super.beforeTest();
        GridServiceAgent gsa = admin.getGridServiceAgents().waitForAtLeastOne();
        machine = gsa.getMachine();
        ip = machine.getHostAddress();
        gsm = loadGSM(machine);
        gsc1 = loadGSC(machine);
        gsc2 = loadGSC(machine);
    }

    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1")
    public void test() throws IOException, InterruptedException {
        
        // 4.3.2.1 ####################################################
        
        ProcessingUnit processor = gsm.deploy(new ProcessingUnitDeployment(getArchive("processorPU.jar")));
        processor.waitFor(processor.getTotalNumberOfInstances());

        // 4.3.2.2 ####################################################
        
        ProcessingUnit feeder = gsm.deploy(new ProcessingUnitDeployment(getArchive("feederPU.jar")));
        feeder.waitFor(feeder.getTotalNumberOfInstances());

        // 4.3.2.3 ####################################################
        
        ProcessingUnit stockdemo = gsm.deploy(new ProcessingUnitDeployment(getArchive("StockDemo.war")));
        stockdemo.waitFor(stockdemo.getTotalNumberOfInstances());

        webClient.setThrowExceptionOnFailingStatusCode(false);

        page = assertPageExists("http://" + ip + ":8080/StockDemo");
        assertNotNull(page.getElementById("grid-example"));
        
        // 4.3.2.4 ####################################################
        
        processor.undeploy();
        feeder.undeploy();
        stockdemo.undeploy();

        assertPageNotExists("http://" + ip + ":8080/StockDemo");
    }
}


