package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.ec2;

import org.apache.commons.io.FileUtils;
import org.cloudifysource.dsl.internal.DSLException;
import org.cloudifysource.dsl.internal.ServiceReader;
import org.cloudifysource.dsl.internal.packaging.PackagingException;
import org.cloudifysource.quality.iTests.framework.utils.IOUtils;
import org.cloudifysource.quality.iTests.framework.utils.ScriptUtils;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * Base class for storage tests that use just one service.
 *
 * Provides functionality of :
 * 1. Linux testing (deployed on management machine to save time)
 * 2. Ubuntu testing (deployed on a dedicated instance)
 *
 * Important
 * =========
 *
 * The service name and recipe file prefix must match. <br></br>
 * Example : for a service called groovy, a groovy-service.groovy file must exist in the service directory.<br></br>
 *
 * This class uninstalls the service automatically after the test is over if it was not uninstalled during the test.<br></br>
 *
 * This class copies the desired recipe to the recipes/services folder in the distribution. so that installars can install using just the service name.<br></br>
 *
 * The desired recipe folder is determined by the two abstract methods tests are required to implement <br></br>
 *  1. getServiceFolder() <br></br>
 *  2. getPathToServicesFolder() <br></br>
 *
 * The actual test logic should be implemented in the doTest() method. <br></br>
 *
 * For further information please see usage examples using the hierarchy view. <br></br>
 *
 * Created with IntelliJ IDEA.
 * User: elip
 * Date: 4/10/13
 * Time: 12:39 PM
 */
public abstract class AbstractEc2OneServiceStorageAllocationTest extends AbstractEc2StorageAllocationTest {

    private String serviceName;

    public abstract String getServiceFolder();
    public abstract String getPathToServicesFolder();
    public abstract void doTest() throws Exception ;

    public String getServiceName() {
        return serviceName;
    }

    @BeforeMethod
    public void prepareService() throws IOException, DSLException, PackagingException {
        String buildRecipesServicesPath = ScriptUtils.getBuildRecipesServicesPath();
        File serviceFolder = new File(buildRecipesServicesPath, getServiceFolder());
        serviceName = ServiceReader.readService(serviceFolder).getName();
        if (serviceFolder.exists()) {
            FileUtils.deleteDirectory(serviceFolder);
        }
        FileUtils.copyDirectoryToDirectory( new File(getPathToServicesFolder() + "/" + getServiceFolder()), new File(buildRecipesServicesPath));
    }

    protected void setTemplate(final String computeTemplateName, boolean useManagement) throws Exception {
        File serviceFile = new File(ScriptUtils.getBuildRecipesServicesPath() + "/" + getServiceFolder(), serviceName + "-service.groovy");
        Map<String, String> props = new HashMap<String,String>();
        props.put("ENTER_TEMPLATE", computeTemplateName);
        if (!useManagement) {
            props.put("useManagement true", "useManagement false");
        }
        IOUtils.replaceTextInFile(serviceFile.getAbsolutePath(), props);
    }

    public void testUbuntu() throws Exception {
        setTemplate("SMALL_UBUNTU", false);
        doTest();

    }

    public void testLinux(final boolean useManagement) throws Exception {
        setTemplate("SMALL_LINUX", useManagement);
        doTest();
    }

    public void testLinux() throws Exception {
        testLinux(true);
    }

    @AfterMethod
    public void deleteService() throws IOException {
        FileUtils.deleteDirectory(new File(ScriptUtils.getBuildRecipesServicesPath() + "/" + getServiceFolder()));
    }

    @AfterMethod
    public void uninstallServiceIfFound() throws IOException, InterruptedException {
        uninstallServiceIfFound(serviceName);
    }
}
