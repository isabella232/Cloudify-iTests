package test.servicegrid.events;

import org.openspaces.admin.Admin;

import test.utils.AdminUtils;

public class PUStatusChangesSingleThreadAdminTest extends PUStatusChangesTest {
	@Override
	protected Admin newAdmin() {
		return AdminUtils.createSingleThreadAdmin();
	}
}
