package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.services.exoscale;

import iTests.framework.utils.IOUtils;
import iTests.framework.utils.LogUtils;
import org.apache.commons.io.FileUtils;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.services.AbstractCloudService;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class ExoscaleCloudService extends AbstractCloudService {

    private static final String EXOSCALE_CERT_PROPERTIES = CREDENTIALS_FOLDER + "/cloud/exoscale/exoscale-cred.properties";
    private final Properties certProperties = getCloudProperties(EXOSCALE_CERT_PROPERTIES);

    public static final String API_KEY_PROP = "apiKey";
    public static final String SECRET_KEY_PROP = "secretKey";
    public static final String KEYPAIR_PROP = "sshKeypairName";
    public static final String KEYFILE_PROP = "sshKeypairFile";
    public static final String PERSISTENT_PATH_PROP = "persistencePath";
    public static final String ZONE_PROP = "zoneId";
    public static final String TEMPLATE_PROP = "tamplateId";
    public static final String CLOUD_STACK_API_ENDPOINT_PROP = "cloudStackAPIEndpoint";
    public static final String COMPUTE_OFFERING_ID_PROP = "computeOfferingId";

    private String apiKey = certProperties.getProperty("apiKey");
    private String secretKey = certProperties.getProperty("secretKey");
    private String persistencePath = certProperties.getProperty("persistencePath");
    private String cloudStackAPIEndpoint = certProperties.getProperty("cloudStackAPIEndpoint");
    private String computeOfferingId = certProperties.getProperty("computeOfferingId");
    private String sshKeypairName = certProperties.getProperty("sshKeypairName");
    private String sshKeypairFile = certProperties.getProperty("sshKeypairFile");
    private String zoneId = certProperties.getProperty("zoneId");
    private String tamplateId = certProperties.getProperty("tamplateId");




	public ExoscaleCloudService() {
		super("exoscale");
        LogUtils.log("credentials file is at: " + EXOSCALE_CERT_PROPERTIES);
	}

    public ExoscaleCloudService(String cloudName) {
        super(cloudName);
        LogUtils.log("credentials file is at: " + EXOSCALE_CERT_PROPERTIES);
    }

    @Override
    public void injectCloudAuthenticationDetails() throws IOException {
        final Map<String, String> propsToReplace = new HashMap<String, String>();

        getProperties().put(API_KEY_PROP, this.apiKey);
        getProperties().put(SECRET_KEY_PROP, this.secretKey);
        getProperties().put(API_KEY_PROP, this.apiKey);
        getProperties().put(KEYPAIR_PROP, this.sshKeypairName);
        getProperties().put(KEYFILE_PROP, this.sshKeypairFile);
        getProperties().put(PERSISTENT_PATH_PROP, this.persistencePath);
        getProperties().put(ZONE_PROP, this.zoneId);
        getProperties().put(TEMPLATE_PROP, this.tamplateId);
        getProperties().put(CLOUD_STACK_API_ENDPOINT_PROP, this.cloudStackAPIEndpoint);
        getProperties().put(COMPUTE_OFFERING_ID_PROP, this.computeOfferingId);
		propsToReplace.put("machineNamePrefix \"cloudify-agent-\"", "machineNamePrefix \"" + getMachinePrefix() + "-cloudify-agent-\"");
		propsToReplace.put("managementGroup \"cloudify-manager-\"", "managementGroup \"" + getMachinePrefix() + "-cloudify-manager-\"");
        propsToReplace.put("numberOfManagementMachines 1", "numberOfManagementMachines " + getNumberOfManagementMachines());
        propsToReplace.put("javaUrl", "// javaUrl");

        String pathToCloudGroovy = getPathToCloudGroovy();
        IOUtils.replaceTextInFile(pathToCloudGroovy, propsToReplace);

        // Copy pem file
        final File fileToCopy = new File(CREDENTIALS_FOLDER + "/cloud/" + getCloudName() + "/" + sshKeypairFile);
        final File targetLocation = new File(getPathToCloudFolder() + "/upload/");
        FileUtils.copyFileToDirectory(fileToCopy, targetLocation);
    }

    @Override
    public String getUser() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getApiKey() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getRegion() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

	@Override
	public boolean supportsStorageApi() {
		return false;
	}

	@Override
	public boolean supportNetworkApi() {
		return false;
	}

	@Override
	public boolean supportsComputeApi() {
		return false;
	}
}