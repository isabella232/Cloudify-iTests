package test.cli.cloudify.github;

import framework.tools.SGTestHelper;
import framework.utils.GitUtils;
import framework.utils.LogUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;
import test.cli.cloudify.AbstractLocalCloudTest;
import test.cli.cloudify.CommandTestUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


public class GitRestDataBuildTest extends AbstractLocalCloudTest {
    private File gitDir = null;

    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
    public void test() throws IOException, InterruptedException {
        String commandOutput = null;
        String url = "https://github.com/OpenSpaces/RESTData.git";
        gitDir = new File(SGTestHelper.getBuildDir() + "/git/");
        GitUtils.pull(url, gitDir);

        String restDataFolder = SGTestHelper.getBuildDir() + "/git/";

        LogUtils.log("building Rest Data...");
        commandOutput = CommandTestUtils.runLocalCommand("mvn -f " + restDataFolder + "pom.xml package -Dmaven.test.skip", true, false);
        Assert.assertFalse(commandOutput.contains("BUILD FAILED"));
    }

    @AfterMethod
    public void afterTest() {
        try {
            FileUtils.forceDelete(gitDir);
        } catch (IOException e) {
            LogUtils.log("Failed to delete git git folder", e);
        }
        super.afterTest();
    }

}
