package test.cli.cloudify;

import org.junit.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.AbstractTest;
import framework.testng.SGTestNGReporter;
import framework.utils.LogUtils;

public class TeardownMessageTest extends AbstractTest {
	
	@Override
	@BeforeMethod
	public void beforeTest() {	
        LogUtils.log("Test Configuration Started: "+ this.getClass());
	}
		
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = false)
	public void teardownLocalcloudOutputTest() throws Exception{
		try{
			CommandTestUtils.runCommandAndWait("teardown-localcloud -lookup-groups abc");
		}catch(AssertionError e){
			//continue
		}
		String output = SGTestNGReporter.getOutput();
		
		Assert.assertTrue("wrong output", output.contains("Teardown failed. Failed to fetch the currently deployed applications list. For force teardown use the -force flag."));
		output = null;
		try{
			CommandTestUtils.runCommandAndWait("teardown-localcloud -force");
		}catch(AssertionError e){
			//continue
		}
		output = SGTestNGReporter.getOutput();
		Assert.assertTrue("wrong output", output.contains("Teardown aborted, an agent was not found on the local machine")
				&& output.contains("Failed to fetch the currently deployed applications list. Continuing teardown-localcloud.")); 
		
	}
}
