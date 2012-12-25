package test.cli.cloudify.cloud.ec2;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.NodeState;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import test.cli.cloudify.cloud.JCloudsUtils;
import test.cli.cloudify.cloud.NewAbstractCloudTest;
import framework.tools.SGTestHelper;
import framework.utils.AssertUtils;
import framework.utils.AssertUtils.RepetitiveConditionProvider;
import framework.utils.IOUtils;
import framework.utils.LogUtils;

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
	public void bootstrap() {	
		try {
			super.bootstrap();
		} catch (Throwable ae) {
			LogUtils.log(ae.getMessage());
		}
		
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, enabled = true)
	public void failedBootstrapTest() throws IOException, InterruptedException {
		
		JCloudsUtils.createContext(getService());
		Set<? extends NodeMetadata> machines = JCloudsUtils.getServersByName(getService().getMachinePrefix());
		Assert.assertTrue(machines != null);
		managementMachine = machines.iterator().next();

		RepetitiveConditionProvider condition = new RepetitiveConditionProvider() {
			@Override
			public boolean getCondition() {
				Set<? extends NodeMetadata> machines = JCloudsUtils.getServersByName(getService().getMachinePrefix());
				managementMachine = machines.iterator().next();
				if (managementMachine.getState() == NodeState.TERMINATED) {
					managementMachineTerminated = true;
				}
				return managementMachineTerminated;
			}
		};

		AssertUtils.repetitiveAssertTrue("management machine was not terminated", condition, TIME_TO_TERMINATE_IN_MILLS);
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
