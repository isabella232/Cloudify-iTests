package test.web;

import static test.utils.AdminUtils.loadGSC;
import static test.utils.AdminUtils.loadGSM;
import static test.utils.DeploymentUtils.getArchive;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitDeployment;
import org.openspaces.admin.pu.events.ProcessingUnitRemovedEventListener;
import org.openspaces.jee.sessions.jetty.SessionData;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.utils.WebUtils;

import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

/***
 * 
 * Setup: 1 machine, 1 gsa, 1 gsm, 2 gsc's
 * Tests:
 * - 4.11.1.1: Deploy servlet-test-embedded (plain mode) 
 * Verify that servlet is accessible by http://IP:8080/session-test
 * Verify that is "New Session" is has "true" value.
 * 
 * - 4.11.1.2 : Add new attribute name and value (new-name:new-value).
 * Verify that the key-value pair was put in the session object in the embedded space 
 * 
 * - 4.11.1.3: Undeploy the webapp.
 * 
 */
public class DistributedHttpSessionEmbeddedTest extends AbstractWebTest {
    private Machine machine;
    private GridServiceManager gsm;
    private GridServiceContainer gsc;
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
        gsc = loadGSC(machine);
        gsc2 = loadGSC(machine);
    }
    
    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1")
    public void test() throws Exception {
  
        // 4.11.1.1 ####################################################
        
        ProcessingUnit pu = gsm.deploy(
                new ProcessingUnitDeployment(getArchive("session-test-embedded.war")).
                numberOfInstances(2).
                clusterSchema("sync_replicated").
                name("session-test"));
        pu.waitFor(pu.getTotalNumberOfInstances());

        webClient.setThrowExceptionOnFailingStatusCode(false);

        page = assertPageExists("http://" + ip + ":8080/session-test");
        assertHtmlContainsText(page, "New Session\ttrue");
        
        // 4.11.1.2 ####################################################
        
        HtmlForm form = page.getFormByName("SessionInfoForm");
        HtmlInput input = form.getInputByName("addAttrName");
        input.setValueAttribute("new-name");
        input = form.getInputByName("addAttrValue");
        input.setValueAttribute("new-value");
        input = form.getInputByName("addAttr");
        page = input.click();
        assertHtmlContainsText(page, "New Session\tfalse");
        
        SessionData sessionData = pu.getSpace().getGigaSpace().read(null, 60000);
        assertNotNull(sessionData);
        ConcurrentHashMap<String, Object> attributeMap = WebUtils.getSessionDataAttributeMap(sessionData);
        Object messageObject = attributeMap.get("new-name");
        String newValue = WebUtils.getMessage(messageObject);

        assertEquals("new-value", newValue);
        
        // 4.11.1.3 ####################################################
        
        final CountDownLatch removedLatch = new CountDownLatch(1);
        admin.getProcessingUnits().getProcessingUnitRemoved().add(new ProcessingUnitRemovedEventListener() {
            public void processingUnitRemoved(ProcessingUnit processingUnit) {
                removedLatch.countDown();
            }
        });
        
        pu.undeploy();
        
        assertTrue(removedLatch.await(60, TimeUnit.SECONDS));
    }
}
