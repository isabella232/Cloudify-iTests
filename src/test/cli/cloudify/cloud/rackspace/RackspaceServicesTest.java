package test.cli.cloudify.cloud.rackspace;

import java.io.IOException;

import org.cloudifysource.restclient.RestException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import test.cli.cloudify.cloud.AbstractServicesTest;

public class RackspaceServicesTest extends AbstractServicesTest{

	@Override
	protected String getCloudName() {
		return "rackspace";
	}
	
	@BeforeClass(alwaysRun = true)
	protected void bootstrap() throws Exception {
		super.bootstrap();
	}
	
	@AfterClass(alwaysRun = true)
	protected void teardown() throws Exception {
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
