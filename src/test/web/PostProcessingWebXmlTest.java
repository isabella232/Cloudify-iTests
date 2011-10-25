package test.web;

import static test.utils.AdminUtils.loadGSC;
import static test.utils.AdminUtils.loadGSM;
import static test.utils.DeploymentUtils.getArchive;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitDeployment;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.utils.ScriptUtils;

import com.gargoylesoftware.htmlunit.html.HtmlPage;

/***
 * 
 * Setup: 1 machine, 1 gsa, 1 gsm, 1 gsc
 * Tests:
 * 
 * - 4.8.1: Deploy 1 instances o servlet-space-postprocessing webapp. (plain mode).
 * Verify webapp is accessible by:
 * http://IP:8080/servlet-space-postprocessing/
 * 
 * Verify that \work\processing-units\servlet-space-postprocessing_1\WEB-INF\web.xml contains 
 * modified value of display name: Hello World, 0
 * 
 * - 4.8.2: Undeploy the servlet webapp.
 * Verify webapp is not accessible by previous url.
 * 
 */
public class PostProcessingWebXmlTest extends AbstractWebTest {
    private Machine              machine;
    private GridServiceManager   gsm;
    private GridServiceContainer gsc;
    private HtmlPage             page;
    private String               ip;

    @BeforeMethod
    public void setup() throws IOException {
        super.beforeTest();
        GridServiceAgent gsa = admin.getGridServiceAgents().waitForAtLeastOne();
        machine = gsa.getMachine();
        ip = machine.getHostAddress();
        gsm = loadGSM(machine);
        gsc = loadGSC(machine);
    }

    // fix the working directory path
    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1")
    public void test() throws IOException, InterruptedException {
        
        // 4.7.1 ####################################################
        
        ProcessingUnit pu = gsm.deploy(new ProcessingUnitDeployment(getArchive("servlet-space-postprocessing.war")));
        pu.waitFor(pu.getTotalNumberOfInstances());

        webClient.setThrowExceptionOnFailingStatusCode(false);

        page = assertPageExists("http://" + ip + ":8080/servlet-space-postprocessing");

        //path relative to gigaspaces folder
        assertFileContainsText("/work/processing-units/servlet-space-postprocessing_1/WEB-INF/web.xml",
        "Hello World, 0");

        // 4.7.2 ####################################################
        
        pu.undeploy();
        assertPageNotExists("http://" + ip + ":8080/servlet-space-postprocessing");

    }

    private void assertFileContainsText(String fileName, String text)
            throws FileNotFoundException, IOException {
        File f = new File(ScriptUtils.getBuildPath() + fileName);

        BufferedReader input = new BufferedReader(new FileReader(f));
        try {
            String line = null;
            boolean found = false;
            while ((line = input.readLine()) != null) {
                if (line.contains(text)) {
                    found = true;
                    break;
                }
            }
            assertTrue(found);
        }
        finally {
            input.close();
        }

    }
}
