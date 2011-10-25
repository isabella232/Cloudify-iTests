package test.servicegrid.events;

import org.openspaces.admin.Admin;

import test.utils.AdminUtils;

public class PUInstanceLifeCycleSingleThreadAdminTest extends PUInstanceLifeCycleTest {
	
	@Override
	protected Admin newAdmin() {
		return AdminUtils.createSingleThreadAdmin();
	}
	
}
