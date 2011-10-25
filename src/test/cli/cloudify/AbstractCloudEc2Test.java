package test.cli.cloudify;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import framework.utils.LogUtils;
import framework.utils.ScriptUtils;

import test.AbstractTest;

public abstract class AbstractCloudEc2Test extends AbstractTest {
    
	protected static final String WEBUI_PORT = String.valueOf(8099); 
	protected static final String REST_PORT = String.valueOf(8100); 
	private static final String IP_REGEX= "\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}"; 
	private static final String WEBUI_URL_REGEX= "Webui service is available at: (http://" + IP_REGEX + ":" + WEBUI_PORT +")";
	private static final String REST_URL_REGEX= "Rest service is available at: (http://" + IP_REGEX + ":" + REST_PORT + ")";
	protected static final int NUM_OF_MANAGEMENT_MACHINES = 2;
	protected static final String INSTALL_SIMPLE_EXPECTED_OUTPUT = "Application simple installed successfully";
	protected static final String UNINSTALL_SIMPLE_EXPECTED_OUTPUT = "Application simple uninstalled successfully";
	
    protected URL[] restAdminUrl = new URL[NUM_OF_MANAGEMENT_MACHINES];
    protected URL[] webUIUrl = new URL[NUM_OF_MANAGEMENT_MACHINES];
    
    private File simpleExampleCloudFile;
    
	@Override
    @BeforeMethod
    public void beforeTest() {
        boolean success = false;
		try {
        	bootstrapCloud();
        	success = true;
		} 
		catch (IOException e) {
			LogUtils.log("bootstrap-cloud failed.", e);
		} 
		catch (InterruptedException e) {
			LogUtils.log("bootstrap-cloud failed.", e);
		} 
		finally {
        	if (!success) {
        		teardownCloud();
        		Assert.fail("bootstrap-cloud failed.");
        	}
        }
    }
    
	private void bootstrapCloud() throws IOException, InterruptedException {
		
		String ec2TestPath = "./apps/cloudify/cloud/";
		String sshKeyPemFile = "cloud-demo.pem";
		String ec2DslFile = "ec2-cloud.groovy";
		String serviceEc2DslFile = "service-cloud.groovy";
		
		// ec2 plugin should include recipe that includes secret key 
		File ec2PluginDir = new File(ScriptUtils.getBuildPath() , "tools/cli/plugins/esc/ec2/");
		FileUtils.copyFile(new File(ec2TestPath ,ec2DslFile), new File(ec2PluginDir, ec2DslFile));

		// each cloudify service needs its own copy of cloud recipe
		simpleExampleCloudFile = new File(ScriptUtils.getBuildPath() , "examples/simple/simple/" + ec2DslFile);
		FileUtils.copyFile(new File(ec2TestPath , ec2DslFile), simpleExampleCloudFile);
		
		// upload dir needs to contain the sshKeyPem 
		FileUtils.copyFile(new File(ec2TestPath, sshKeyPemFile), new File(ScriptUtils.getBuildPath(), "tools/cli/plugins/esc/ec2/upload/" + sshKeyPemFile));
		
		// TODO fix uploadCloudifyInstallationToS3. currently we simply assume that gigaspaces.zip is located as described in the dsl
        // upload gigaspaces.zip to s3 (to the locations defined in the cloud dsl)
//            LogUtils.log("Uploading cloudify to S3. This might take a while");
//            uploadCloudifyInstallationToS3(ec2PluginDir);
		
		String output = CommandTestUtils.runCommandAndWait("bootstrap-cloud --verbose ec2");

		Pattern webUIPattern = Pattern.compile(WEBUI_URL_REGEX);
		Pattern restPattern = Pattern.compile(REST_URL_REGEX);
		
		Matcher webUIMatcher = webUIPattern.matcher(output);
		Matcher restMatcher = restPattern.matcher(output);
		
		// This is sort of hack.. currently we are outputing this over ssh and locally with different results
		
		assertTrue("Could not find remote (internal) webui url", webUIMatcher.find()); 
		assertTrue("Could not find remote (internal) rest url", restMatcher.find());

		
		for (int i = 0; i < NUM_OF_MANAGEMENT_MACHINES ; i++) {
			assertTrue("Could not find actual webui url", webUIMatcher.find());
			assertTrue("Could not find actual rest url", restMatcher.find());

			String rawWebUIUrl = webUIMatcher.group(1);
			String rawRestAdminUrl = restMatcher.group(1);
			
			webUIUrl[i] = new URL(rawWebUIUrl);
			restAdminUrl[i] = new URL(rawRestAdminUrl);
		}
	}


	@Override
    @AfterMethod
    public void afterTest() {
        teardownCloud();
    }

	private void deleteSimpleExampleCloudFile() {
		if (simpleExampleCloudFile != null) {
			try {
				FileUtils.forceDelete(simpleExampleCloudFile);
			} catch (FileNotFoundException e) {
				//ignore
			} catch (IOException e) {
				LogUtils.log("Failed to delete " + simpleExampleCloudFile + " delete file manually");
			}
		}
	}

	private void teardownCloud() {
		
		try {
			CommandTestUtils.runCommandAndWait("teardown-cloud --verbose ec2");
		} catch (IOException e) {
			Assert.fail("teardown-cloud failed. SHUTDOWN VIRTUAL MACHINES MANUALLY !!!",e);
		} catch (InterruptedException e) {
			Assert.fail("teardown-cloud failed. SHUTDOWN VIRTUAL MACHINES MANUALLY !!!",e);
		}
		finally {
			deleteSimpleExampleCloudFile();
		}
	}
	
//	private static void uploadCloudifyInstallationToS3(File cloudDslDir) throws IOException {
//	    
//	    Cloud cloudDsl = ServiceReader.getCloudFromDirectory(cloudDslDir);
//	    File cloudifyInstallation = ScriptUtils.getGigaspacesZipFile();
//	    
//	    BlobStoreContext context = null;
//	    try {
//    	    BlobStoreContextFactory contextFactory = new BlobStoreContextFactory();
//    	    context = contextFactory.createContext("aws-s3", cloudDsl.getUser(), cloudDsl.getApiKey());
//
//    	    S3Client client = S3Client.class.cast(context.getProviderSpecificContext().getApi());
//    	    
//    	    S3Object object = client.newS3Object();
//    	    object.setPayload(cloudifyInstallation);
//    	    object.getMetadata().setKey("cloudify/gigaspaces.zip");
//    	    
//    	    client.putObject("test-repository-ec2dev", object, 
//    	            PutObjectOptions.Builder.withAcl(CannedAccessPolicy.PUBLIC_READ));
//	    } catch (Exception e) {
//    	    e.printStackTrace();
//	    } finally {
//	        if (context != null) {
//    	        context.close();
//	        }
//	    }
//	}
	
}
