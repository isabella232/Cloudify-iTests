package test.cli.cloudify.cloud.hp;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.testng.Assert;

import framework.tools.SGTestHelper;
import framework.utils.AssertUtils;
import framework.utils.ScriptUtils;

import test.cli.cloudify.CloudTestUtils;
import test.cli.cloudify.cloud.AbstractCloudService;

public class HpCloudService extends AbstractCloudService {
	
	private static final String tenant = "24912589714038";
	private static final String cloudName = "openstack";
	private static final String user = "98173213380893";
	private static final String apiKey = "C5nobOW90bhnCmE5AQaLaJ0Ubd8UISPxGih";
	private static final String pemFileName = "sgtest-hp";
	
	private static HpCloudService self = null;

	private HpCloudService() {};

	public static HpCloudService getService() {
		if (self == null) {
			self = new HpCloudService();
		}
		return self;	
	}

	@Override
	public String getCloudName() {
		return cloudName;
	}

	public String getUser() {
		return user;
	}

	public String getApiKey() {
		return apiKey;
	}

	public String getPemFileName() {
		return pemFileName;
	}
	

	@Override
	public void injectAuthenticationDetails() throws IOException {
		
		String cloudTestPath = (SGTestHelper.getSGTestRootDir() + "/apps/cloudify/cloud/" + cloudName).replace('\\', '/');
		String sshKeyPemName = pemFileName + ".pem";

		// cloud plugin should include recipe that includes secret key 
		File cloudPluginDir = new File(ScriptUtils.getBuildPath() , "tools/cli/plugins/esc/" + cloudName + "/");
		File originalCloudDslFile = new File(cloudPluginDir, cloudName + "-cloud.groovy");
		File backupCloudDslFile = new File(cloudPluginDir, cloudName + "-cloud.backup");

		// Read file contents
		final String originalDslFileContents = FileUtils.readFileToString(originalCloudDslFile);
		Assert.assertTrue(originalDslFileContents.contains("ENTER_USER"), "Missing ENTER_USER statement in " + cloudName + "-cloud.groovy");
		Assert.assertTrue(originalDslFileContents.contains("ENTER_API_KEY"), "Missing ENTER_API_KEY statement in " + cloudName + "-cloud.groovy");
		Assert.assertTrue(originalDslFileContents.contains("ENTER_KEY_FILE"), "Missing ENTER_KEY_FILE statement in " + cloudName + "-cloud.groovy");
		Assert.assertTrue(originalDslFileContents.contains("ENTER_TENANT"), "Missing ENTER_TENANT statement in " + cloudName + "-cloud.groovy");
		Assert.assertTrue(originalDslFileContents.contains("machineNamePrefix"), "Missing machineNamePrefix statement in " + cloudName + "-cloud.groovy");
		Assert.assertTrue(originalDslFileContents.contains("managementGroup"), "Missing managementGroup statement in " + cloudName + "-cloud.groovy");
		

		// first make a backup of the original file
		FileUtils.copyFile(originalCloudDslFile, backupCloudDslFile);

		String modifiedDslFileContents = originalDslFileContents.replace("ENTER_USER", user).replace("ENTER_API_KEY", apiKey).
			replace("cloudify_agent_", CloudTestUtils.SGTEST_MACHINE_PREFIX + "cloudify_agent_").replace("cloudify_manager", CloudTestUtils.SGTEST_MACHINE_PREFIX + "cloudify_manager")
				.replace("ENTER_KEY_FILE", pemFileName + ".pem").replace("ENTER_TENANT", tenant).replace("hp-cloud-demo", "sgtest");

		FileUtils.write(originalCloudDslFile, modifiedDslFileContents);

		// upload dir needs to contain the sshKeyPem 
		File targetPem = new File(ScriptUtils.getBuildPath(), "tools/cli/plugins/esc/" + cloudName + "/upload/" + sshKeyPemName);
		FileUtils.copyFile(new File(cloudTestPath, sshKeyPemName), targetPem);
		AssertUtils.assertTrue("File not found", targetPem.isFile());

		
	}
}
