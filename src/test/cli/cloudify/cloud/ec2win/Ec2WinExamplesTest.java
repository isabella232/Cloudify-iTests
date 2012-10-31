package test.cli.cloudify.cloud.ec2win;

import org.testng.ITestContext;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import test.cli.cloudify.cloud.AbstractExamplesTest;

public class Ec2WinExamplesTest extends AbstractExamplesTest {
	@Override
	protected String getCloudName() {
		return "ec2-win";
	}
	
	@BeforeClass(alwaysRun = true)
	protected void bootstrap(final ITestContext testContext) {
		super.bootstrap(testContext);
	}
	
	@AfterClass(alwaysRun = true)
	protected void teardown() {
		super.teardown();
	}
	
	@AfterMethod(alwaysRun = true)
	public void cleanUp() {
		super.uninstallApplicationIfFound();
		super.scanAgentNodesLeak();
	}
		
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 4, enabled = true)
	public void testTravel() throws Exception {
		super.testTravel();
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 4, enabled = true)
	public void testPetclinicSimple() throws Exception {
		super.testPetclinicSimple();
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 4, enabled = true)
	public void testPetclinic() throws Exception {
		super.testPetclinic();
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 4, enabled = true)
	public void testHelloWorld() throws Exception {
		super.testHelloWorld();
	}
}
