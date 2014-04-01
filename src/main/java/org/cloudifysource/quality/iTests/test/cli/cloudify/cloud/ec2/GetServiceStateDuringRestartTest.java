package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.ec2;

import iTests.framework.utils.AssertUtils;
import iTests.framework.utils.LogUtils;

import java.net.URL;
import java.util.Set;

import org.cloudifysource.esc.driver.provisioning.MachineDetails;
import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
import org.cloudifysource.quality.iTests.test.cli.cloudify.CommandTestUtils;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.NewAbstractCloudTest;
import org.cloudifysource.restclient.GSRestClient;
import org.cloudifysource.restclient.RestException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.j_spaces.kernel.PlatformVersion;

/**
 * @author noak
 */
public class GetServiceStateDuringRestartTest extends NewAbstractCloudTest {

	private final static String SIMPLE_RECIPE_FOLDER = CommandTestUtils.getPath("src/main/resources/apps/USM/usm/simple");
	private final static String SIMPLE_SERVICE_NAME = "simple";
	private final static String FULL_SERVICE_NAME = "default.simple";
	private final static String SUCCESSFULLY_INSTALLED = "successfully installed";
	private final static String MAINTENANCE_MODE_SET_OUTPUT = "agent failure detection disabled successfully";
	private static final String FAILED_TO_GET_MONITORS = "Failed to get monitors for processing unit instance";
	
	GSRestClient restClient = null;
	
	
    @Override
    protected String getCloudName() {
        return "ec2";
    }

    @BeforeClass(alwaysRun = true)
    protected void bootstrap() throws Exception {
        super.bootstrap();
        restClient = new GSRestClient("", "", new URL(getRestUrl()), PlatformVersion.getVersionNumber());
    }


    @Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 4, enabled = true)
    public void testGetStateDuringInstanceReboot() throws Exception {
    	// install service
    	LogUtils.log("installing service " + SIMPLE_SERVICE_NAME);
    	String installationOutput = installServiceAndWait(SIMPLE_RECIPE_FOLDER, SIMPLE_SERVICE_NAME);
    	AssertUtils.assertTrue("Unexpected service installation output: " + installationOutput, 
    			installationOutput.contains(SUCCESSFULLY_INSTALLED));
    	
    	// get maching running service instance
    	String agentPrefix = this.cloudService.getCloud().getProvider().getMachineNamePrefix();
    	Set<MachineDetails> agentMachines = computeApiHelper.getServersContaining(agentPrefix);
    	AssertUtils.assertTrue("Failed to find machines with agent prefix: " + agentPrefix, agentMachines.size() > 0);
    	MachineDetails md = agentMachines.iterator().next();
    	
    	// invoking maintenance mode so the ESM won't start a new machine when we terminate one
    	String invokeMaintenanceModeCommand = "connect " + getRestUrl() + ";invoke " + SIMPLE_SERVICE_NAME 
    			+ " cloudify:start-maintenance-mode 5";
    	String invokeMaintenanceModeOutput = CommandTestUtils.runCommandExpectedFail(invokeMaintenanceModeCommand);
    	AssertUtils.assertTrue("Unexpected output on invoking maintenance mode: " + invokeMaintenanceModeOutput, 
    			invokeMaintenanceModeOutput.contains(MAINTENANCE_MODE_SET_OUTPUT));
    	
    	// terminate the machine
    	terminateServiceInstance(md.getMachineId(), SIMPLE_SERVICE_NAME);
    	
        // now get the service state
    	String getServiceCommand = "connect " + getRestUrl() + ";list-services";
    	String listServicesOutput = CommandTestUtils.runCommandExpectedFail(getServiceCommand);
    	AssertUtils.assertTrue("Failed to get service status since get_monitors was called", 
    			listServicesOutput.contains(FULL_SERVICE_NAME) && !listServicesOutput.contains(FAILED_TO_GET_MONITORS));
    }

    @AfterClass(alwaysRun = true)
    protected void teardown() throws Exception {
        super.teardown();
    }


    @Override
    protected boolean isReusableCloud() {
        return false;
    }
    
	private void terminateServiceInstance(final String instanceId, final String serviceName)
			throws Exception {
		
		final String serviceRestUrl = "ProcessingUnits/Names/default." + serviceName;
        final int originalNumberOfInstances = (Integer) restClient.getAdminData(serviceRestUrl).get("Instances-Size");
        LogUtils.log("Original number of " + serviceName + " instances is " + originalNumberOfInstances + " (before failover)");
        
		LogUtils.log("Shutting down agent");
        computeApiHelper.shutdownServerById(instanceId);

        LogUtils.log("Waiting for service " + serviceName + " machine to terminate");
		
		AssertUtils.repetitiveAssertTrue("Service " + serviceName + " didn't break", new AssertUtils.RepetitiveConditionProvider() {
            @Override
            public boolean getCondition() {
            	boolean done = false;
	            try {
	                int newNumberOfInstances = (Integer) restClient.getAdminData(serviceRestUrl).get("Instances-Size");
	                LogUtils.log("Number of " + serviceName + " instances is " + newNumberOfInstances);
	                if (newNumberOfInstances < originalNumberOfInstances) {
	                    LogUtils.log(serviceName + " service broke. it now has only " + newNumberOfInstances + " instance(s)");
	                    done = true;
	                }
	                return done;
	            } catch (RestException e) {
	                throw new RuntimeException(e);
	            }
            }
        } , AbstractTestSupport.OPERATION_TIMEOUT * 4);
	}
	
}
