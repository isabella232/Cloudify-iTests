package test.wan;


import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;


import test.AbstractTest;
import test.data.Data;

public class WanBulkTest extends AbstractWanTest {

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

		// first write 1000 items to the space
		writeDataEntriesToSpace(1000, 0, "Wan Bulk Test -");

		int localCount = gigaSpace1.count(new Data());
		AbstractTest.assertEquals(localCount, 1000);

		writeEndMarkersAndWait(5, 60000);
		

		int remoteCount = gigaSpace2.count(new Data());

		AbstractTest.assertEquals(remoteCount, localCount);

		writeRandomDataEntriesAndWait(1000, 1000, 5, 60000);

		localCount = gigaSpace1.count(new Data());

		remoteCount = gigaSpace2.count(new Data());

		AbstractTest.assertEquals(remoteCount, localCount);
		compareDataResults();

		// TODO - check partition distribution is the same, if number of
		// partitions is the same.

	}

}
