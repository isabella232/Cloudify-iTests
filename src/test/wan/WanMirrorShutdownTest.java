package test.wan;

import org.testng.annotations.Test;

public class WanMirrorShutdownTest extends AbstractWanTest {
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "2")
	public void test() throws Exception {

		getMirrorPU1().getInstances()[0].destroy();

	}

}
