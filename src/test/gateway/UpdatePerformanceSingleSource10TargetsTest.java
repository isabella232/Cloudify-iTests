package test.gateway;

import org.testng.annotations.Test;

public class UpdatePerformanceSingleSource10TargetsTest extends UpdatePerformanceSingleSourceAbstractTest{
	
	public UpdatePerformanceSingleSource10TargetsTest(){
        super();
    }
  
    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups="3", enabled=true)
    public void test_10() throws Exception {
    	runUpdatePerformanceTest(10);
    }
}
