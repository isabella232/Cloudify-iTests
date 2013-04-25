package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.ec2;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import iTests.framework.tools.SGTestHelper;
import org.cloudifysource.quality.iTests.framework.utils.AssertUtils;
import org.cloudifysource.quality.iTests.framework.utils.CloudBootstrapper;
import org.cloudifysource.quality.iTests.framework.utils.IOUtils;
import org.cloudifysource.quality.iTests.framework.utils.JCloudsUtils;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.NewAbstractCloudTest;
import org.jclouds.compute.domain.NodeMetadata;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * This test makes a bootstrap on ec2 fail by changing the JAVA_HOME path to a bad one in the bootstrap-management.sh file.
 * <p>After the bootstrap fails, the test checks if the management machine was shutdown.
 * 
 * @author nirb
 *
 */
public class BootstrapFailureEc2Test extends NewAbstractCloudTest {

	private NodeMetadata managementMachine;
	private static final long TIME_TO_TERMINATE_IN_MILLS = 60000;
	private boolean managementMachineTerminated = false;

	@BeforeClass
	public void bootstrap() throws Exception {
        CloudBootstrapper bootstrapper = new CloudBootstrapper();
        bootstrapper.setBootstrapExpectedToFail(true);
        super.bootstrap(bootstrapper);
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, enabled = true)
	public void failedBootstrapTest() throws IOException, InterruptedException {
		
		JCloudsUtils.createContext(getService());
		Set<? extends NodeMetadata> machines = JCloudsUtils.getServersByName(getService().getMachinePrefix());
		AssertUtils.assertTrue("Found running management instances even though bootstrap failed", machines == null);
	}

	@AfterClass
	public void teardown() throws Exception {
		JCloudsUtils.closeContext();		
		if (!managementMachineTerminated) {
			super.teardown();			
		}		
	}

	@Override
	protected String getCloudName() {
		return "ec2";
	}

	@Override
	protected boolean isReusableCloud() {
		return false;
	}
	
	@Override
	public void beforeBootstrap() throws IOException {
		//replace the bootstrap-management with a bad version, to fail the bootstrap.
		File standardBootstrapManagement = new File(getService().getPathToCloudFolder() + "/upload", "bootstrap-management.sh");
		File badBootstrapManagement = new File(SGTestHelper.getSGTestRootDir() + "/src/main/resources/apps/cloudify/cloud/ec2/bad-bootstrap-management.sh");
		IOUtils.replaceFile(standardBootstrapManagement, badBootstrapManagement);
	}
}
