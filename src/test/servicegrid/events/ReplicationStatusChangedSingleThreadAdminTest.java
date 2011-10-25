package test.servicegrid.events;

import org.openspaces.admin.Admin;

import test.utils.AdminUtils;

public class ReplicationStatusChangedSingleThreadAdminTest extends ReplicationStatusChangedTest {
	
	@Override
	protected Admin newAdmin() {
		return AdminUtils.createSingleThreadAdmin();
	}
}
