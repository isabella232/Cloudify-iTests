package test.wan;

import org.openspaces.admin.pu.ProcessingUnitDeployment;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.AbstractTest;
import test.data.Data;

public class WanFilterTest extends AbstractWanTest {

	private static final String FILTER_MODIFIED = "FILTER -- MODIFIED";
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

	private ProcessingUnitDeployment addFilterProps(final ProcessingUnitDeployment pud) {

		return pud
				.setContextProperty("classToFilter", Data.class.getName())
				.setContextProperty("fieldToFilter", "type")
				.setContextProperty("valueToFilter", "100")
				.setContextProperty("fieldToModify", "data")
				.setContextProperty("valueToModify", FILTER_MODIFIED);
	}

	@Override
	protected org.openspaces.admin.pu.ProcessingUnitDeployment createPUDeploymentForMirror2(final String ip1,
			final String ip2) {
		return addFilterProps(super.createPUDeploymentForMirror2(ip1, ip2));
	};

	@Override
	protected org.openspaces.admin.pu.ProcessingUnitDeployment createPUDeploymentForMirror1(final String ip1,
			final String ip2) {
		return addFilterProps(super.createPUDeploymentForMirror1(ip1, ip2));
	};

	/**
	 * @throws Exception
	 */
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "2" )
	public void testFilterDiscard() throws Exception {

		final Data data1 = new Data();
		data1.setData("Basic Test");
		data1.setId("1");
		data1.setType(100);
		gigaSpace1.write(data1);

		final Data data2 = new Data();
		data2.setId("2");
		data2.setType(1);
		data2.setData("MARKER");
		gigaSpace1.write(data2);

		// wait until marker arrives
		Data template = new Data();
		template.setId("2");
		template.setType(1);
		final Data marker = gigaSpace2.read(template, 30000);
		AbstractTest.assertNotNull("Marker Object did not arrive at second space", marker);

		final int localCount = gigaSpace1.count(new Data());
		final int remoteCount = gigaSpace2.count(new Data());
		AbstractTest.assertEquals("Local count should be 1", localCount, 2);
		AbstractTest.assertEquals("Remote count should be 1", remoteCount, 1);

	}
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "2")
	public void testFilterModify() throws Exception {

		final Data data1 = new Data();
		data1.setData("Basic Test");
		data1.setId("1");
		data1.setType(1);
		gigaSpace1.write(data1);		

		// wait until object arrives
		Data template = new Data(1);
		template.setId("1");
		
		final Data res = gigaSpace2.read(template, 15000);
		AbstractTest.assertNotNull("Object did not arrive at remote space", res);

		
		AbstractTest.assertEquals("Remote object not modified", res.getData(), FILTER_MODIFIED);
		

	}

}
