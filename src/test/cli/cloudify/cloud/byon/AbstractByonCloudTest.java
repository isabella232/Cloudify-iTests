package test.cli.cloudify.cloud.byon;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.openspaces.admin.Admin;
import org.openspaces.admin.AdminFactory;
import org.testng.ITestContext;

import test.AbstractTest;
import test.cli.cloudify.cloud.NewAbstractCloudTest;
import test.cli.cloudify.cloud.services.byon.ByonCloudService;
import framework.tools.SGTestHelper;
import framework.utils.LogUtils;
import framework.utils.SSHUtils;

public class AbstractByonCloudTest extends NewAbstractCloudTest {
	
	protected Admin admin;

	public ByonCloudService getService() {
		return (ByonCloudService) super.getService();
	}
	
	@Override
	protected void beforeBootstrap() throws Exception {
		super.beforeBootstrap();
		cleanMachines();
		printBootstrapManagementFile();
	}


	@Override
	protected void bootstrap(ITestContext testContext) {
		super.bootstrap(testContext);
	}


	@Override
	protected void afterBootstrap() throws Exception {
		super.afterBootstrap();
		createAdmin();
	}
	
	@Override
	protected void afterTeardown() throws Exception {
		super.afterTeardown();
		cleanMachines();
	}


	private void createAdmin() {
		String[] managementHosts = getService().getRestUrls();
		AdminFactory factory = new AdminFactory();
		for (String host : managementHosts) {
			LogUtils.log("creating admin");
			String utlNoHttp = host.substring(7); // remove "http://"
			String ip = utlNoHttp.split(":")[0];
			factory.addLocators(ip + ":" + CloudifyConstants.DEFAULT_LUS_PORT);
		}
		admin = factory.createAdmin();
	}
	
	@Override
	protected void customizeCloud() throws Exception {
		// use a script that does not install java
		File standardBootstrapManagement = new File(getService().getPathToCloudFolder() + "/upload", "bootstrap-management.sh");
		File customBootstrapManagement = new File(SGTestHelper.getSGTestRootDir() + "/apps/cloudify/cloud/byon/bootstrap-management.sh");
		Map<File, File> filesToReplace = new HashMap<File, File>();
		filesToReplace.put(standardBootstrapManagement, customBootstrapManagement);
		getService().addFilesToReplace(filesToReplace);

	}

	@Override
	protected String getCloudName() {
		return "byon";
	}

	@Override
	protected boolean isReusableCloud() {
		return false;
	}
	
	private void cleanMachines() {
		killAllJavaOnAllHosts();
		cleanGSFilesOnAllHosts();
	}
	
	private void printBootstrapManagementFile() throws IOException {
		String pathToBootstrap = getService().getPathToCloudFolder() + "/upload/bootstrap-management.sh";
		File bootstrapFile = new File(pathToBootstrap);
		if (!bootstrapFile.exists()) {
			LogUtils.log("Failed to print the cloud configuration file content");
			return;
		}
		String cloudConfigFileAsString = FileUtils.readFileToString(bootstrapFile);
		LogUtils.log("Bootstrap-management file: " + bootstrapFile.getAbsolutePath());
		LogUtils.log(cloudConfigFileAsString);
		
	}

	private void cleanGSFilesOnAllHosts() {
		String command = "rm -rf /tmp/gs-files";
		String[] hosts = getService().getMachines();
		for (String host : hosts) {
			try {
				LogUtils.log(SSHUtils.runCommand(host, AbstractTest.OPERATION_TIMEOUT, command, "tgrid", "tgrid"));
			} catch (AssertionError e) {
				LogUtils.log("Failed to clean gs-files on host " + host + " .Reason --> " + e.getMessage());
			}
		}	
	}
	
	private void killAllJavaOnAllHosts() {
		String command = "killall -9 java";
		String[] hosts = getService().getMachines();
		for (String host : hosts) {
			try {
				LogUtils.log(SSHUtils.runCommand(host, AbstractTest.OPERATION_TIMEOUT, command, "tgrid", "tgrid"));
			} catch (AssertionError e) {
				LogUtils.log("Failed to clean gs-files on host " + host + " .Reason --> " + e.getMessage());
			}
		}
	}
}
