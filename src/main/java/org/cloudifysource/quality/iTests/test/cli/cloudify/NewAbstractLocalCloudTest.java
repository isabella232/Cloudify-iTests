package org.cloudifysource.quality.iTests.test.cli.cloudify;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
import org.hyperic.sigar.SigarException;
import org.openspaces.admin.Admin;
import org.openspaces.admin.AdminFactory;
import org.openspaces.admin.application.Application;
import org.openspaces.admin.pu.ProcessingUnit;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;

import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
import org.cloudifysource.quality.iTests.test.cli.cloudify.CommandTestUtils.ProcessResult;
import org.cloudifysource.quality.iTests.framework.utils.ApplicationInstaller;
import org.cloudifysource.quality.iTests.framework.utils.AssertUtils;
import org.cloudifysource.quality.iTests.framework.utils.LocalCloudBootstrapper;
import org.cloudifysource.quality.iTests.framework.utils.ProcessUtils;
import org.cloudifysource.quality.iTests.framework.utils.ServiceInstaller;

public class NewAbstractLocalCloudTest extends AbstractTestSupport {
	
	private static final Set<String> suspectProcessNames = new HashSet<String>(
			Arrays.asList("mongo", "mongod", "mongos", "nc"));

	private static final Set<String> suspectJavaProcessNames = new HashSet<String>(
			Arrays.asList(
					"org.codehaus.groovy.tools.GroovyStarter",
					"simplejavaprocess.jar",
					"org.apache.catalina.startup.Bootstrap",
					"org.apache.cassandra.thrift.CassandraDaemon"));
	
	private Admin admin;
	
	@BeforeSuite
	public void bootstrap() throws Exception {
		LocalCloudBootstrapper bootstrapper = new LocalCloudBootstrapper();
		bootstrap(bootstrapper);		
		admin = super.createAdminAndWaitForManagement();
	}
	
    @AfterMethod(alwaysRun = true)
    public void cleanup() throws SigarException, IOException, InterruptedException {
    	
    	uninstallApplications();
    	uninstallService();
    	
    	// scan for leaked processes.
    	boolean processesByBaseNameFound = ProcessUtils.killProcessesByBaseName(suspectJavaProcessNames);
    	boolean javaProcessesByArgsFound = ProcessUtils.killJavaProcessesByArgs(suspectProcessNames);
    	
    	// Fail if leaked processes were found after uninstallation.
    	AssertUtils.assertTrue("Leaked processes found!", !javaProcessesByArgsFound & !processesByBaseNameFound);
    	
    }
	
    private void uninstallApplications() throws IOException, InterruptedException {
		
    	ApplicationInstaller installer;
    	for (Application app : admin.getApplications() ) {
    		final String applicationName = app.getName();
    		if (applicationName.equals(CloudifyConstants.MANAGEMENT_APPLICATION_NAME)) {
    			return; // dont uninstall the management.
    		}
    		installer = new ApplicationInstaller(getRestUrl(), applicationName);
    		installer.uninstall();
    	}
		
	}

	private void uninstallService() throws IOException, InterruptedException {

		ServiceInstaller installer;
    	for (ProcessingUnit pu : admin.getProcessingUnits() ) {
    		final String serviceName = pu.getName();
    		if (serviceName.equals("rest") || serviceName.equals("webui") || serviceName.equals("cloudifyManagementSpace")) {
    			return; // dont uninstall management services.
    		}
    		installer = new ServiceInstaller(getRestUrl(), serviceName);
    		installer.uninstall();
    	}
	}

	@AfterSuite
    public void teardown() throws IOException, InterruptedException {
    	LocalCloudBootstrapper bootstrapper = new LocalCloudBootstrapper();
    	if (admin != null) {
    		admin.close();
    	}
    	bootstrapper.teardown();
    }

	protected ProcessResult bootstrap(LocalCloudBootstrapper bootstrapper) throws Exception {
		ProcessResult bootstrapResult = bootstrapper.bootstrap();
		if (bootstrapper.isBootstraped()) {
			// only create admin if bootstrap was successful
			admin = super.createAdminAndWaitForManagement();
		}
		return bootstrapResult;
	}
    
    @Override
    protected AdminFactory createAdminFactory() {
		AdminFactory factory = new AdminFactory();
		factory.addLocator("127.0.0.1:" + CloudifyConstants.DEFAULT_LOCALCLOUD_LUS_PORT);
		return factory;
    }
    
    protected String getRestUrl() {
    	return "http://127.0.0.1:8100";
    }
}
