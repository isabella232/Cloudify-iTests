package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.hp;

import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.AbstractServicesTest;
import org.cloudifysource.restclient.RestException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;

public class HPServicesTest extends AbstractServicesTest {

	@Override
	protected String getCloudName() {
		return "hp";
	}
	
	@BeforeClass(alwaysRun = true)
	protected void bootstrap() throws Exception {
		super.bootstrap();
	}
	
	@AfterClass(alwaysRun = true)
	protected void teardown() throws Exception {
		super.teardown();
	}
	
	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 2, enabled = true)
	public void testSolr() throws IOException, InterruptedException, RestException{
		super.testService("solr", "solr");
	}
	
	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 2, enabled = true)
	public void testActivemq() throws IOException, InterruptedException, RestException{
		super.testService("activemq", "activemq");
	}
	
	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 2, enabled = true)
	public void testHsqldb() throws IOException, InterruptedException, RestException{
		super.testService("hsqldb", "hsqldb");
	}

    @Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 2, enabled = true)
    public void testApacheLB() throws IOException, InterruptedException, RestException{
        super.testService("apacheLB", "apacheLB");
    }


}
