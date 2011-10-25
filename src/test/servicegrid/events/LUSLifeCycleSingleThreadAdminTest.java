package test.servicegrid.events;

import org.openspaces.admin.Admin;

import test.utils.AdminUtils;

public class LUSLifeCycleSingleThreadAdminTest extends LUSLifeCycleTest{
	
	@Override
	protected Admin newAdmin() {
		return AdminUtils.createSingleThreadAdmin();
	}

}