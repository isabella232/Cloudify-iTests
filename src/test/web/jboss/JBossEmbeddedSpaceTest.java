package test.web.jboss;

import static test.utils.AdminUtils.loadGSC;
import static test.utils.AdminUtils.loadGSM;
import static test.utils.LogUtils.log;

import java.io.IOException;

import org.openspaces.admin.gsa.GridServiceOptions;
import org.openspaces.admin.machine.Machine;
import org.openspaces.core.GigaSpace;
import org.openspaces.core.GigaSpaceConfigurer;
import org.openspaces.core.space.UrlSpaceConfigurer;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import test.data.Data;
import test.web.AbstractWebTest;

import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.j_spaces.core.IJSpace;

/***
 * 
 * Setup: j2ee application 'simplesession.ear' is deployed to JBoss AS,
 *        1 machine, 1 gsa, 1 gsm, 1 gsc.
 *        
 * Test:
 * a: Press 'init' button on 'simplesession' page to start embedded space (within 'simplesession') and to put 10
 * Data objects in it.
 * Now, verify space has 10 objects in it and that object's content is correct.
 * 
 * b: Remove 5 objects from space, modify one object, add 1 new object (from test acting as client of remote space)
 * Now press 'check' button on 'simplesession' page to generate page which should represent current status of space.
 * Verify this content appears.
 * 
 * @author Dan Kilman
 */

public class JBossEmbeddedSpaceTest extends AbstractWebTest {
    
    private HtmlPage page;
    private int jbossProcessID;
    private Machine machine;
    private final String REMOTE_HOST = "pc-lab44";
    
    
    @BeforeMethod
    @Override
    public void beforeTest() {
        
        super.beforeTest();
        
        // we want to make sure pc-lab44 is available and use it
        log("waiting for 1 machine1");
        machine =  admin.getMachines().waitFor(REMOTE_HOST);
        
        log("waiting for 1 GSA");
        admin.getGridServiceAgents().waitFor(1);
        
        loadGSM(machine);
        loadGSC(machine);
        
        jbossProcessID = machine.getGridServiceAgents().waitForAtLeastOne().startGridService(
            new GridServiceOptions("jboss").argument("-b").argument("0.0.0.0").useScript()
        );
        
        try {
            Thread.sleep(60000);
        } catch (InterruptedException e) {
            Assert.fail("While sleeping...", e);
        }
        
    }
    
    //@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1")
    public void test() throws IOException, InterruptedException {
        
        Data data = null;
        
        page = assertPageExists("http://" + REMOTE_HOST + ":8080/simplesession");
        
        // Press 'submit' after selecting 'init'
        HtmlForm form = page.getFormByName("simpleSessionForm");
        HtmlInput initRadio = form.getInputByValue("Init");
        initRadio.click();
        HtmlInput button = form.getInputByName("button");
        page = button.click();
        
        IJSpace space = new UrlSpaceConfigurer("jini://*/*/space").lookupGroups(admin.getGroups()).space();
        GigaSpace gigaSpace = new GigaSpaceConfigurer(space).gigaSpace();
        
        int spaceCount = gigaSpace.count(null);
        assertEquals(10, spaceCount);

        Data[] dataObjects = gigaSpace.readMultiple(new Data(), 10);
        boolean[] validData = new boolean[10];
        for (int i=0; i < 10; i++) {
            int cell = Integer.parseInt(dataObjects[i].getId());
            assertTrue(1 <= cell && cell <= 10);
            validData[cell-1] = true;
        }
        for (int i=0; i < 10; i++) {
            assertTrue(validData[i]);
        }
        
        for (int i=6; i <= 10; i++) {
            data = new Data();
            data.setId(Integer.toString(i));
            gigaSpace.take(data);
        }
        
        data = new Data();
        data.setId("1");
        data.setType(1);
        data.setData("Updated J2EE data");
        gigaSpace.write(data);
        
        data = new Data();
        data.setId("11");
        data.setType(11);
        data.setData("New J2EE data");
        gigaSpace.write(data);
        
        // Press 'submit' after selecting 'check'
        form = page.getFormByName("simpleSessionForm");
        initRadio = form.getInputByValue("Check");
        initRadio.click();
        button = form.getInputByName("button");
        page = button.click();

        assertHtmlContainsText(page, "Object count: 6");
        assertHtmlContainsText(page, "Data ID 1: Updated J2EE data");
        assertHtmlContainsText(page, "Data ID 11: New J2EE data");
        
    }
    
    
    @Override
    @AfterMethod
    public void afterTest() {
        machine.getGridServiceAgent().killByAgentId(jbossProcessID);
        
        super.afterTest();
    }
    
}
