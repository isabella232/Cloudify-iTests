package test.gateway.xen;

import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.testng.Assert;

import test.gsm.AbstractXenGSMTest;
import test.utils.SSHUtils;


/**
 * TODO: extract AbstractXenTest and let AbstractXenGSMTest and AbstractXenWANTest inherit from it
 * @author Dank
 */
public class GatewayWanPrototypeTest extends AbstractXenGSMTest {

    private static final String WANEM_IP = "192.168.10.51";
    
    private static final String SSH_USERNAME = "root";
    private static final String SSH_PASSWORD = "123456";
    
//    @Test(timeOut = DEFAULT_TEST_TIMEOUT)
    public void test() throws InterruptedException {

        GridServiceAgent gsa1 = admin.getGridServiceAgents().waitForAtLeastOne();
        GridServiceAgent gsa2 = startNewVM(30, TimeUnit.MINUTES);

        String ip1 = gsa1.getMachine().getHostAddress();
        String ip2 = gsa2.getMachine().getHostAddress();
        
        addRoute(ip1, ip2, WANEM_IP);
        addRoute(ip2, ip1, WANEM_IP);
        
        Thread.sleep(5000);
        
        assertResponseTimeInMillis(ip1, ip2, 2000, 2200);
        assertResponseTimeInMillis(ip2, ip1, 2000, 2200);
       
    }

    private static void addRoute(String srcIP, String dstIP, String routeThroughIP) {
        SSHUtils.runCommand(srcIP, 60000, addRouteCommand(dstIP, routeThroughIP), SSH_USERNAME, SSH_PASSWORD);
    }
    
    private static String addRouteCommand(String dstIP, String routeThroughIP) {
        return "route add -host " + dstIP + " netmask 0.0.0.0 gw " + routeThroughIP;
    }
 
    private static String traceRoute(String srcIP, String dstIP) {
        return SSHUtils.runCommand(srcIP, 60000, traceRouteCommand(dstIP), SSH_USERNAME, SSH_PASSWORD);
    }

    private static String traceRouteCommand(String dstIP) {
        return "traceroute " + dstIP;
    }
    
    public static void assertResponseTimeInMillis(String srcIP, String dstIP, long minMillis, long maxMillis) {
        String traceRoute = traceRoute(srcIP, dstIP);
        String groupRegex = "([0-9]+[\\.]?[0-9]*) ms";
        String regex = "\\(" + dstIP + "\\)  " + groupRegex + "  " + groupRegex + "  " + groupRegex;
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(traceRoute);
        if (matcher.find()) {
            double firstResult = Double.parseDouble(matcher.group(1));
            double secondResult = Double.parseDouble(matcher.group(2));
            double thirdResult = Double.parseDouble(matcher.group(3));
            double average = (firstResult + secondResult + thirdResult) / 3;
            Assert.assertTrue(average >= minMillis && average <= maxMillis, "Unexpected millis");
        } else {
            Assert.fail("Failed checking response time");
        }
    }
    
}

