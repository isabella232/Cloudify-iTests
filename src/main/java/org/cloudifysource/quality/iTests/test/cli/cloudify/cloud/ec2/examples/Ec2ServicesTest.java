package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.ec2.examples;

import iTests.framework.utils.ScriptUtils;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.AbstractServicesTest;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class Ec2ServicesTest extends AbstractServicesTest{

	@Override
	protected String getCloudName() {
		return "ec2";
	}
	
	@BeforeClass(alwaysRun = true)
	protected void bootstrap() throws Exception {
		super.bootstrap();
	}
		
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, enabled = true)
	public void testSolr() throws Exception {
		super.testService(ScriptUtils.getBuildRecipesServicesPath() + "/solr", null);
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, enabled = true)
	public void testActivemq() throws Exception{
		super.testService(ScriptUtils.getBuildRecipesServicesPath() + "/activemq", null);
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, enabled = true)
	public void testHsqldb() throws Exception{
		super.testService(ScriptUtils.getBuildRecipesServicesPath() + "/hsqldb", null);
	}

    @Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, enabled = true)
    public void testApacheLB() throws Exception {
        super.testService(ScriptUtils.getBuildRecipesServicesPath() + "/apacheLB", null);
    }
    
	@AfterClass(alwaysRun = true)
	protected void teardown() throws Exception {
		super.teardown();
	}

}
