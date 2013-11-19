package org.cloudifysource.esc.driver.provisioning.network

import java.util.Map
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

public class NetworkProvisioningDriverClass extends org.cloudifysource.esc.driver.provisioning.network.BaseNetworkDriver {

	@Override
	public String allocateFloatingIP(final String poolName, final Map<String, Object> context,
			final long duration, final TimeUnit timeUnit) throws NetworkProvisioningException, TimeoutException {
		return "it works";
	}
}