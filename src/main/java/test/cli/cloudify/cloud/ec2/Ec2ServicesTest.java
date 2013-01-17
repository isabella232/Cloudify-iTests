package test.cli.cloudify.cloud.ec2;

import org.cloudifysource.restclient.RestException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import test.cli.cloudify.cloud.AbstractServicesTest;

import java.io.IOException;

public class Ec2ServicesTest extends AbstractServicesTest{

	@Override
	protected String getCloudName() {
		return "ec2";
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

    @Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, enabled = true)
    public void testApacheLB() throws IOException, InterruptedException, RestException{
        super.testService("apacheLB", "apacheLB");
    }

}
