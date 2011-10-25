package test.vm;

import static test.utils.AdminUtils.loadGSC;
import static test.utils.AdminUtils.loadGSM;
import static test.utils.AdminUtils.loadESM;
import static test.utils.LogUtils.log;

import java.io.IOException;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.openspaces.admin.GridComponent;
import org.openspaces.admin.esm.ElasticServiceManager;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.lus.LookupService;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.vm.VirtualMachine;
import org.openspaces.admin.vm.VirtualMachineDetails;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.AbstractTest;

/**
 * Test jmx url from VirtualMachineDetails 
 * @author Evgeny Fisher
 * @since 8.0
 */
public class JmxUrlTest extends AbstractTest {
	
	private Machine machine;
	private GridServiceAgent gsa;
    private GridServiceManager gsm;
    private GridServiceContainer gsc1;
    private GridServiceContainer gsc2;
    private ElasticServiceManager esm;
    private LookupService lus;
	
    @Override
	@BeforeMethod
	public void beforeTest() {
		
        super.beforeTest();
        
		log("waiting for 1 GSA");
		gsa = admin.getGridServiceAgents().waitForAtLeastOne();
		machine = gsa.getMachine();
		
		log("loading GSM");
		gsm = loadGSM(machine);

        log("loading ESM");
        esm = loadESM( gsa );
		
		
		log("loading 2 GSC on 1 machine");
		
		gsc1 = loadGSC(machine);
		gsc2 = loadGSC(machine);
        lus = admin.getLookupServices().getLookupServices()[0];
	}	
    	
    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1")
    public void vmTest() throws IOException {
        testGridComponent(gsa);
        testGridComponent(gsm);
        testGridComponent(esm);
        testGridComponent(gsc1);
        testGridComponent(gsc2);
        testGridComponent(lus);
    }
	
    private void testGridComponent(GridComponent gridComponent) throws IOException {
        VirtualMachine virtualMachine = gridComponent.getVirtualMachine();
        VirtualMachineDetails details = virtualMachine.getDetails();
        String jmxUrl = details.getJmxUrl();
        log("jmxUrl=" + jmxUrl + " for " + getServiceName(gridComponent));
        createConnection(jmxUrl);
        log("Created connection for jmxUrl=" + jmxUrl);
    }
	
    private void createConnection(String jmxUrl) throws IOException {
        JMXServiceURL url = new JMXServiceURL(jmxUrl);
        JMXConnector jmxc = JMXConnectorFactory.connect(url, null);
        MBeanServerConnection mBeanServerConnection = jmxc.getMBeanServerConnection();
        Assert.assertNotNull(mBeanServerConnection);
    }
	
    private String getServiceName(GridComponent gridComponent) {

        if (gridComponent instanceof ElasticServiceManager) {
            return "esm";
        } else if (gridComponent instanceof GridServiceAgent) {
            return "gsa";
        } else if (gridComponent instanceof GridServiceManager) {
            return "gsm";
        } else if (gridComponent instanceof GridServiceContainer) {
            return "gsc";
        }

        return "lus";
    }
}