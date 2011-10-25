package test.wan;

import java.util.Map;

import org.openspaces.pu.service.ServiceMonitors;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.AbstractTest;

public class WanMonitorsTest extends AbstractWanTest {

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

		final Map<String, ServiceMonitors> map = getMirrorPU1().getInstances()[0].getStatistics().getMonitors();
		AbstractTest.assertNotNull(map);
		AbstractTest.assertNotNull(map.get("WAN_MIRROR"));

	}
}
