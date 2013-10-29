package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.byon;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import iTests.framework.utils.LogUtils;
import org.openspaces.admin.Admin;
import org.openspaces.admin.AdminFactory;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitInstance;

import org.apache.commons.lang.StringUtils;
import org.cloudifysource.dsl.utils.IPUtils;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.NewAbstractCloudTest;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.services.byon.ByonCloudService;

public class AbstractByonCloudTest extends NewAbstractCloudTest {
	
	@Override
	protected void beforeTeardown() throws Exception {
		closeAdmin();
		((ByonCloudService) this.cloudService).cleanCronTasks();
	}
	
	protected boolean isFilteredAdmin() {
		return false;
	}

	protected Admin admin;

	public ByonCloudService getService() {
		return (ByonCloudService) super.getService();
	}

	@Override
	protected void afterBootstrap() throws Exception {
		super.afterBootstrap();
		admin = super.createAdminAndWaitForManagement();
	}
	
	@Override
	protected AdminFactory createAdminFactory() {

        LogUtils.log("Creating admin factory");
		ByonCloudService byonService = getService();
		AdminFactory factory = super.createAdminFactory();
		String[] managementHosts;

        if (byonService.getBootstrapper().isNoWebServices()) {
            managementHosts = byonService.getMachines();
            String host = managementHosts[0];
            factory.addLocators(IPUtils.getSafeIpAddress(host) + ":" +
            byonService.getCloud().getConfiguration().getComponents().getDiscovery().getDiscoveryPort());
        } else {
            managementHosts = byonService.getRestUrls();
            for (String host : managementHosts) {
                String urlNoHttp;
                if (getService().getBootstrapper().isSecured()) {
                    urlNoHttp = host.substring(8); /* remove "https://" */
                } else {
                    urlNoHttp = host.substring(7); /* remove "http://" */
                }
                String ip = StringUtils.substringBeforeLast(urlNoHttp, ":");
                String locator = IPUtils.getSafeIpAddress(ip) + ":" +
                        byonService.getCloud().getConfiguration().getComponents().getDiscovery().getDiscoveryPort();
                LogUtils.log("Adding locator " + locator);
                factory.addLocators(locator);
            }
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


	protected List<Machine> getManagementMachines() {
		return getProcessingUnitMachines("rest");
	}

	protected List<Machine> getAgentMachines(final String processingUnitName) {
		return getProcessingUnitMachines(processingUnitName);
	}

	protected List<Machine> getAllMachines() {
		return Arrays.asList(admin.getMachines().getMachines());
	}

	protected List<Machine> getProcessingUnitMachines(final String processingUnitName) {
		List<Machine> machines = new ArrayList<Machine>();
		ProcessingUnit pu = admin.getProcessingUnits().waitFor(processingUnitName, OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
		for (ProcessingUnitInstance puInstance : pu.getInstances()) {
			machines.add(puInstance.getMachine());
		}
		return machines;
	}
}
