package test.wan;

import java.util.Map;

import org.openspaces.pu.service.ServiceMonitors;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.AbstractTest;

public class WanPersistenceTest extends AbstractWanTest {

	@Override
	@AfterMethod
	public void afterTest() {
		super.afterTest();
	}

	@Override
	@BeforeMethod
	public void beforeTest() {
		this.gatewayPUFilePath = "./apps/wan/wan-gateway-persistent";
		super.beforeTest();
	}

	/**
	 * @throws Exception
	 */
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "2")
	public void test() throws Exception {

		this.writeDataEntriesToSpace(1000, 1, "Wan Persistence Test");
		
		this.writeEndMarkersAndWait(5, 20000);
		Thread.sleep(10000);
		
		final Map<String, ServiceMonitors> map = getMirrorPU1().getInstances()[0].getStatistics().getMonitors();
		final ServiceMonitors sm = map.get("TestPersistenceEDS");
		AbstractTest.assertNotNull("Could not find TestPersistenceEDS Service Monitors", sm);
		final Integer countObjects = (Integer) sm.getMonitors().get("countObjects");
		final Integer countTypes = (Integer) sm.getMonitors().get("countTypes");

		AbstractTest.assertEquals("Expected one type in Persistence EDS", 2, countTypes.intValue());
		AbstractTest.assertEquals("Expected number of entries in Persistence EDS is wrong", 1005,
				countObjects.intValue());

		

		final Map<String, ServiceMonitors> map2 = getMirrorPU2().getInstances()[0].getStatistics().getMonitors();

		final ServiceMonitors sm2 = map2.get("TestPersistenceEDS");
		AbstractTest.assertNotNull("Could not find TestPersistenceEDS Service Monitors", sm2);
		final Integer countObjects2 = (Integer) sm2.getMonitors().get("countObjects");
		final Integer countTypes2 = (Integer) sm2.getMonitors().get("countTypes");

		AbstractTest.assertEquals("Expected one type in Persistence EDS", 2, countTypes2.intValue());
		AbstractTest.assertEquals("Expected number of entries in Persistence EDS is wrong", 1005,
				countObjects2.intValue());

	}
}
