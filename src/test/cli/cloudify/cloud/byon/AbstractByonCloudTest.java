package test.cli.cloudify.cloud.byon;

import framework.utils.LogUtils;
import framework.utils.SSHUtils;
import test.cli.cloudify.cloud.NewAbstractCloudTest;
import test.cli.cloudify.cloud.services.byon.ByonCloudService;

public class AbstractByonCloudTest extends NewAbstractCloudTest {

	public ByonCloudService getService() {
		return (ByonCloudService) getService();
	}
	
	@Override
	protected void customizeCloud() throws Exception {
	}
	
	protected void cleanGSFilesOnAllHosts() {
		String command = "rm -rf /tmp/gs-files";
		String[] hosts = getService().getMachines();
		for (String host : hosts) {
			try {
				LogUtils.log(SSHUtils.runCommand(host, OPERATION_TIMEOUT, command, "tgrid", "tgrid"));
			} catch (AssertionError e) {
				LogUtils.log("Failed to clean gs-files on host " + host + " .Reason --> " + e.getMessage());
			}
		}	
	}
	
	protected void killAllJavaOnAllHosts() {
		String command = "killall -9 java";
		String[] hosts = getService().getMachines();
		for (String host : hosts) {
			try {
				LogUtils.log(SSHUtils.runCommand(host, OPERATION_TIMEOUT, command, "tgrid", "tgrid"));
			} catch (AssertionError e) {
				LogUtils.log("Failed to clean gs-files on host " + host + " .Reason --> " + e.getMessage());
			}
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
}
