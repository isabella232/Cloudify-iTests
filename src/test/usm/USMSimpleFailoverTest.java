package test.usm;

import static framework.utils.AdminUtils.loadGSCs;
import static framework.utils.AdminUtils.loadGSM;
import static framework.utils.LogUtils.log;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.usm.examples.simplejavaprocess.SimpleBlockingJavaProcessMBean;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;


import org.cloudifysource.dsl.Service;
import org.cloudifysource.dsl.internal.CloudifyConstants;

import framework.utils.LogUtils;
import framework.utils.AssertUtils.RepetitiveConditionProvider;


public class USMSimpleFailoverTest extends UsmAbstractTest {

    private static final String SIMPLE_JAVA_PROCESS_DETAILS_TEXT = "DETAILS TEST";
    private Machine machineA;

    @Override
    @BeforeMethod
    public void beforeTest() {
        super.beforeTest();
        
        //1 GSM and 1 GSC at 1 machines
        log("waiting for 1 machine");
        admin.getMachines().waitFor(1);

        log("waiting for 1 GSA");
        admin.getGridServiceAgents().waitFor(1);

        GridServiceAgent[] agents = admin.getGridServiceAgents().getAgents();
        GridServiceAgent gsaA = agents[0];

        machineA = gsaA.getMachine();

        //Start GSM A, GSC A
        log("starting: 1 GSM and 1 GSC at 1 machines");
        loadGSM(machineA); //GSM A
        loadGSCs(machineA, 1); //GSC A
        this.processName = CloudifyConstants.DEFAULT_APPLICATION_NAME + "." + processName;
    }

    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1")
    public void test() throws Exception {
        Service service = USMTestUtils.usmDeploy(processName, this.serviceFileName);

        ProcessingUnit pu = admin.getProcessingUnits().waitFor(processName);
        pu.waitFor(pu.getTotalNumberOfInstances());
        assertTrue("Service " + processName + " State is not RUNNING.",
        		USMTestUtils.waitForPuRunningState(processName, 60, TimeUnit.SECONDS, admin));
        pu.startStatisticsMonitor();
        
        USMTestUtils.assertMonitors(pu);

        JMXConnector jmxc = getJMXConnector(machineA.getHostAddress());
        SimpleBlockingJavaProcessMBean simpleProcessProxy = getSimpleBlockingJavaProcessMBeanProxy(jmxc);
        
        LogUtils.log("Waiting for pu to be fully deployed");
        assertSimpleJavaProcessIsDeployed(simpleProcessProxy);

        LogUtils.log("calling die method on pu");
        simpleProcessProxy.die();

        LogUtils.log("Waiting for pu to actually die for 10000ms");
        Thread.sleep(10000);
        
        LogUtils.log("Waiting for the pu to be redeployed by the usm");

        // for some reason, renewing the JMXConnector is needed
        jmxc = getJMXConnector(machineA.getHostAddress());
        simpleProcessProxy = getSimpleBlockingJavaProcessMBeanProxy(jmxc);
        assertSimpleJavaProcessIsDeployed(simpleProcessProxy);
        jmxc.close();
        pu.undeploy();

    }

    private JMXConnector getJMXConnector(String ipAddress) throws IOException {
        JMXServiceURL url = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://" + ipAddress + ":9999/jmxrmi");
        JMXConnector jmxc = JMXConnectorFactory.connect(url, null);
        return jmxc;
    }
    
    private SimpleBlockingJavaProcessMBean getSimpleBlockingJavaProcessMBeanProxy(JMXConnector jmxc) throws Exception {
        MBeanServerConnection mbsc = jmxc.getMBeanServerConnection();
        ObjectName mbeanName = new ObjectName("org.openspaces.usm.examples.simplejavaprocess:type=SimpleBlockingJavaProcess");
        SimpleBlockingJavaProcessMBean simpleProcessProxy = (SimpleBlockingJavaProcessMBean) MBeanServerInvocationHandler.newProxyInstance(mbsc, mbeanName, SimpleBlockingJavaProcessMBean.class, false); 
        return simpleProcessProxy;
    }
    
    private void assertSimpleJavaProcessIsDeployed(final SimpleBlockingJavaProcessMBean simpleProcessProxy) {
        repetitiveAssertTrue("Failed waiting for pu to deploy", new RepetitiveConditionProvider() {
            public boolean getCondition() {
                try {
                    return simpleProcessProxy.getDetails().equals(SIMPLE_JAVA_PROCESS_DETAILS_TEXT);
                } catch (Exception e) {
                    return false;
                }
            }
        }, OPERATION_TIMEOUT);
    }
    
}
