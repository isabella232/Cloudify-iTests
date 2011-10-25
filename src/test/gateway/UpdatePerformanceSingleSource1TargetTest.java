package test.gateway;

import org.testng.annotations.Test;

public class UpdatePerformanceSingleSource1TargetTest extends UpdatePerformanceSingleSourceAbstractTest {

	public UpdatePerformanceSingleSource1TargetTest() {
		super();
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups="3", enabled=true)
    public void test_1() throws Exception {
    	runUpdatePerformanceTest(1);
    }
}
