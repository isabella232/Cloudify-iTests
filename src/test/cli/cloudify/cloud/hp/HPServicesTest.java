package test.cli.cloudify.cloud.hp;

import java.io.IOException;

import org.cloudifysource.restclient.RestException;
import org.testng.ITestContext;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import test.cli.cloudify.cloud.AbstractServicesTest;

public class HPServicesTest extends AbstractServicesTest{

	@Override
	protected String getCloudName() {
		return "hp";
	}
	
	@BeforeClass(alwaysRun = true)
	protected void bootstrap(final ITestContext testContext) {
		super.bootstrap(testContext);
	}
	
	@AfterClass(alwaysRun = true)
	protected void teardown() {
		super.teardown();
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, enabled = true)
	public void testSolr() throws IOException, InterruptedException, RestException{
		super.testService("solr", "solr");
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, enabled = true)
	public void testActivemq() throws IOException, InterruptedException, RestException{
		super.testService("activemq", "activemq");
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, enabled = true)
	public void testHsqldb() throws IOException, InterruptedException, RestException{
		super.testService("hsqldb", "hsqldb");
	}
	


}
