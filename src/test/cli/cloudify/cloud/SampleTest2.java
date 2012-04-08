package test.cli.cloudify.cloud;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import framework.utils.AssertUtils;
import framework.utils.LogUtils;

public class SampleTest2 extends AbstractSampleTest {
	
	@Override
	@BeforeMethod
	public void beforeInAbstract() {
		AssertUtils.assertTrue(1 == 2);
	}
	
	@Test
	public void test() {
		
		LogUtils.log("this is sample test 2");
	}

}
