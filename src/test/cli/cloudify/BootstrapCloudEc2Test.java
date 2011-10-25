package test.cli.cloudify;

import org.testng.Assert;
import test.utils.AssertUtils.RepetitiveConditionProvider;
import test.utils.ScriptUtils;
import test.utils.WebUtils;

import java.net.URL;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class BootstrapCloudEc2Test extends AbstractCloudEc2Test {

	/**
	 * CLOUDIFY-128
	 * cloud bootstrapping test 
	 * @throws Exception 
	 * @see AbstractCloudEc2Test
	 */
	//@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, groups = "1", enabled = true)
	public void bootstrapEc2CloudTest() throws Exception {
	    
		for (int i = 0; i < NUM_OF_MANAGEMENT_MACHINES; i++) {
			assertWebServiceAvailable(restAdminUrl[i]);
			assertWebServiceAvailable(webUIUrl[i]);
		}
	    
		String connectCommand = "connect " + restAdminUrl[0].toString() + ";";
	    
	    URL machinesURL = getMachinesUrl(restAdminUrl[0].toString());
	    assertEquals("Expecting " + NUM_OF_MANAGEMENT_MACHINES + " machines", 
	    		NUM_OF_MANAGEMENT_MACHINES, getNumberOfMachines(machinesURL));
	    
	    //running install application on simple
	    String installCommand = "install-application --verbose -timeout " + 
		TimeUnit.MILLISECONDS.toMinutes(DEFAULT_TEST_TIMEOUT * 2) + 
		" " + ScriptUtils.getBuildPath() + "/examples/simple";
	    
	    String output = CommandTestUtils.runCommandAndWait(connectCommand + installCommand);
	    
	    Assert.assertTrue(output.contains(INSTALL_SIMPLE_EXPECTED_OUTPUT));

	    // Simple is started with 1 instance so we are expecting one more machine
        assertEquals("Expecting " + (NUM_OF_MANAGEMENT_MACHINES+1) + " machines", 
                NUM_OF_MANAGEMENT_MACHINES+1, getNumberOfMachines(machinesURL));
	    
	    
	    //uninstall simple application
	    String uninstallCommand = "uninstall-application --verbose simple";
	    output = CommandTestUtils.runCommandAndWait(connectCommand + uninstallCommand);
	    
	    Assert.assertTrue(output.contains(UNINSTALL_SIMPLE_EXPECTED_OUTPUT));
	    
	}
	
	private static void assertWebServiceAvailable(final URL url) {
        repetitiveAssertTrue(url + " is not up", new RepetitiveConditionProvider() {
            public boolean getCondition() {
                try {
                    return WebUtils.isURLAvailable(url);
                } catch (Exception e) {
                    return false;
                }
            }
        }, OPERATION_TIMEOUT);	    
	}
	
    private static int getNumberOfMachines(URL machinesRestAdminUrl) throws Exception {
        String json = WebUtils.getURLContent(machinesRestAdminUrl);
        Matcher matcher = Pattern.compile("\"Size\":\"([0-9]+)\"").matcher(json);
        if (matcher.find()) {
            String rawSize = matcher.group(1);
            int size = Integer.parseInt(rawSize);
            return size;
        } else {
            return 0;
        }
    }
	
    private static String stripSlash(String str) {
        if (str == null || !str.endsWith("/")) {
            return str;
        }
        return str.substring(0, str.length()-1);
    }
    
    private static URL getMachinesUrl(String url) throws Exception {
        return new URL(stripSlash(url) + "/admin/machines");
    }
    
}
