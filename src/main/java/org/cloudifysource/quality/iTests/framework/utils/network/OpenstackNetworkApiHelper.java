/*******************************************************************************
 * Copyright (c) 2013 GigaSpaces Technologies Ltd. All rights reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.cloudifysource.quality.iTests.framework.utils.network;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.commons.lang.StringUtils;
import org.cloudifysource.domain.cloud.Cloud;
import org.cloudifysource.domain.cloud.compute.ComputeTemplate;
import org.cloudifysource.esc.driver.provisioning.openstack.OpenStackNetworkClient;
import org.cloudifysource.esc.driver.provisioning.openstack.rest.Subnet;

public class OpenstackNetworkApiHelper implements NetworkApiHelper {
	
	private static final int NUM_OF_POSSIBLE_SUBNET_RANGES = 255;
	private static final String CIDR_SEPARATOR = "/";
	private static final String IPv4_SEPARATOR = ".";
	private static final String DEFAULT_GATEWAY_SUFFIX = "111";
	private static final String REGION_SEPARATOR = "/";
	public static final String OPENSTACK_ENDPOINT = "openstack.endpoint";		// example: openstack.endpoint="https://<IP>:5000/v2.0/"
	
	private OpenStackNetworkClient networkClient;
	
	
	public OpenstackNetworkApiHelper(final Cloud cloud, final String computeTemplateName) {
		ComputeTemplate computeTemplate = cloud.getCloudCompute().getTemplates().get(computeTemplateName);
		if (computeTemplate == null) {
			throw new IllegalStateException("Template with name \"" + computeTemplateName + "\" could not be found.");
		}

		String endpoint = null;
		final Map<String, Object> overrides = computeTemplate.getOverrides();
		if (overrides != null && !overrides.isEmpty()) {
			endpoint = (String) overrides.get(OPENSTACK_ENDPOINT);
		}

		final String region = computeTemplate.getImageId().split(REGION_SEPARATOR)[0];
		final String cloudUser = cloud.getUser().getUser();
		final String password = cloud.getUser().getApiKey();

		if (cloudUser == null || password == null) {
			throw new IllegalStateException("Cloud user or password not found");
		}

		final StringTokenizer st = new StringTokenizer(cloudUser, ":");
		final String tenant = st.hasMoreElements() ? (String) st.nextToken() : null;
		final String username = st.hasMoreElements() ? (String) st.nextToken() : null;
		
		try {
			networkClient = new OpenStackNetworkClient(endpoint, username, password, tenant, region);
		} catch (final Exception e) {
			throw new RuntimeException("Failed to initialize network helper : " + e.getMessage(), e);
		}
	}
	
	
	public synchronized String getValidSubnetRange(final String basicRange) throws Exception {
		
		boolean rangeInConflict = false;
		int rangesTested = 1;
		String subnetBasicIP = StringUtils.substringBefore(basicRange, CIDR_SEPARATOR);
		Set<String> SubnetRangesAlreadyInUse = new HashSet<String>();
		
		// validate the subnet range formatting
		validateSubnetRangeFormatting(basicRange);
		
		// retrieve from the cloud all the subnets already being used by this tenant
		List<Subnet> existingSubnets = networkClient.getSubnets();
		for (Subnet subnet : existingSubnets) {
			String existingIP = StringUtils.substringBefore(subnet.getCidr(), CIDR_SEPARATOR);
			SubnetRangesAlreadyInUse.add(existingIP);
		}
		
		rangeInConflict = SubnetRangesAlreadyInUse.contains(subnetBasicIP);
		
		// if the subnet range is already being used - find another free range
		while (rangeInConflict && rangesTested <= NUM_OF_POSSIBLE_SUBNET_RANGES) {
			rangesTested++;
			subnetBasicIP = getNextRange(subnetBasicIP);
			rangeInConflict = SubnetRangesAlreadyInUse.contains(subnetBasicIP);			
		}
		
		if (rangeInConflict) {
			throw new Exception("Failed to find a valid subnet range based on starting range: " + basicRange);
		}
		
		return subnetBasicIP;
	}
	
	/**
	 * Gets the next IP range by changing the 3rd octet.
	 * E.g. 177.86.0.0 => 177.86.1.0 or 177.86.255.0 => 177.86.0.0
	 */
	private String getNextRange(final String basicIP) {
		// get the third octet in the given range
		String [] rangeArr = StringUtils.split(basicIP, ".");
		String thirdOctet = rangeArr[2];
		int nextValue = ((Integer.parseInt(thirdOctet) + 1) % 255);
		
		rangeArr[2] = nextValue + "";
		return StringUtils.join(rangeArr, ".");
	}
	
	
	/**
	 * Validate the subnet is formatted correctly in a CIDR notation with valid values
	 * @param subnetRange the subnet range to validate
	 */
	private void validateSubnetRangeFormatting(final String subnetRange) {
		
		// validate the range include a single "/" for CIDR notation
		String [] rangeSplittedByCidr = StringUtils.split(subnetRange, CIDR_SEPARATOR);
		if (rangeSplittedByCidr.length != 2) {
			throw new IllegalArgumentException("Invalid subnet range: " + subnetRange 
					+ ".  A single \"/\" character is required for a CIDR notation");
		}
		
		String ipAsStr = rangeSplittedByCidr[0];
		String cidrAsStr = rangeSplittedByCidr[1];
		
		// validate the IP value structure
		String [] ipOctets = StringUtils.split(ipAsStr, IPv4_SEPARATOR);
		if (ipOctets.length != 4) {
			throw new IllegalArgumentException("Invalid IP value: " + ipAsStr + ". Expecting a value of this format: "
					+ "0.0.0.0");
		}
		
		// validate the octet values are numbers between 0 and 255
		for (String octetAsStr : ipOctets) {
			int octetAsInt;
			try {
				octetAsInt = Integer.parseInt(octetAsStr);	
				if (octetAsInt < 0 || octetAsInt > 255) {
					throw new IllegalArgumentException("Invalid IP value: \"" + ipAsStr + "\". \"" + octetAsStr + "\" "
							+ "is not in the valid range (0-255)");
				}
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException("Invalid IP value: " + ipAsStr + ". Please use numeric values in "
						+ "this format: 0.0.0.0");
			}
			
		}
		
		// validate the CIDR value is a number between 1 and 31
		int cidrAsInt;
		try {
			cidrAsInt = Integer.parseInt(cidrAsStr);
			if (cidrAsInt < 1 || cidrAsInt > 31) {
				throw new IllegalArgumentException("Invalid CIDR value: \"" + cidrAsStr + "\". The valid range is 1-31");
			}
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Invalid CIDR value: \"" + cidrAsStr + "\". Expecting a number in the range 1-31");
		}

	}
	
	
	public String getGatewayForSubnet(final String subnetRange) {
		// calc the gateway address according to the subnet
        return StringUtils.substringBeforeLast(subnetRange, ".") + "." + DEFAULT_GATEWAY_SUFFIX;
	}

}
