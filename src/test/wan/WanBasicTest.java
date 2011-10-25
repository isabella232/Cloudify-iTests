package test.wan;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.AbstractTest;
import test.data.Data;

public class WanBasicTest extends AbstractWanTest {

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

	/**
	 * @throws Exception
	 */
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "2")
	public void test() throws Exception {
		try {

		// Test Write
		final Data data = new Data();
		data.setData("Basic Test");
		data.setId("1");
		data.setType(1);
		gigaSpace1.write(data);

		final Data template = new Data(1);
		final Data res1 = gigaSpace2.read(template, 20000);

		AbstractTest.assertNotNull("Could not find data object in remote site", res1);

		// Write Test successful

		// Test Update
		data.setData("Basic Test Modified");
		gigaSpace1.write(data);

		final Data template2 = new Data();
		template2.setType(1);
		template2.setData("Basic Test Modified");
		template2.setId("1");
		// blocking read until update arrives to other site
		final Data res2 = gigaSpace2.read(data, 20000);
		AbstractTest.assertNotNull("Could not find data object in remote site", res2);

		// Update Test Successful

		// Test take
		final Data temp = gigaSpace1.take(data);
		AbstractTest.assertNotNull("Could not remove data object from source site", temp);
		Thread.sleep(10000);
		final int count = gigaSpace2.count(data);
		AbstractTest.assertEquals("Expected 0 entries in remote site, found: " + count, count, 0);
		// Take test successful
		} catch(Exception e) {
			System.out.println(e.toString());
			e.printStackTrace();
			throw e;
		}

	}

}
