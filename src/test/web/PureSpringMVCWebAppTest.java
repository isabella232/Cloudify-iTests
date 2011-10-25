package test.web;

import static test.utils.AdminUtils.loadGSC;
import static test.utils.AdminUtils.loadGSM;
import static test.utils.DeploymentUtils.getArchive;
import static test.utils.LogUtils.log;
import static test.utils.ProcessingUnitUtils.getProcessingUnitInstanceName;
import static test.utils.ToStringUtils.gscToString;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitDeployment;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.openspaces.admin.pu.events.ProcessingUnitInstanceAddedEventListener;
import org.openspaces.admin.pu.events.ProcessingUnitInstanceRemovedEventListener;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.gargoylesoftware.htmlunit.html.HtmlPage;

/***
 * 
 * Setup: 1 machine, 1 gsa, 1 gsm, 2 gsc's (one is only loaded during the test)
 * Tests:
 * - 4.1.1: Deploy two instances of petclinic webapp. 
 * Verify webapp is accessible by:
 * http://IP:8080/petclinic
 * http://IP:8081/petclinic
 * 
 * Verify webapp is not accessible by:
 * http://IP:8082/petclinic
 * http://IP:8080/petclinic_1
 * http://IP:8080/petclinic_2
 *
 * - 4.1.2: Start additional GSC and relocate petclinic.PU[1] to empty GSC.
 * Verify webapp is accessible by:
 * http://IP:8080/petclinic 
 * http://IP:8081/petclinic
 * 
 * - 4.1.3: Increase (+1) the petclinic webapp instances.
 * Verify webapp is accessible by:
 * http://IP:8082/petclinic 

 * Verify webapp is not accessible by:
 * http://IP:8083/petclinic
 * 
 * - 4.1.4: Decrease (-1) the petclinic webapp instances (third instance).
 * Verify webapp is not accessible by:
 * http://IP:8082/petclinic
 * 
 * - 4.1.5: Kill the GSC that contains at least one petclinic.PU instance.
 * Verify webapp is accessible by:
 * http://IP:8080/petclinic 
 * http://IP:8081/petclinic
 * 
 * - 4.1.6: Undeploy the petclinic webapp.
 * Verify webapp is not accessible by previous urls.
 * 
 */
public class PureSpringMVCWebAppTest extends AbstractWebTest {
    private Machine machine;
    private GridServiceManager gsm;
    private GridServiceContainer gsc0;
    private GridServiceContainer gsc1;
    private HtmlPage page;
    private String ip;

    @Override
    @BeforeMethod
    public void beforeTest() {
        super.beforeTest();
        GridServiceAgent gsa = admin.getGridServiceAgents().waitForAtLeastOne();
        machine = gsa.getMachine();
        ip = machine.getHostAddress();
        gsm = loadGSM(machine);
        gsc0 = loadGSC(machine);
    }

    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1")
    public void test() throws IOException, InterruptedException {
        
        // 4.1.1 ####################################################

        ProcessingUnit pu = gsm.deploy(new ProcessingUnitDeployment(getArchive("petclinic.war")).numberOfInstances(2));
        pu.waitFor(pu.getTotalNumberOfInstances());
        webClient.setThrowExceptionOnFailingStatusCode(false);

        page = assertPageExists("http://"+ ip +":8080/petclinic");
        assertNotNull(((HtmlPage)page.getAnchorByHref("/petclinic/findOwners.do").click()).
                getAnchorByHref("/petclinic/addOwner.do").click());

        page = assertPageExists("http://"+ ip +":8081/petclinic");
        assertNotNull(((HtmlPage)page.getAnchorByHref("/petclinic/findOwners.do").click()).
                getAnchorByHref("/petclinic/addOwner.do").click());

        assertPageNotExists("http://"+ ip +":8082/petclinic");
        assertPageNotFound("http://"+ ip +":8080/petclinic_1");
        assertPageNotFound("http://"+ ip +":8080/petclinic_2");

        // 4.1.2 ######################################################
        
        gsc1 = loadGSC(machine);

        final CountDownLatch puAddedLatch = new CountDownLatch(1);
        gsc1.getProcessingUnitInstanceAdded().add(new ProcessingUnitInstanceAddedEventListener() {
            public void processingUnitInstanceAdded(ProcessingUnitInstance processingUnitInstance) {
                log("event: " + getProcessingUnitInstanceName(processingUnitInstance)
                        + " instantiated on " + gscToString(gsc1));
                puAddedLatch.countDown();
            }
        });

        gsc0.getProcessingUnitInstances()[0].relocate(gsc1);
        log("waiting for instance to be relocated to " + gscToString(gsc1));
        assertTrue(puAddedLatch.await(60, TimeUnit.SECONDS));
        assertPageExists("http://"+ ip +":8080/petclinic");
        assertNotNull(((HtmlPage)page.getAnchorByHref("/petclinic/findOwners.do").click()).
                getAnchorByHref("/petclinic/addOwner.do").click());
        assertPageExists("http://"+ ip +":8081/petclinic");
        assertNotNull(((HtmlPage)page.getAnchorByHref("/petclinic/findOwners.do").click()).
                getAnchorByHref("/petclinic/addOwner.do").click());

        // 4.1.3 ######################################################
        
        final ProcessingUnitInstance[] puInstanceIncrement = new ProcessingUnitInstance[1];
        final CountDownLatch puInstanceAddedLatch = new CountDownLatch(1);
        pu.getProcessingUnitInstanceAdded().add(new ProcessingUnitInstanceAddedEventListener() {
            public void processingUnitInstanceAdded(ProcessingUnitInstance processingUnitInstance) {
                log("event: " + getProcessingUnitInstanceName(processingUnitInstance) + " instantiated");
                puInstanceIncrement[0] = processingUnitInstance;
                puInstanceAddedLatch.countDown();
            }
        }, false);

        pu.incrementInstance();
        assertTrue(puInstanceAddedLatch.await(60, TimeUnit.SECONDS));
        //GS-8275
        // for some reason pu.getTotalNumberOfInstances() returns 2 at this point.
        // so we write our expectation explicitly (i.e: 3)
        assertTrue(pu.waitFor(3));
        
        assertPageExists("http://"+ ip +":8080/petclinic");
        assertPageExists("http://"+ ip +":8081/petclinic");
        assertPageExists("http://"+ ip +":8082/petclinic");
        assertNotNull(((HtmlPage)page.getAnchorByHref("/petclinic/findOwners.do").click()).
                getAnchorByHref("/petclinic/addOwner.do").click());
        assertPageNotExists("http://"+ ip +":8083/petclinic");
        
        // 4.1.4 ######################################################
        
        final CountDownLatch puInstanceRemovedLatch = new CountDownLatch(1);
        pu.getProcessingUnitInstanceRemoved().add(new ProcessingUnitInstanceRemovedEventListener() {
            public void processingUnitInstanceRemoved(ProcessingUnitInstance processingUnitInstance) {
                 log("event: " + getProcessingUnitInstanceName(processingUnitInstance) + " removed");
                puInstanceIncrement[0] = processingUnitInstance;
                puInstanceRemovedLatch.countDown();
            }
        });

        puInstanceIncrement[0].decrement();
        assertTrue(puInstanceRemovedLatch.await(60, TimeUnit.SECONDS));
        assertPageExists("http://"+ ip +":8080/petclinic");
        assertPageExists("http://"+ ip +":8081/petclinic");
        assertPageNotExists("http://"+ ip +":8082/petclinic");
        
        // 4.1.5 ######################################################
        assertEquals(1, gsc0.getProcessingUnitInstances().length);

        final CountDownLatch addedLatch = new CountDownLatch(1);
        gsc1.getProcessingUnitInstanceAdded().add(new ProcessingUnitInstanceAddedEventListener() {
            public void processingUnitInstanceAdded(ProcessingUnitInstance processingUnitInstance) {
                addedLatch.countDown();
            }
        }, false);

        
		log("killing GSC #0, containing: " + getProcessingUnitInstanceName(gsc0.getProcessingUnitInstances()));
		gsc0.kill();

		log("waiting for processing units to be instantiated on GSC #1");
		assertTrue(addedLatch.await(60, TimeUnit.SECONDS));
        assertPageExists("http://"+ ip +":8080/petclinic");
        assertNotNull(((HtmlPage)page.getAnchorByHref("/petclinic/findOwners.do").click()).
                getAnchorByHref("/petclinic/addOwner.do").click());
        assertPageExists("http://"+ ip +":8081/petclinic");
        assertNotNull(((HtmlPage)page.getAnchorByHref("/petclinic/findOwners.do").click()).
                getAnchorByHref("/petclinic/addOwner.do").click());

        // 4.1.6 ######################################################
        
        assertEquals(2, pu.getTotalNumberOfInstances());
        
        final CountDownLatch removedLatch = new CountDownLatch(pu.getTotalNumberOfInstances());
        gsc1.getProcessingUnitInstanceRemoved().add(new ProcessingUnitInstanceRemovedEventListener() {
            public void processingUnitInstanceRemoved(
                    ProcessingUnitInstance processingUnitInstance) {
                removedLatch.countDown();
            }
        });
        
        pu.undeploy();
        assertTrue(removedLatch.await(60, TimeUnit.SECONDS));

        assertPageNotExists("http://"+ ip +":8080/petclinic");
        assertPageNotExists("http://"+ ip +":8081/petclinic");
        
    }

}
