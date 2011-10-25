package test.servicegrid.events;

import org.openspaces.admin.Admin;

import test.utils.AdminUtils;

public class SpaceInstanceLifeCycleSingleThreadAdminTest extends SpaceInstanceLifeCycleTest{
	@Override
	protected Admin newAdmin() {
		return AdminUtils.createSingleThreadAdmin();
	}
}
