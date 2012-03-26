package test.cli.cloudify.cloud.ec2;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import test.cli.cloudify.CloudTestUtils;
import framework.tools.SGTestHelper;
import framework.utils.ScriptUtils;

public class Ec2StockDemoCloudService extends Ec2CloudService {
	
	@Override
	public void injectAuthenticationDetails() throws IOException {
		
		File backupEc2Dir = new File(SGTestHelper.getBuildDir() + "/tools/cli/plugins/esc/ec2.backup");
		
		File originalEc2Dir = new File(SGTestHelper.getBuildDir() + "/tools/cli/plugins/esc/ec2");
		FileUtils.copyDirectory(originalEc2Dir, backupEc2Dir);
		
		File bootstapManagement = new File(SGTestHelper.getSGTestRootDir() + "/apps/cloudify/cloud/ec2/bootstrap-management.sh");
		File oldBootstrap = new File(SGTestHelper.getBuildDir() + "/tools/cli/plugins/esc/ec2/upload/bootstrap-management.sh");
		oldBootstrap.delete();
		FileUtils.copyFile(bootstapManagement, oldBootstrap);
		
		File xapLicense = new File(SGTestHelper.getSGTestRootDir() + "/apps/cloudify/cloud/gslicense.xml");
		File cloudifyOverrides = new File(originalEc2Dir.getAbsolutePath() + "/upload/cloudify-overrides");
		if (!cloudifyOverrides.exists()) {
			cloudifyOverrides.mkdir();
		}
		FileUtils.copyFileToDirectory(xapLicense, cloudifyOverrides);
		
		// cloud plugin should include recipe that includes secret key 
		File cloudPluginDir = new File(ScriptUtils.getBuildPath() , "/tools/cli/plugins/esc/ec2/");
		File originalCloudDslFile = new File(cloudPluginDir, cloudName + "-cloud.groovy");
		File backupCloudDslFile = new File(cloudPluginDir, cloudName + "-cloud.backup");

		// Read file contents
		final String originalDslFileContents = FileUtils.readFileToString(originalCloudDslFile);

		// first make a backup of the original file
		FileUtils.copyFile(originalCloudDslFile, backupCloudDslFile);

		String modifiedDslFileContents = originalDslFileContents.
			replace(CloudTestUtils.SGTEST_MACHINE_PREFIX + "cloudify_agent_", CloudTestUtils.SGTEST_MACHINE_PREFIX + this.getClass().getSimpleName() + "cloudify_agent_").replace(CloudTestUtils.SGTEST_MACHINE_PREFIX + "cloudify_manager", CloudTestUtils.SGTEST_MACHINE_PREFIX + this.getClass().getSimpleName() + "cloudify_manager");

		FileUtils.write(originalCloudDslFile, modifiedDslFileContents);


	}

}
