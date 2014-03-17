package org.cloudifysource.quality.iTests.framework.utils.network;


/**
 * @author noak
 * @since 2.7.1
 */
public interface NetworkApiHelper {

	String getValidSubnetRange(final String startingRange) throws Exception;
	
	String getGatewayForSubnet(final String subnetRange);
    
}