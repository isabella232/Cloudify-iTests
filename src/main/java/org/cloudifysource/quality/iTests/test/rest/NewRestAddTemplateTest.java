package org.cloudifysource.quality.iTests.test.rest;

import org.cloudifysource.quality.iTests.framework.utils.ServiceInstaller;
import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
import org.cloudifysource.quality.iTests.test.cli.cloudify.CommandTestUtils;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.byon.AddTemplateRestFailoverTest;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.templates.SimpleServiceCreator;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.templates.TemplatesFolderHandler;
import org.testng.annotations.Test;

/**
 * User: Sagi Bernstein
 * Date: 24/06/13
 * Time: 15:33
 */
public class NewRestAddTemplateTest extends AddTemplateRestFailoverTest {


    @Override @Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 2, enabled = false)
    public void addTemplateRestFailoverTest() {
    	TemplatesFolderHandler templatesFolder = addTemplate();
        doInstall();
        removeTemplate(templatesFolder);
    }

    private void removeTemplate(TemplatesFolderHandler templatesFolder) {
        try {
        	templatesHandler.removeTemplatesFromCloudUsingRestAPI(templatesFolder, "template_0_0", false, null);
        } catch (Exception e) {
            AssertFail("remove template failed", e);
        }
    }

    private void doInstall() {
        final ServiceInstaller installerWithTemplate = 
        		new ServiceInstaller(getRestUrl(), SimpleServiceCreator.SERVICE_NAME_PROPERTY_NAME);
        installerWithTemplate.recipePath(SimpleServiceCreator.SERVICES_ROOT_PATH + "/../");
        final ServiceInstaller installerWithOutTemplate = new ServiceInstaller(getRestUrl(), "tomcat");
        installerWithOutTemplate.recipePath(CommandTestUtils.getBuildApplicationsPath() + "/tomcat");

        try {
            installerWithTemplate.restInstall();
            installerWithOutTemplate.restInstall();
        } catch (Exception e) {
            AssertFail("failed to install service", e);
        }
    }

    @Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 2, enabled = false)
    public void withFOTest(){
        super.addTemplateRestFailoverTest();
        doInstall();
    }

}
