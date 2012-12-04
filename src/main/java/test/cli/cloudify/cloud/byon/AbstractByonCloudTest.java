package test.cli.cloudify.cloud.byon;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.openspaces.admin.Admin;
import org.openspaces.admin.AdminFactory;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitInstance;

import framework.utils.AssertUtils;
import framework.utils.LogUtils;

import test.cli.cloudify.cloud.NewAbstractCloudTest;
import test.cli.cloudify.cloud.services.byon.ByonCloudService;

public class AbstractByonCloudTest extends NewAbstractCloudTest {

	private static final int CREATE_ADMIN_TIMEOUT = 120 * 1000; // two minutes

	@Override
	protected void beforeTeardown() throws Exception {
		closeAdmin();
	}
	
	protected boolean isFilteredAdmin() {
		return false;
	}

	protected Admin admin;

	public ByonCloudService getService() {
		return (ByonCloudService) super.getService();
	}


	@Override
	protected void bootstrap() throws Exception {
		super.bootstrap();
	}


	@Override
	protected void afterBootstrap() throws Exception {
		super.afterBootstrap();
		createAdmin();
	}

	protected void createAdmin() throws TimeoutException, InterruptedException {
		
		long endTime = System.currentTimeMillis() + CREATE_ADMIN_TIMEOUT;

		// TODO elip - Remove this once GS-10453 is fixed and use factory.createAndWait();
		while (System.currentTimeMillis() < endTime) {
			admin = createAdminFactory().create();

			try {
				if (!isFilteredAdmin()) {
					// make sure lus is discovered
					AssertUtils.assertTrue("Failed to discover lookup service even though admin was created", admin.getLookupServices().waitFor(1, 1, TimeUnit.MINUTES));
					// make sure rest is discovered
					AssertUtils.assertTrue("Failed to discover lookup service even though admin was created", admin.getProcessingUnits().waitFor("rest", 1, TimeUnit.MINUTES) != null);
					return;
				} else {
					return; // cant wait for management services in filtered admin
				}
			} catch (final AssertionError ae) {
				LogUtils.log("Failed to create admin succesfully --> " + ae.getMessage());
				LogUtils.log("Closing admin and retrying");
				closeAdmin();
				Thread.sleep(5000);
			}

		}
		throw new TimeoutException("Timed out while creating admin");
	}
	
	protected AdminFactory createAdminFactory() {
		
		ByonCloudService cloudService = getService();
		AdminFactory factory = new AdminFactory();
		String[] managementHosts;
		// if the cloud is using web services 
		if (!cloudService.isNoWebServices()){
			managementHosts = cloudService.getRestUrls();		
			for (String host : managementHosts) {				
				String utlNoHttp = host.substring(7); /* remove "http://" */
				String ip = utlNoHttp.split(":")[0];
				factory.addLocators(ip + ":" + CloudifyConstants.DEFAULT_LUS_PORT);
			}
		}
		// if the cloud is not using web services
		else {
			managementHosts = cloudService.getMachines();			
			String host = managementHosts[0];
			factory.addLocators(host + ":" + CloudifyConstants.DEFAULT_LUS_PORT);
		}
		return factory;
	}

	protected void closeAdmin() {
		if (admin != null) {
			admin.close();
			admin = null;
		}
	}

	@Override
	protected String getCloudName() {
		return "byon";
	}

	@Override
	protected boolean isReusableCloud() {
		return false;
	}


	@Override
	protected void customizeCloud() throws Exception {
		// TODO Auto-generated method stub

	}

	protected List<Machine> getManagementMachines() {
		return getProcessingUnitMachines("rest");
	}

	protected List<Machine> getAgentMachines(final String processingUnitName) {
		return getProcessingUnitMachines(processingUnitName);
	}

	protected List<Machine> getAllMachines() {
		return Arrays.asList(admin.getMachines().getMachines());
	}

	private List<Machine> getProcessingUnitMachines(final String processingUnitName) {
		List<Machine> machines = new ArrayList<Machine>();
		ProcessingUnit pu = admin.getProcessingUnits().waitFor(processingUnitName, OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
		for (ProcessingUnitInstance puInstance : pu.getInstances()) {
			machines.add(puInstance.getMachine());
		}
		return machines;
	}
}
