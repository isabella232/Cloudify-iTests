package test.wan;


import org.openspaces.admin.gsc.GridServiceContainer;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;


import test.AbstractTest;
import test.data.Data;

public class WanMirrorRecoveryTest extends AbstractWanTest {

	

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
		writeDataEntriesToSpace(this.gigaSpace1, 1000, 0, "Wan Mirror Recovery Test -");		

		int localCount = gigaSpace1.count(new Data());
		AbstractTest.assertEquals(localCount, 1000);

		writeEndMarkersAndWait(this.gigaSpace1, this.gigaSpace2, 5, 60000);
		
		int remoteCount = gigaSpace2.count(new Data());
		
		AbstractTest.assertEquals(remoteCount, localCount);
		
		writeDataEntriesToSpace(this.gigaSpace2, 2000, 0, "Wan Mirror Recovery Test -");		

		localCount = gigaSpace2.count(new Data());
		AbstractTest.assertEquals(localCount, 2000);

		writeEndMarkersAndWait(this.gigaSpace2, this.gigaSpace1, 5, 60000);
		
		remoteCount = gigaSpace1.count(new Data());
		
		AbstractTest.assertEquals(remoteCount, localCount);
		

		GridServiceContainer gscMirror1 = this.admin.getProcessingUnits().waitFor(this.site1MirrorName).getInstances()[0].getGridServiceContainer();
		gscMirror1.kill();

		writeRandomDataEntriesAndWait(this.gigaSpace1, this.gigaSpace2, 1000, 3000, 5, 60000);
		writeRandomDataEntriesAndWait(this.gigaSpace2, this.gigaSpace1, 1000, 4000, 5, 60000);

		localCount = gigaSpace1.count(new Data());
		remoteCount = gigaSpace2.count(new Data());

		AbstractTest.assertEquals(remoteCount, localCount);
		compareDataResults();
		
		GridServiceContainer gscMirror2 = this.admin2.getProcessingUnits().waitFor(this.site2MirrorName).getInstances()[0].getGridServiceContainer();
		
		gscMirror2.kill();

		writeRandomDataEntriesAndWait(this.gigaSpace1, this.gigaSpace2, 1000, 5000, 5, 60000);
		writeRandomDataEntriesAndWait(this.gigaSpace2, this.gigaSpace1, 1000, 6000, 5, 60000);

		localCount = gigaSpace1.count(new Data());
		remoteCount = gigaSpace2.count(new Data());

		AbstractTest.assertEquals(remoteCount, localCount);
		compareDataResults();
		

	}
}
