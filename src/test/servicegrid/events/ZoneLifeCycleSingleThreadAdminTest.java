package test.servicegrid.events;

import org.openspaces.admin.Admin;

import test.utils.AdminUtils;

public class ZoneLifeCycleSingleThreadAdminTest extends ZoneLifeCycleTest {

	@Override
	protected Admin newAdmin() {
		return AdminUtils.createSingleThreadAdmin();
	}
}
