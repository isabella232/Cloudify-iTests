package org.cloudifysource.quality.iTests.test.cli.cloudify.xen;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutionException;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.cloud.xenserver.XenServerMachineProvisioningConfig;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.cloudifysource.quality.iTests.framework.utils.SSHUtils;
import org.cloudifysource.quality.iTests.framework.utils.AssertUtils.RepetitiveConditionProvider;
import org.cloudifysource.quality.iTests.framework.utils.xen.AbstractXenGSMTest;


public class BootstrapLocalCloudXenTest extends AbstractXenGSMTest {
	
    private static final int DEFAULT_LUS_PORT = net.jini.discovery.Constants.getDiscoveryPort();
	private static final int LOCALCLOUD_LUS_PORT = DEFAULT_LUS_PORT +2;
	private GridServiceAgent gsa;
    private String host;
    private String username;
    private String password;
	@Override
	protected void overrideXenServerProperties(XenServerMachineProvisioningConfig machineProvisioningConfig) {
        super.overrideXenServerProperties(machineProvisioningConfig);
        machineProvisioningConfig.setFileAgentRemoteLocation("/opt/bootstrap-localcloud.sh");
    }
	
	@Override
	@BeforeMethod
	public void beforeTest() {
		super.setLookupServicePort(LOCALCLOUD_LUS_PORT);
	    super.setAcceptGSCsOnStartup(true);
	    super.beforeTest();
	}
	
	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT , groups="1")
	public void test() throws InterruptedException, ExecutionException, URISyntaxException {
	    
		gsa = admin.getGridServiceAgents().waitForAtLeastOne();
	    host = gsa.getMachine().getHostAddress();
	    username = getMachineProvisioningConfig().getSshUsername();
	    password = getMachineProvisioningConfig().getSshPassword();
	    
	    repetitiveAssertNumberOfGSAsAdded(1, AbstractTestSupport.OPERATION_TIMEOUT);
	    repetitiveAssertNumberOfGSAsRemoved(0, AbstractTestSupport.OPERATION_TIMEOUT);
	    
	    final URI restAdminURI = new URI("http", null, host, 8100, null, null, null);
	    final URI webUIURI = new URI("http", null, host, 8099, null, null, null);
	    
	    AbstractTestSupport.repetitiveAssertTrue("Failed waiting for REST/WebIU services", new RepetitiveConditionProvider() {
            public boolean getCondition() {
                return isURIAvailable(restAdminURI) &&
                        isURIAvailable(webUIURI);
            }
        }, AbstractTestSupport.OPERATION_TIMEOUT);
	    
	    repetitiveAssertNumberOfGSAsRemoved(0, AbstractTestSupport.OPERATION_TIMEOUT);
	    SSHUtils.runCommand(host, 1000 * 10, cliTeardownLocalcloudCommand(), username, password);
	    AbstractTestSupport.repetitiveAssertTrue("Failed waiting for agent to be removed", new RepetitiveConditionProvider() {
            public boolean getCondition() {
                return admin.getGridServiceAgents().isEmpty();
            }
        }, AbstractTestSupport.OPERATION_TIMEOUT);
	}
	
	private boolean isURIAvailable(URI uri) {
	    HttpClient client = new DefaultHttpClient();
	    HttpGet httpGet = new HttpGet(uri);
	    try {
	        client.execute(httpGet, new BasicResponseHandler());
	        return true;
	    } catch (Exception e) {
	        return false;
	    } finally {
	        client.getConnectionManager().shutdown();
	    }
	}

	private static String cliTeardownLocalcloudCommand() {
	    return "/opt/gigaspaces/tools/cli/cloudify.sh teardown-localcloud -force";
	}
	
}
