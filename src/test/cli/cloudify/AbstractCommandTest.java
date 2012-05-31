package test.cli.cloudify;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.openspaces.admin.pu.elastic.ElasticStatelessProcessingUnitDeployment;
import org.openspaces.admin.pu.elastic.config.DiscoveredMachineProvisioningConfigurer;
import org.openspaces.admin.pu.elastic.config.ManualCapacityScaleConfigurer;
import org.openspaces.core.util.MemoryUnit;
import org.openspaces.pu.service.ServiceDetails;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import framework.utils.AdminUtils;
import framework.utils.LogUtils;
import framework.utils.PortConnectionUtils;

import test.AbstractTest;

@Deprecated
public abstract class AbstractCommandTest extends AbstractTest {

	private static final long REST_PROCESSINGUNIT_TIMEOUT_SEC = 60;
	protected static final String DEFAULT_APPLICTION_NAME = "default";
	protected final String restPort = "8101";
	private static final int PU_UNDEPLOY_TIMEOUT = 20000;
	
	protected String restUrl = null;
	
	@Override
	@BeforeMethod
	public void beforeTest() {
		super.beforeTest();
		AdminUtils.loadGSM(admin.getGridServiceAgents().waitForAtLeastOne());
		AdminUtils.loadESM(admin.getGridServiceAgents().waitForAtLeastOne());
		this.restUrl = deployRestServer();
	}

	@Override
	@AfterMethod
	public void afterTest() throws Exception {
		if (admin != null) {
			// gracefully undeploy all PUs
			long end = System.currentTimeMillis() + PU_UNDEPLOY_TIMEOUT* admin.getProcessingUnits().getSize();
			for (ProcessingUnit pu : admin.getProcessingUnits()) {
				pu.undeploy();
			}
			
			while (admin.getProcessingUnits().getSize() > 0 && 
					System.currentTimeMillis() < end) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
				}
			}
		}
		super.afterTest();
	}

	// This method is to be used ONLY when using REST-Admin commands that
	// involve the connect command.
	public String deployRestServer() {
		LogUtils.log("Deploying REST Server in GSC");
		final GridServiceManager gsm = admin.getGridServiceManagers().waitForAtLeastOne();
		
		for(Machine machine : admin.getMachines()){
			boolean portOpenBeforeRestDeployment = 
					PortConnectionUtils.isPortOpen(machine.getHostAddress(), Integer.parseInt(restPort));
			
			assertTrue("port "+restPort +" is open on " + machine.getHostAddress() + " before rest deployment. will not try to deploy rest"
					, !portOpenBeforeRestDeployment);
		}
			
		//final ProcessingUnit pu = gsm.deploy(new ProcessingUnitDeployment(Constants.RESTFUL_WAR_PATH));
		final ProcessingUnit pu = gsm.deploy(new ElasticStatelessProcessingUnitDeployment(Constants.RESTFUL_WAR_PATH)
		.memoryCapacityPerContainer(128,MemoryUnit.MEGABYTES)
		.addContextProperty("com.gs.application","Management")
		.addContextProperty("web.port", restPort)
        .addContextProperty("web.context.unique", "true")
		.scale(new ManualCapacityScaleConfigurer().memoryCapacity(128,MemoryUnit.MEGABYTES).create())
		.sharedMachineProvisioning("public",
		new DiscoveredMachineProvisioningConfigurer()
		.reservedMemoryCapacityPerMachine(256,MemoryUnit.MEGABYTES)
		.create())
		);

		final boolean deploymentResult =
				pu.waitFor(1, REST_PROCESSINGUNIT_TIMEOUT_SEC, TimeUnit.SECONDS);

		if (!deploymentResult) {
			AbstractTest.AssertFail("in deployRestServer: failed to deploy rest server");
		}
		final ProcessingUnitInstance pui = pu.getInstances()[0];
		final ServiceDetails restServiceDetails = pui.getServiceDetailsByServiceId("jee-container");

		final String url = "http://" + restServiceDetails.getAttributes().get("host") + ":" +
		restServiceDetails.getAttributes().get("port") +
		restServiceDetails.getAttributes().get("context-path");
		
		LogUtils.log("REST Server is available at: " + url);
		return url;
	}
	
	protected String runCommand(String command) throws IOException, InterruptedException {
		return CommandTestUtils.runCommandAndWait(command);
	}

}
