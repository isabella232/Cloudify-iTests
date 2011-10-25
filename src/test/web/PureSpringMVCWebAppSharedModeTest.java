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
 * - 4.2.1: Deploy two instances of petclinic webapp (in shared mode). 
 * Verify webapp is accessible by:
 * http://IP:8080/petclinic_1
 * http://IP:8080/petclinic_2
 * 
 * Verify webapp is not accessible by:
 * http://IP:8082/petclinic_3
 * http://IP:8080/petclinic
 * http://IP:8081/petclinic
 *
 * - 4.2.2: Start additional GSC and relocate petclinic.PU[1] to empty GSC.
 * Verify webapp is accessible by:
 * http://IP:8080/petclinic_1
 * http://IP:8080/petclinic_2
 * 
 * - 4.2.3: Increase (+1) the petclinic webapp instances.
 * Verify webapp is accessible by:
 * http://IP:8080/petclinic_3 

 * Verify webapp is not accessible by:
 * http://IP:8080/petclinic_4
 * 
 * - 4.2.4: Decrease (-1) the petclinic webapp instances (third instance).
 * Verify webapp is not accessible by:
 * http://IP:8080/petclinic_3
 * 
 * - 4.2.5: Kill the GSC that contains at least one petclinic.PU instance.
 * Verify webapp is accessible by:
 * http://IP:8080/petclinic_1
 * http://IP:8080/petclinic_2
 * 
 * - 4.2.6: Undeploy the petclinic webapp.
 * Verify webapp is not accessible by previous urls.
 * 
 */ 
public class PureSpringMVCWebAppSharedModeTest extends AbstractWebTest {
    private Machine machine;
    private GridServiceManager gsm;
    private GridServiceContainer gsc0;
    private GridServiceContainer gsc1;
    private HtmlPage page;
    private String ip;

    @BeforeMethod
    public void setup() throws IOException {
        super.beforeTest();
        GridServiceAgent gsa = admin.getGridServiceAgents().waitForAtLeastOne();
        machine = gsa.getMachine();
        ip = machine.getHostAddress();
        gsm = loadGSM(machine);
        gsc0 = loadGSC(machine);
    }

    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1")
    public void test() throws IOException, InterruptedException {
        
        // 4.2.1 ####################################################
        
        ProcessingUnit pu = gsm.deploy(new ProcessingUnitDeployment(getArchive("petclinic.war")).
                numberOfInstances(2).setContextProperty("jetty.instance", "shared"));
        pu.waitFor(pu.getTotalNumberOfInstances());
        
        webClient.setThrowExceptionOnFailingStatusCode(false);

        page = assertPageExists("http://"+ ip +":8080/petclinic_1");
        assertNotNull(((HtmlPage)page.getAnchorByHref("/petclinic_1/findOwners.do").click()).
                getAnchorByHref("/petclinic_1/addOwner.do").click());

        page = assertPageExists("http://"+ ip +":8080/petclinic_2");
        assertNotNull(((HtmlPage)page.getAnchorByHref("/petclinic_2/findOwners.do").click()).
                getAnchorByHref("/petclinic_2/addOwner.do").click());

        assertPageNotFound("http://"+ ip +":8080/petclinic_3");
        assertPageNotFound("http://"+ ip +":8080/petclinic");
        assertPageNotExists("http://"+ip +":8081/petclinic");

        
        // 4.2.2 ####################################################
        
        gsc1 = loadGSC(machine);

        final CountDownLatch puAddedLatch = new CountDownLatch(1);
        gsc1.getProcessingUnitInstanceAdded().add(new ProcessingUnitInstanceAddedEventListener() {
            public void processingUnitInstanceAdded(ProcessingUnitInstance processingUnitInstance) {
                log("event: " + getProcessingUnitInstanceName(processingUnitInstance)
                        + " instantiated on " + gscToString(gsc1));
                puAddedLatch.countDown();
            }
        }, false);

        int movedInstanceId = gsc0.getProcessingUnitInstances()[0].getInstanceId();
        int unmovedInstanceId = movedInstanceId == 1 ? 2 : 1;
        gsc0.getProcessingUnitInstances()[0].relocate(gsc1);
        log("waiting for instance to be relocated to " + gscToString(gsc1));
        assertTrue(puAddedLatch.await(60, TimeUnit.SECONDS));

        page = assertPageExists("http://"+ip +":8080/petclinic_" + unmovedInstanceId);
        assertNotNull(((HtmlPage)page.getAnchorByHref("/petclinic_"+ unmovedInstanceId + "/findOwners.do").click()).
                getAnchorByHref("/petclinic_" + unmovedInstanceId + "/addOwner.do").click());
        page = assertPageExists("http://"+ip +":8081/petclinic_" + movedInstanceId);
        assertNotNull(((HtmlPage)page.getAnchorByHref("/petclinic_" + movedInstanceId + "/findOwners.do").click()).
                getAnchorByHref("/petclinic_" + movedInstanceId + "/addOwner.do").click());

        // 4.2.3 ####################################################
        
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
        
        // for some reason pu.getTotalNumberOfInstances() returns 2 at this point.
        // so we write our expectation explicitly (i.e: 3)
        pu.waitFor(3);
        
        assertPageExists("http://"+ip +":8080/petclinic_" + unmovedInstanceId);
        assertPageExists("http://"+ip +":8081/petclinic_" + movedInstanceId);
        
        HtmlPage pageOn8080 = getPage("http://"+ip +":8080/petclinic_3");
        HtmlPage pageOn8081 = null;
        if (pageOn8080 == null) {
            pageOn8081 = getPage("http://"+ip +":8081/petclinic_3");
        }
        assertTrue(pageOn8080 != null || pageOn8081 != null);
        
        page = pageOn8080 != null ? pageOn8080 : pageOn8081;
        assertNotNull(((HtmlPage)page.getAnchorByHref("/petclinic_3/findOwners.do").click()).
                getAnchorByHref("/petclinic_3/addOwner.do").click());
        assertPageNotFound("http://"+ip +":8080/petclinic_4");
        assertPageNotFound("http://"+ip +":8081/petclinic_4");

        // 4.2.4 ####################################################
        
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
        
        assertPageExists("http://"+ip +":8080/petclinic_" + unmovedInstanceId);
        assertPageExists("http://"+ip +":8081/petclinic_" + movedInstanceId);
        assertPageNotFound("http://"+ip +":8080/petclinic_3");
        assertPageNotFound("http://"+ip +":8081/petclinic_3");
        
        // 4.2.5 ####################################################
        
        assertEquals(1, gsc0.getProcessingUnitInstances().length);

       final CountDownLatch addedLatch = new CountDownLatch(1);
		gsc1.getProcessingUnitInstanceAdded().add(new ProcessingUnitInstanceAddedEventListener() {
			public void processingUnitInstanceAdded(
					ProcessingUnitInstance processingUnitInstance) {
				addedLatch.countDown();
			}
		}, false);

		
		log("killing GSC #0, containing: " + getProcessingUnitInstanceName(gsc0.getProcessingUnitInstances()));
		gsc0.kill();

		log("waiting for processing units to be instantiated on GSC #1");
		assertTrue(addedLatch.await(60, TimeUnit.SECONDS));
        page = assertPageExists("http://"+ip +":8081/petclinic_1");
        assertNotNull(((HtmlPage)page.getAnchorByHref("/petclinic_1/findOwners.do").click()).
                getAnchorByHref("/petclinic_1/addOwner.do").click());
        page = assertPageExists("http://"+ip +":8081/petclinic_2");
        assertNotNull(((HtmlPage)page.getAnchorByHref("/petclinic_2/findOwners.do").click()).
                getAnchorByHref("/petclinic_2/addOwner.do").click());


        // 4.2.6 ####################################################
        
        assertEquals(2, gsc1.getProcessingUnitInstances().length);
        
        final CountDownLatch removedLatch = new CountDownLatch(pu.getTotalNumberOfInstances());
        gsc1.getProcessingUnitInstanceRemoved().add(new ProcessingUnitInstanceRemovedEventListener() {
            public void processingUnitInstanceRemoved(
                    ProcessingUnitInstance processingUnitInstance) {
                removedLatch.countDown();
            }
        });
        
        pu.undeploy();
        assertTrue(removedLatch.await(60, TimeUnit.SECONDS));

        assertPageNotExists("http://"+ ip +":8080/petclinic_1");
        assertPageNotExists("http://"+ ip +":8081/petclinic_1");
        assertPageNotExists("http://"+ ip +":8080/petclinic_2");
        assertPageNotExists("http://"+ ip +":8081/petclinic_2");
        
    }
}

