package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud;

//import java.util.Set;
//
//import org.jclouds.ContextBuilder;
//import org.jclouds.aws.ec2.AWSEC2AsyncClient;
//import org.jclouds.aws.ec2.AWSEC2Client;
//import org.jclouds.aws.ec2.services.AWSSecurityGroupClient;
//import org.jclouds.compute.ComputeServiceContext;
//import org.jclouds.ec2.domain.SecurityGroup;
//import org.jclouds.rest.RestContext;

public class RemoveSecurityGroupsScript {

	
	private static String region = "eu-west-1";
	//add creds here.
	private static String userName = "****";
	private static String apiKey = "***";
	private static String cloudProvider = "aws-ec2";

	public static void main(String[] args) {
		
		boolean cleaned = false;
		while (!cleaned) {
//			cleaned = cleanSecurityGroups();
		}
	}

//	static boolean cleanSecurityGroups() {
//		ContextBuilder contextBuilder;
//		ComputeServiceContext buildView = null;
//		try {
//			contextBuilder = ContextBuilder.newBuilder(cloudProvider);
//			contextBuilder.credentials(userName, apiKey);
//			buildView = contextBuilder.buildView(ComputeServiceContext.class);
//		
//			RestContext<AWSEC2Client, AWSEC2AsyncClient> restContext = buildView.unwrap(RestContext.class);
//			AWSEC2Client api = restContext.getApi();
//			Set<String> configuredRegions = api.getConfiguredRegions();
//			for (String string : configuredRegions) {
//				System.out.println(string);
//			}
//
//			AWSSecurityGroupClient securityGroupServices = api.getSecurityGroupServices();
//			Set<SecurityGroup> describeSecurityGroupsInRegion = securityGroupServices.describeSecurityGroupsInRegion(region);
//			for (SecurityGroup securityGroup : describeSecurityGroupsInRegion) {
//				System.out.println(securityGroup.getName());
//				if (securityGroup.getName().startsWith("jclouds#")) {
//					try {
//						securityGroupServices.deleteSecurityGroupInRegion(region, securityGroup.getName());
//						
//					} catch (Exception e) {
//						// we ignore this exception. Usually means request limit was exceeded.
//						System.out.println("Exception " + e.getMessage() );
//					}
//					Thread.sleep(500);
//				}
//			}
//			return true;
//		} catch (Exception e) {
//			if (buildView != null) {
//				buildView.close();
//			}
//			return false;
//		}
//	}
}
