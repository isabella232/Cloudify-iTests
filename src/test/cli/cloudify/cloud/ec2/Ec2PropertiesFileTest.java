package test.cli.cloudify.cloud.ec2;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.testng.ITestContext;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import test.cli.cloudify.CloudTestUtils;
import test.cli.cloudify.cloud.NewAbstractCloudTest;
import test.cli.cloudify.cloud.services.ec2.Ec2CloudService;
import framework.tools.SGTestHelper;
import framework.utils.ScriptUtils;

public class Ec2PropertiesFileTest extends NewAbstractCloudTest{
	final String tomcatServicePath = ScriptUtils.getBuildPath() + "/recipes/services/tomcat" ;

	@Override
	protected String getCloudName() {
		return "ec2";
	}
	
	@BeforeClass(alwaysRun = true)
	protected void bootstrap(final ITestContext testContext) {
		super.bootstrap(testContext);
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 4, enabled = true)
	public void testInstallationWithPropsFile() throws Exception {		
		installServiceAndWait(tomcatServicePath, "tomcat");
	}

	@Override
	protected void customizeCloud() throws Exception {
		//create the properties file.
		Map<File, File> filesToReplace = new HashMap<File, File>();
		
		String pathToCloudFolder = ((Ec2CloudService)cloud).getPathToCloudFolder();
		String sgTestRootDir = SGTestHelper.getSGTestRootDir() ;
		File propertiesFile = new File(sgTestRootDir + "/apps/cloudify/cloud/ec2/", "ec2-cloud.properties");
		File mockFile = new File(pathToCloudFolder, "ec2-cloud.properties");
		filesToReplace.put(mockFile, propertiesFile);
		((Ec2CloudService)cloud).addFilesToReplace(filesToReplace);
		
		cloud.setMachinePrefix(this.getClass().getName() + CloudTestUtils.SGTEST_MACHINE_PREFIX);
		
	}

	@Override
	protected boolean isReusableCloud() {
		return false;
	}

}
