package test.admin.os;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.os.OperatingSystemDetails.VendorDetails;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.AbstractTest;
import test.utils.AdminUtils;

/**
 * A simple test to verify that details are passed over the wire.
 * 
 * @author Moran Avigdor
 * @since 8.0.4
 */
public class OSVendorDetailsTest extends AbstractTest {

	private GridServiceAgent gsa;
	private GridServiceManager gsm;
	private GridServiceContainer gsc;

	@BeforeMethod
	public void setup() {
		gsa = admin.getGridServiceAgents().waitForAtLeastOne();
		gsm = AdminUtils.loadGSM(gsa);
		gsc = AdminUtils.loadGSC(gsa);
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT , groups="1")
	public void test() throws Exception {

		VendorDetails gsaVendorDetails = gsa.getOperatingSystem().getDetails().getVendorDetails();

		VendorDetails gscVendorDetails = gsc.getOperatingSystem().getDetails().getVendorDetails();
		assertVendorDetails(gsaVendorDetails, gscVendorDetails);
		
		VendorDetails gsmVendorDetails = gsm.getOperatingSystem().getDetails().getVendorDetails();
		assertVendorDetails(gsaVendorDetails, gsmVendorDetails);
	}

	private void assertVendorDetails(VendorDetails gsaVendorDetails,
			VendorDetails otherVendorDetails) {
		assertEquals(otherVendorDetails.getVendor(), gsaVendorDetails.getVendor());
		assertEquals(otherVendorDetails.getVendorName(), gsaVendorDetails.getVendorName());
		assertEquals(otherVendorDetails.getVendorVersion(), gsaVendorDetails.getVendorVersion());
		assertEquals(otherVendorDetails.getVendorCodeName(), gsaVendorDetails.getVendorCodeName());
	}
}
