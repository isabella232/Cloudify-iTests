package test.cli.cloudify.cloud.services.azure;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;

import framework.tools.SGTestHelper;
import framework.utils.IOUtils;

import test.cli.cloudify.cloud.services.AbstractCloudService;

public class MicrosoftAzureCloudService extends AbstractCloudService {

	public MicrosoftAzureCloudService(String uniqueName) {
		super(uniqueName, "azure");
	}
	
	

	@Override
	public void injectServiceAuthenticationDetails() throws IOException {
		File pfxFilePath = new File(SGTestHelper.getSGTestRootDir() + "/apps/cloudify/cloud/azure/azure-cert.pfx"); 	
		File uploadDir = new File(getPathToCloudFolder() + "/upload");
		FileUtils.copyFileToDirectory(pfxFilePath, uploadDir);
	}

	@Override
	public String getUser() {
		return "sgtest";
	}

	@Override
	public String getApiKey() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public void beforeBootstrap() throws Exception {		
		Map<String,String> propToReplace = new HashMap<String, String>();
		propToReplace.put("localDirectory \"tools/cli/plugins/esc/" + getCloudName() + "/upload\"", "localDirectory \""	+ "tools/cli/plugins/esc/" + getServiceFolder() + "/upload\"");
		IOUtils.replaceTextInFile(getPathToCloudGroovy(), propToReplace);
	}

}
