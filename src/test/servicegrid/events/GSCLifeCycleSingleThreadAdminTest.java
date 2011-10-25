package test.servicegrid.events;

import org.openspaces.admin.Admin;

import test.utils.AdminUtils;

public class GSCLifeCycleSingleThreadAdminTest extends GSCLifeCycleTest{
	
	@Override
	protected Admin newAdmin() {
		return AdminUtils.createSingleThreadAdmin();
	}
}
