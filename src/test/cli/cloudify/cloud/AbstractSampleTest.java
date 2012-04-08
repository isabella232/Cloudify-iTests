package test.cli.cloudify.cloud;

import org.testng.annotations.BeforeMethod;

import framework.utils.AssertUtils;

public class AbstractSampleTest {
	
	@BeforeMethod(alwaysRun = true)
	public void beforeInAbstract() {
		
		AssertUtils.assertTrue(1 == 2);
	}

}
