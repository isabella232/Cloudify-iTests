package test.wan;

import org.openspaces.admin.pu.ProcessingUnitDeployment;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.AbstractTest;
import test.data.VersionedData;

public class WanCollisionDetectionTest extends AbstractWanTest {

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

	@Override
	protected ProcessingUnitDeployment createPUDeploymentForMirror2(final String ip1, final String ip2) {
		return super.createPUDeploymentForMirror2(ip1, ip2)
				.setContextProperty("localClusterSpaceUrl",
						"jini://*/*/site2Space?versioned=true&groups=" + this.group2);
	};

	@Override
	protected ProcessingUnitDeployment createPUDeploymentForMirror1(final String ip1, final String ip2) {
		return super.createPUDeploymentForMirror1(ip1, ip2)
				.setContextProperty("localClusterSpaceUrl",
						"jini://*/*/site1Space?versioned=true&groups=" + this.group1);
	}

	@Override
	protected ProcessingUnitDeployment createSite1PUDeployment() {
		return super
				.createSite1PUDeployment()
				.setContextProperty(
						"spaceURL",
						"/./site1Space?cluster_schema=partitioned-sync2backup&total_members=1,1&mirror=true&versioned=true&groups="
								+ this.group1);
	}

	@Override
	protected ProcessingUnitDeployment createSite2PUDeployment() {
		return super
				.createSite2PUDeployment()
				.setContextProperty(
						"spaceURL",
						"/./site2Space?cluster_schema=partitioned-sync2backup&total_members=1,1&mirror=true&versioned=true&groups="
								+ this.group2);
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "2")
	public void test() throws Exception {

		final VersionedData template = new VersionedData(1);

		final VersionedData data = new VersionedData();
		data.setData("Basic Test");
		data.setId("1");
		data.setType(1);
		gigaSpace1.write(data);

		final VersionedData ver1 = gigaSpace1.read(template, 5000);
		ver1.setData("SITE1");
		AbstractTest.assertNotNull(ver1);
		final VersionedData ver2 = gigaSpace2.read(template, 20000);
		ver2.setData("SITE2");
		AbstractTest.assertNotNull("Could not find data object in remote site", ver2);

		System.out.println("writing version: " + ver1.getVersionID() + " to site 1");
		System.out.println("writing version: " + ver2.getVersionID() + " to site 1");
		gigaSpace1.write(ver1);
		gigaSpace2.write(ver2);

		final VersionedData res1 = gigaSpace1.read(template, 5000);
		final VersionedData res2 = gigaSpace2.read(template, 20000);

		AbstractTest.assertEquals(res1.getData(), "SITE1");
		AbstractTest.assertEquals(res2.getData(), "SITE2");

	}

}
