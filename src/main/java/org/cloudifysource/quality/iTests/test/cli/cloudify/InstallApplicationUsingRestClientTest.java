package org.cloudifysource.quality.iTests.test.cli.cloudify;

import iTests.framework.tools.SGTestHelper;
import iTests.framework.utils.AssertUtils;
import iTests.framework.utils.LogUtils;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.cloudifysource.dsl.Application;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.CloudifyConstants.DeploymentState;
import org.cloudifysource.dsl.internal.DSLException;
import org.cloudifysource.dsl.internal.DSLReader;
import org.cloudifysource.dsl.internal.DSLUtils;
import org.cloudifysource.dsl.internal.packaging.Packager;
import org.cloudifysource.dsl.internal.packaging.PackagingException;
import org.cloudifysource.dsl.rest.request.InstallApplicationRequest;
import org.cloudifysource.dsl.rest.response.ApplicationDescription;
import org.cloudifysource.dsl.rest.response.UninstallApplicationResponse;
import org.cloudifysource.dsl.rest.response.UploadResponse;
import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
import org.cloudifysource.restclient.RestClient;
import org.cloudifysource.restclient.exceptions.RestClientException;
import org.cloudifysource.shell.commands.CLIException;
import org.cloudifysource.shell.rest.RestAdminFacade;
import org.junit.Assert;
import org.junit.Ignore;
import org.testng.annotations.Test;

import com.j_spaces.kernel.PlatformVersion;

/**
 * unstall application on localcloud using the rest API
 *
 * @author adaml
 *
 */
public class InstallApplicationUsingRestClientTest extends AbstractLocalCloudTest {
	private static final String APPLICATION_NAME = "groovyApp";
	private static final String APPLICATION_NAME2 = "simple";
	private final String APPLICATION_FOLDER_PATH = SGTestHelper.getSGTestRootDir() +
						"/src/main/resources/apps/USM/usm/applications/groovyApp";
	private static final int INSTALL_TIMEOUT_IN_MINUTES = 15;
	private static final int INSTALL_TIMEOUT_MILLIS = INSTALL_TIMEOUT_IN_MINUTES * 60 * 1000;

	@Ignore
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, groups = "1")
    public void testApplicationInstall()
    		throws IOException, PackagingException,
    		DSLException, RestClientException, CLIException {
		installAndUninstallApplication(APPLICATION_FOLDER_PATH, APPLICATION_NAME);

	}

	@Ignore
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, groups = "1")
    public void testApplicationInstallWithModifiedName()
    		throws IOException, PackagingException,
    		DSLException, RestClientException, CLIException {
		installAndUninstallApplication(APPLICATION_FOLDER_PATH, APPLICATION_NAME2);

	}

	private void installAndUninstallApplication(String path, String applicationName) throws MalformedURLException, RestClientException, DSLException,
			IOException, PackagingException, CLIException {
		final String version = PlatformVersion.getVersion();
		final URL url = new URL(restUrl);
		final RestClient client = new RestClient(url, "", "", version);
		client.connect();

		final File appFolder = new File(path);
		final DSLReader dslReader = createDslReader(appFolder);
		final Application application = dslReader.readDslEntity(Application.class);
		final File packedFile = Packager.packApplication(application, appFolder);
		final UploadResponse uploadResponse = client.upload(packedFile.getName(), packedFile);
		final String uploadKey = uploadResponse.getUploadKey();

		InstallApplicationRequest request = new InstallApplicationRequest();
		request.setApplcationFileUploadKey(uploadKey);

		//Test will run in unsecured mode.
		request.setAuthGroups("");
		//no debugging.
		request.setDebugAll(false);
		request.setSelfHealing(true);
		request.setApplicationName(applicationName);
		//set timeout
		request.setTimeoutInMillis(INSTALL_TIMEOUT_MILLIS);

		//make install service API call
		client.installApplication(APPLICATION_NAME, request);
		//wait for the application to reach STARTED state.
		waitForApplicationInstall();

		//make un-install service API call
		UninstallApplicationResponse response = client.uninstallApplication(APPLICATION_NAME, INSTALL_TIMEOUT_IN_MINUTES);
		Assert.assertNotNull(response);
		Assert.assertTrue("Expected non black operation ID", StringUtils.isNotBlank(response.getDeploymentID()));

		System.out.println(response);
		//wait for the application to be removed.
		waitForApplicationUninstall();
	}

	void waitForApplicationInstall()
			throws CLIException, RestClientException {
		final RestAdminFacade adminFacade = new RestAdminFacade();
		adminFacade.connect(null, null, restUrl.toString(), false);

		LogUtils.log("Waiting for application deployment state to be " + DeploymentState.STARTED) ;
		AssertUtils.repetitiveAssertTrue(APPLICATION_NAME + " application failed to deploy", new AssertUtils.RepetitiveConditionProvider() {
			@Override
			public boolean getCondition() {
				try {
					final List<ApplicationDescription> applicationDescriptionsList = adminFacade.getApplicationDescriptionsList();
					for (ApplicationDescription applicationDescription : applicationDescriptionsList) {
						if (applicationDescription.getApplicationName().equals(APPLICATION_NAME)) {
							if (applicationDescription.getApplicationState().equals(DeploymentState.STARTED)) {
								return true;
							}
						}
					}
				} catch (final CLIException e) {
					LogUtils.log("Failed reading application description : " + e.getMessage());
				}
				return false;
			}
		} , AbstractTestSupport.OPERATION_TIMEOUT * 3);
	}

	void waitForApplicationUninstall()
			throws CLIException, RestClientException {
		final RestAdminFacade adminFacade = new RestAdminFacade();
		adminFacade.connect(null, null, restUrl.toString(), false);

		LogUtils.log("Waiting for USM_State to be " + CloudifyConstants.USMState.RUNNING);
		AssertUtils.repetitiveAssertTrue("uninstall failed for application " + APPLICATION_NAME, new AssertUtils.RepetitiveConditionProvider() {
			@Override
			public boolean getCondition() {
				try {
					final List<ApplicationDescription> applicationDescriptionsList = adminFacade.getApplicationDescriptionsList();
					for (ApplicationDescription applicationDescription : applicationDescriptionsList) {
						if (applicationDescription.getApplicationName().equals(APPLICATION_NAME)) {
							return false;
						}
					}
					return true;
				} catch (final CLIException e) {
					LogUtils.log("Failed getting application list.");
				}
				return false;
			}
		} , AbstractTestSupport.OPERATION_TIMEOUT * 3);
	}

	private DSLReader createDslReader(final File applicationFile) {
		final DSLReader dslReader = new DSLReader();
		final File dslFile = DSLReader.findDefaultDSLFile(DSLUtils.APPLICATION_DSL_FILE_NAME_SUFFIX, applicationFile);
		dslReader.setDslFile(dslFile);
		dslReader.setCreateServiceContext(false);
		dslReader.addProperty(DSLUtils.APPLICATION_DIR, dslFile.getParentFile().getAbsolutePath());
		return dslReader;
	}

}
