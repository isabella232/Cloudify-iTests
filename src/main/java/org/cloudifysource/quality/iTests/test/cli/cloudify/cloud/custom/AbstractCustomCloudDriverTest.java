package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.custom;

import java.io.File;
import java.io.IOException;

import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.NewAbstractCloudTest;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

/**
 * 
 * @author adaml
 *
 */
public abstract class AbstractCustomCloudDriverTest extends NewAbstractCloudTest {
	
	final private static String SIMPLE_SERVICE_PATH = "src/main/resources/apps/USM/usm/simple";
	final private static String SIMPLE_SERVICE_NAME = "simple";

	@Override
	protected String getCloudName() {
		return "ec2";
	}
	
	@BeforeClass(alwaysRun = true)
	protected void bootstrap() throws Exception {
		super.bootstrap();
	}
	
	@Override
	protected boolean isReusableCloud() {
		return false;
	}
	

	public void installService() throws IOException, InterruptedException {
		installServiceAndWait(SIMPLE_SERVICE_PATH, SIMPLE_SERVICE_NAME);
	}
	
	@AfterClass
	public void teardown() throws Exception {		
		super.teardown();					
	}
	
	@Override
	protected void customizeCloud() throws Exception {
        super.customizeCloud();
        final String cloudFolder = getService().getPathToCloudFolder();
        File libFolder = new File(cloudFolder, "lib");
        libFolder.mkdir();
	}
}
