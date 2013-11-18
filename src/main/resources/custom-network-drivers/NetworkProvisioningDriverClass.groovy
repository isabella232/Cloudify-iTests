package org.cloudifysource.esc.driver.provisioning.network

import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

public class NetworkProvisioningDriverClass extends org.cloudifysource.esc.driver.provisioning.network.BaseNetworkDriver {

	@Override
	public String allocateFloatingIP(String poolName, long duration, TimeUnit timeUnit)
			throws NetworkProvisioningException, TimeoutException {
		return "it works";
	}
}