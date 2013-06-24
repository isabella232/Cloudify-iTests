package org.cloudifysource.quality.iTests.test.rest;

import org.cloudifysource.quality.iTests.framework.utils.ServiceInstaller;
import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
import org.cloudifysource.quality.iTests.test.cli.cloudify.CommandTestUtils;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.byon.AddTemplateRestFailoverTest;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * User: Sagi Bernstein
 * Date: 24/06/13
 * Time: 15:33
 */
public class NewRestAddTemplateTest extends AddTemplateRestFailoverTest {


    @Override @Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 2, enabled = false)
    public void addTemplateRestFailoverTest() {
        addTemplate();
        doInstall();
        removeTemplate();
    }

    private void removeTemplate() {
        List<String> templates = new ArrayList<String>();
        templates.add("template_0_0");

        try {
            removeAllAddedTemplates(templates);
        } catch (Exception e) {
            AssertFail("remove template failed", e);
        }
    }

    private void doInstall() {
        final ServiceInstaller installerWithTemplate = new ServiceInstaller(getRestUrl(), SERVICE_NAME_PROPERTY_NAME);
        installerWithTemplate.recipePath(SERVICES_ROOT_PATH + "/../");
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
