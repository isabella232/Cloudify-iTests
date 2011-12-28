package test.cli.cloudify;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.gigaspaces.cloudify.dsl.utils.ServiceUtils;

import framework.utils.AssertUtils;
import framework.utils.ScriptUtils;

public class PetClinicApplicationTest extends AbstractLocalCloudTest {

    private static final String PETCLINIC_APPLICTION_NAME = "petclinic-mongo";

    @Override
    @BeforeMethod
    public void beforeTest() {
        super.beforeTest();
        try {
            checkOSSupportForMongo();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void checkOSSupportForMongo() throws IOException, InterruptedException {
        if (!isWindows()) {
            String cmd = "uname -a";
            Runtime run = Runtime.getRuntime();
            Process pr = run.exec(cmd);
            pr.waitFor();
            BufferedReader buf = new BufferedReader(new InputStreamReader(pr.getInputStream()));
            String out = "";
            String line = "";
            while ((line = buf.readLine()) != null) {
                out += line + System.getProperty("line.separator");
            }
            if (out.contains("2007") && out.contains("linux")) {
                AssertUtils.AssertFail("Mongo service is not supported in this version of linux");
            }
        }
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().startsWith("win");
    }

    @Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, groups = "1", enabled = true)
    public void testPetClinincApplication() throws Exception {
        String applicationDir = ScriptUtils.getBuildPath() + "/examples/petclinic";
        String command = "connect " + this.restUrl + ";" + "install-application " + "--verbose -timeout 25 " + applicationDir;

        CommandTestUtils.runCommandAndWait(command);
        int currentNumberOfInstances;
        currentNumberOfInstances = getProcessingUnitInstanceCount(ServiceUtils.getAbsolutePUName(PETCLINIC_APPLICTION_NAME, "mongod"));
        assertTrue("Expected 2 PU instances. Actual number of instances is " + currentNumberOfInstances, currentNumberOfInstances == 2);

        currentNumberOfInstances = getProcessingUnitInstanceCount(ServiceUtils.getAbsolutePUName(PETCLINIC_APPLICTION_NAME, "mongo-cfg"));
        assertTrue("Expected 1 PU instances. Actual number of instances is " + currentNumberOfInstances, currentNumberOfInstances == 1);

        currentNumberOfInstances = getProcessingUnitInstanceCount(ServiceUtils.getAbsolutePUName(PETCLINIC_APPLICTION_NAME, "mongos"));
        assertTrue("Expected 1 PU instances. Actual number of instances is " + currentNumberOfInstances, currentNumberOfInstances == 1);

        currentNumberOfInstances = getProcessingUnitInstanceCount(ServiceUtils.getAbsolutePUName(PETCLINIC_APPLICTION_NAME, "tomcat"));
        assertTrue("Expected 1 PU instances. Actual number of instances is " + currentNumberOfInstances, currentNumberOfInstances == 1);
    }
}
