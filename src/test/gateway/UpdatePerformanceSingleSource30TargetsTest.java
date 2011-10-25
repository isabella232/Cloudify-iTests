package test.gateway;

import org.testng.annotations.Test;

public class UpdatePerformanceSingleSource30TargetsTest extends UpdatePerformanceSingleSourceAbstractTest{
	
	public UpdatePerformanceSingleSource30TargetsTest(){
        super();
    }
  
    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups="3", enabled=true)
    public void test_30() throws Exception {
    	runUpdatePerformanceTest(30);
    }
}
