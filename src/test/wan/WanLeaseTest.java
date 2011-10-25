package test.wan;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.AbstractTest;
import test.data.Data;

public class WanLeaseTest extends AbstractWanTest {

	@Override
	@AfterMethod
	public void afterTest() {
		super.afterTest();
	}

	@Override
	@BeforeMethod
	public void beforeTest() {

		super.beforeTest();
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "2")
	public void test() throws Exception {

		// Write an entry
		final Data data = new Data();
		data.setData("Basic Test");
		data.setId("1");
		data.setType(1);
		gigaSpace1.write(data, 2000);

		final Data template = new Data(1);
		final Data res1 = gigaSpace2.read(template, 20000);

		AbstractTest.assertNotNull("Could not find data object in remote site", res1);

		// wait until after expiration
		Thread.sleep(4000);

		// Verify data has expired
		final int localCount = gigaSpace1.count(template);
		AbstractTest.assertEquals(0, localCount);

		// check data expired on remote cluster
		final int remoteCount = gigaSpace2.count(template);
		AbstractTest.assertEquals(0, remoteCount);
	}

}
