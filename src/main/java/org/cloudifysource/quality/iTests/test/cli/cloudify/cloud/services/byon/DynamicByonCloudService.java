package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.services.byon;

import java.io.File;
import java.io.IOException;

import iTests.framework.tools.SGTestHelper;
import org.cloudifysource.quality.iTests.framework.utils.IOUtils;

public class DynamicByonCloudService extends ByonCloudService {

	
	public DynamicByonCloudService() {
		super("dynamic-byon");
	}

	@Override
	public String getCloudName() {
		return "dynamic-byon";
	}
	
	@Override
	public void injectCloudAuthenticationDetails() throws IOException {
		super.injectCloudAuthenticationDetails();
		
		File dynamicByonGroovy = new File(SGTestHelper.getSGTestRootDir()
				+ "/src/main/resources/apps/cloudify/cloud/dynamic-byon/dynamic-byon-cloud.groovy");
		
		File groovyFileToBeReplaced = new File(getPathToCloudFolder(), "dynamic-byon-cloud.groovy");
		IOUtils.replaceFile(groovyFileToBeReplaced, dynamicByonGroovy);
		
		File dynamicByonProperties = new File(SGTestHelper.getSGTestRootDir()
				+ "/src/main/resources/apps/cloudify/cloud/dynamic-byon/dynamic-byon-cloud.properties");
		
		File propertiesFileToBeReplaced = new File(getPathToCloudFolder(), "dynamic-byon-cloud.properties");
		IOUtils.replaceFile(propertiesFileToBeReplaced, dynamicByonProperties);
	}
}
