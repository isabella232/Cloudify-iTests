package test.servicegrid.events;

import org.openspaces.admin.Admin;

import test.utils.AdminUtils;

public class PartitionIDOnSpaceAddedSingleThreadAdminTest extends PartitionIDOnSpaceAddedTest {

	@Override
	protected Admin newAdmin() {
		return AdminUtils.createSingleThreadAdmin();
	}
	
}
