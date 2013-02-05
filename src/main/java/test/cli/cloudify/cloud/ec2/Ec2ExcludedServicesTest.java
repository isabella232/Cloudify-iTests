package test.cli.cloudify.cloud.ec2;

import com.j_spaces.kernel.PlatformVersion;
import framework.utils.AssertUtils;
import framework.utils.ScriptUtils;
import org.apache.commons.io.FileUtils;
import org.cloudifysource.restclient.GSRestClient;
import org.cloudifysource.restclient.RestException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepository;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import test.cli.cloudify.cloud.AbstractServicesTest;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Map;


public class Ec2ExcludedServicesTest extends AbstractServicesTest {

    private static String localPath, remotePath;
    private static Repository localRepo;
    private static Git git;
    private File excludedRecipesDir;
    private static final String STATUS_PROPERTY = "DeclaringClass-Enumerator";
    
    @Override
    protected String getCloudName() {
        return "ec2";
    }

    @BeforeClass(alwaysRun = true)
    protected void bootstrap() throws Exception {
        localPath = ScriptUtils.getBuildPath() + "/excludedRecipes";
        remotePath = "https://github.com/CloudifySource/cloudify-recipes.git";
        localRepo = new FileRepository(localPath + "/.git");
        git = new Git(localRepo);
        excludedRecipesDir = new File(localPath + "/services");
        Git.cloneRepository()
                .setURI(remotePath)
                .setDirectory(new File(localPath))
                .call();
        super.bootstrap();
    }

    @AfterClass(alwaysRun = true)
    protected void teardown() throws Exception {
        FileUtils.deleteDirectory(new File(localPath));
        super.teardown();
    }

    @Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, enabled = true)
    public void testApache() throws Exception {
        testService("apache", "apache");
    }

    @Override
    public void testService(String serviceFolderName, String serviceName) throws IOException, InterruptedException, RestException {
        installServiceAndWait(excludedRecipesDir.getAbsolutePath() + " /" + serviceName, serviceName);

        String restUrl = getRestUrl();
        GSRestClient client = new GSRestClient("", "", new URL(restUrl), PlatformVersion.getVersionNumber());
        Map<String, Object> entriesJsonMap  = client.getAdminData("ProcessingUnits/Names/default." + serviceName + "/Status");
        String serviceStatus = (String)entriesJsonMap.get(STATUS_PROPERTY);

        AssertUtils.assertTrue("service is not intact", serviceStatus.equalsIgnoreCase("INTACT"));

        uninstallServiceAndWait(serviceName);
    }

}

