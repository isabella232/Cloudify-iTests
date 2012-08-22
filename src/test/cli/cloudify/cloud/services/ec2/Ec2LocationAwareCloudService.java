package test.cli.cloudify.cloud.services.ec2;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.cloudifysource.esc.driver.provisioning.jclouds.DefaultProvisioningDriver;

import framework.utils.IOUtils;

/**
 * Uses a different cloud driver that injects a different EC2 Availability Zone for different start() commands
 * unless the ESM specified a specific Availability Zone to be used.
 * 
 * This is used to mock the behavior of opinionated clouds (ALU CloudBand for example)
 * that decide which zone to start the machine upon, and expect the ESM to do auto-scaling 
 * with location awareness.
 * 
 * @author itaif, elip
 * @since 9.1.0
 */
public class Ec2LocationAwareCloudService extends Ec2CloudService {

	public Ec2LocationAwareCloudService(String uniqueName) {
		super(uniqueName);
	}

	@Override
	protected void injectCloudDriverClass() throws IOException {
		final Map<String, String> propsToReplace = new HashMap<String, String>();
		final String oldCloudDriverClazz = DefaultProvisioningDriver.class.getName();
		final String newCloudDriverClazz = "test.LocationAwareDriver";
		propsToReplace.put(toClassName(oldCloudDriverClazz),toClassName(newCloudDriverClazz));
		IOUtils.replaceTextInFile(getPathToCloudGroovy(), propsToReplace);
	}
	
	public String toClassName(String className) {
		return "className \""+className+"\"";
	}
}
