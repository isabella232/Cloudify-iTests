package test.cli.cloudify.cloud.services.ec2;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.cloudifysource.esc.driver.provisioning.jclouds.DefaultProvisioningDriver;

import framework.tools.SGTestHelper;
import framework.utils.DeploymentUtils;
import framework.utils.IOUtils;
import framework.utils.ScriptUtils;

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
		
		// copy custom location aware driver to cloudify-overrides
		File locationAwareDriver = DeploymentUtils.getArchive("location-aware-driver-2.2.0.jar");
		File uploadOverrides =
				new File(getPathToCloudFolder() + "/upload/cloudify-overrides/");
		if (!uploadOverrides.exists()) {
			uploadOverrides.mkdir();
		}
		File uploadEsmDir = new File(uploadOverrides.getAbsoluteFile() + "/lib/platform/esm");
		File localEsmFolder = new File(SGTestHelper.getBuildDir() + "/lib/platform/esm");
		
		FileUtils.copyFileToDirectory(locationAwareDriver, uploadEsmDir, true);
		FileUtils.copyFileToDirectory(locationAwareDriver, localEsmFolder, false);
		
		// copy openspaces jar from workspace if exists
		if (SGTestHelper.isDevMode()) {
			File opnespacesWorkspaceJar = new File(SGTestHelper.getSGTestRootDir() + "/../openspaces/lib/required/gs-openspaces.jar");
			File openspacesUploadPath = new File(uploadOverrides.getAbsolutePath() + "/lib/required");
			File openspacesLocalPath = new File(ScriptUtils.getBuildPath() + "/lib/required/");
			File gsOpnespacesLocalJar = new File(openspacesLocalPath, "gs-openspaces.jar");
			if (opnespacesWorkspaceJar.exists()) {
				FileUtils.copyFileToDirectory(opnespacesWorkspaceJar, openspacesUploadPath, true);
				if (gsOpnespacesLocalJar.exists()) {
					FileUtils.deleteQuietly(openspacesLocalPath);
				}
				FileUtils.copyFileToDirectory(opnespacesWorkspaceJar, openspacesLocalPath, false);
			}
		}
		
		
		final Map<String, String> propsToReplace = new HashMap<String, String>();
		final String oldCloudDriverClazz = DefaultProvisioningDriver.class.getName();
		final String newCloudDriverClazz = "org.cloudifysource.test.LocationAwareDriver";
		propsToReplace.put(toClassName(oldCloudDriverClazz),toClassName(newCloudDriverClazz));
		IOUtils.replaceTextInFile(getPathToCloudGroovy(), propsToReplace);
	}
	
	public String toClassName(String className) {
		return "className \""+className+"\"";
	}
	
}
