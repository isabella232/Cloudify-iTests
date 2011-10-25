package test.gateway;

import org.testng.annotations.Test;

public class UpdatePerformanceSingleSource20TargetsTest extends UpdatePerformanceSingleSourceAbstractTest{
	
    public UpdatePerformanceSingleSource20TargetsTest() {
		super();
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups="3", enabled=true)
    public void test_20() throws Exception {
    	runUpdatePerformanceTest(20);
    }
}
