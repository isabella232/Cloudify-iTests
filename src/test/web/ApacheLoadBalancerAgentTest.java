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
import org.openspaces.admin.gsa.GridServiceOptions;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitDeployment;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.openspaces.admin.pu.events.ProcessingUnitInstanceAddedEventListener;
import org.openspaces.admin.pu.events.ProcessingUnitRemovedEventListener;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.gargoylesoftware.htmlunit.html.HtmlPage;

/***
 *
 * Setup: 2 machines, 1 gsa, 1 gsm, 2 gsc's, apache server and apache-lb-agent are brought up on pc-lab44
 * 
 * Tests:
 * - 4.10.2: Deploy two instances of petclinic-routing webapp (plain mode) 
 * 
 * Verify BeanLevelProperties Context {web.selector.forwarded=true}
 * 
 * Verify the accessibility of webapp by following urls:
 * http://IP:8080/petclinic
 * http://IP:8081/petclinic
 * 
 * - 4.10.3: Perform relocation of web application instance to GSC on another machine.
 * Verify the accessibility of relocated webapp instance.
 * 
 * - 4.10.4: on balancer management page http://pc-lab44:10101/balancer :
 * Verify that balancer member (Worker URL) has the actual host-name.
 * 
 * - 4.10.5: undeploy the webapp
 * 
 */
public class ApacheLoadBalancerAgentTest extends AbstractWebTest {
    
    private int lbAgentPid;
    private Machine machine;
    private Machine machine2;
    private GridServiceManager gsm;
    private GridServiceContainer gsc;
    private GridServiceContainer gsc2;
    private GridServiceContainer gsc3;
    private HtmlPage page;
    private String ip;
    private String ip2;
    private final String APACHE_HOST = "pc-lab44";
    
    @Override
    @BeforeMethod
    public void beforeTest() {

        super.beforeTest();
        
        // we want to make sure pc-lab44 is available and use it
        log("waiting for 2 machines"); // add exception if pc-lab44 is not found
        assertNotNull("Test has to run on " + APACHE_HOST, admin.getMachines().waitFor(APACHE_HOST, 60, TimeUnit.SECONDS));
        admin.getMachines().waitFor(2);
        
        log("waiting for 2 GSA's");
        admin.getGridServiceAgents().waitFor(2);
        
        machine = admin.getMachines().getMachineByHostName(APACHE_HOST);
        
        GridServiceAgent[] agents = admin.getGridServiceAgents().getAgents(); 
        String hostName = agents[0].getMachine().getHostName();
        
        machine2 = hostName.equals(APACHE_HOST) ? agents[1].getMachine() : agents[0].getMachine();
        
        ip = machine.getHostAddress();
        ip2 = machine2.getHostAddress();
        
        machine.getGridServiceAgents().waitForAtLeastOne().startGridService(
                new GridServiceOptions("apache").argument("-k").argument("start").useScript()
        );
        
        lbAgentPid = machine.getGridServiceAgents().waitForAtLeastOne().startGridService(
                new GridServiceOptions("apache-lb").argument("-apache").argument("/usr/local/apache2").
                argument("-update-interval").argument("1000").useScript()
        );
        
        gsm = loadGSM(machine);
        gsc = loadGSC(machine);
        gsc2 = loadGSC(machine);
    }
    
    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "2")
    public void test() throws IOException, InterruptedException {
        
        // 4.10.2 ####################################################
        
        ProcessingUnit pu = gsm.deploy(
                new ProcessingUnitDeployment(getArchive("petclinic-routing.war")).
                numberOfInstances(2).
                clusterSchema("partitioned").maxInstancesPerVM(1).name("petclinic"));
        
        pu.waitFor(pu.getTotalNumberOfInstances());
      
        assertEquals(pu.getBeanLevelProperties().
                getContextProperties().
                getProperty("web.selector.forwarded"), "true");
        
        webClient.setThrowExceptionOnFailingStatusCode(false);
        
        page = assertPageExists("http://"+ ip +":8080/petclinic");
        assertNotNull(((HtmlPage)page.getAnchorByHref("/petclinic/findOwners.do").click()).
                getAnchorByHref("/petclinic/addOwner.do").click());
        
        page = assertPageExists("http://"+ ip +":8081/petclinic");
        assertNotNull(((HtmlPage)page.getAnchorByHref("/petclinic/findOwners.do").click()).
                getAnchorByHref("/petclinic/addOwner.do").click());
        
        // 4.10.3 ####################################################
        
        gsc3 = loadGSC(machine2);
        
        final CountDownLatch puAddedLatch = new CountDownLatch(1);
        gsc3.getProcessingUnitInstanceAdded().add(new ProcessingUnitInstanceAddedEventListener() {
            public void processingUnitInstanceAdded(ProcessingUnitInstance processingUnitInstance) {
                log("event: " + getProcessingUnitInstanceName(processingUnitInstance)
                        + " instantiated on " + gscToString(gsc3));
                puAddedLatch.countDown();
            }
        });
        
        // checking port before relocation to IP2, it should stay the same
        int portOnIp2 = gsc.getProcessingUnitInstances()[0].getJeeDetails().getPort();
        int portOnIp1 = (portOnIp2 == 8080) ? 8081 : 8080;
        
        gsc.getProcessingUnitInstances()[0].relocate(gsc3);
        log("waiting for instance to be relocated to " + gscToString(gsc3));
        assertTrue(puAddedLatch.await(60, TimeUnit.SECONDS));
        
        assertPageExists("http://"+ ip +":" + portOnIp1 + "/petclinic");
        assertNotNull(((HtmlPage)page.getAnchorByHref("/petclinic/findOwners.do").click()).
                getAnchorByHref("/petclinic/addOwner.do").click());
        assertPageExists("http://"+ ip2 +":" + portOnIp2 + "/petclinic");
        assertNotNull(((HtmlPage)page.getAnchorByHref("/petclinic/findOwners.do").click()).
                getAnchorByHref("/petclinic/addOwner.do").click());
        
        
        assertPageExists("http://" + APACHE_HOST + ":10101/petclinic");
        assertNotNull(((HtmlPage)page.getAnchorByHref("/petclinic/findOwners.do").click()).
                getAnchorByHref("/petclinic/addOwner.do").click());
        
        // 4.10.4 ####################################################
        
        // waiting for apache-lb-agent to finish update
        // TODO remember to change interval when running
        Thread.sleep(2000);
        
        page = assertPageExists("http://" + APACHE_HOST + ":10101/balancer");
        assertHtmlContainsText(page, "http://"+ ip +":" + portOnIp1 + "/petclinic");
        assertHtmlContainsText(page, "http://"+ ip2 +":" + portOnIp2 + "/petclinic");

        // 4.10.5 ####################################################
        
        final CountDownLatch removedLatch = new CountDownLatch(1);
        admin.getProcessingUnits().getProcessingUnitRemoved().add(new ProcessingUnitRemovedEventListener() {
            public void processingUnitRemoved(ProcessingUnit processingUnit) {
                removedLatch.countDown();
            }
        });
        
        pu.undeploy();
        
        assertTrue(removedLatch.await(60, TimeUnit.SECONDS));
        
    }
    
    @Override
    @AfterMethod
    public void afterTest() {
        
        //doesn't work well, will kill it with SSH in assertCleanSetup
        //machine.getGridServiceAgent().killByAgentId(lbAgentPid);
        
        machine.getGridServiceAgents().waitForAtLeastOne().startGridService(
                new GridServiceOptions("apache").argument("-k").argument("stop").useScript()
        );
     
        super.afterTest();
    }
    
}
