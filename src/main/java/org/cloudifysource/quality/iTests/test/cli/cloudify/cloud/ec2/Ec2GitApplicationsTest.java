package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.ec2;

import java.io.File;
import java.io.IOException;

import org.cloudifysource.quality.iTests.framework.utils.ScriptUtils;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.AbstractExamplesTest;
import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.lib.Ref;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class Ec2GitApplicationsTest extends AbstractExamplesTest {

    private static String localGitRepoPath;
    private static String BRANCH_NAME = System.getProperty("branch.name");

    @Override
    protected String getCloudName() {
        return "ec2";
    }

    @BeforeClass(alwaysRun = true)
    protected void bootstrap() throws Exception {
        super.bootstrap();

        localGitRepoPath = ScriptUtils.getBuildPath() + "/git-recipes-" + this.getClass().getSimpleName();

        if (!new File(localGitRepoPath).exists()) {
            String remotePath = "https://github.com/CloudifySource/cloudify-recipes.git";
            Git.cloneRepository()
                    .setURI(remotePath)
                    .setDirectory(new File(localGitRepoPath))
                    .call();
            if (!BRANCH_NAME.equalsIgnoreCase("master")) {
                    Git git = Git.open(new File(localGitRepoPath));
                    CheckoutCommand checkout = git.checkout();
                    checkout.setCreateBranch(true)
                            .setName(BRANCH_NAME)
                            .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK).
                            setStartPoint("origin/" + BRANCH_NAME)
                            .call();
            }
        }
    }

    //should work
    @Test(timeOut = DEFAULT_TEST_TIMEOUT * 4, enabled = true)
    public void testComputers() throws Exception {
        super.testComputers(localGitRepoPath + "/apps");
    }

    //should work
    @Test(timeOut = DEFAULT_TEST_TIMEOUT * 4, enabled = true)
    public void testBabies() throws Exception {
        super.testBabies(localGitRepoPath + "/apps");
    }

    //fails
    @Test(timeOut = DEFAULT_TEST_TIMEOUT * 4, enabled = false)
    public void testBiginsights() throws Exception {
        super.testBiginsights(localGitRepoPath + "/apps");
    }

    //should work
    @Test(timeOut = DEFAULT_TEST_TIMEOUT * 4, enabled = true)
    public void testPetclinicJboss() throws Exception {
        super.testPetclinicJboss(localGitRepoPath + "/apps");
    }

    @Test(timeOut = DEFAULT_TEST_TIMEOUT * 4, enabled = true)
    public void testLamp() throws Exception {
        super.testLamp(localGitRepoPath + "/apps");
    }

    @Test(timeOut = DEFAULT_TEST_TIMEOUT * 4, enabled = true)
    public void testMasterSlave() throws Exception {
        super.testMasterSlave(localGitRepoPath + "/apps");
    }

    //needs configuration to work.
    @Test(timeOut = DEFAULT_TEST_TIMEOUT * 4, enabled = false)
    public void testPetclinicWas() throws Exception {
        super.testPetclinicWas(localGitRepoPath + "/apps");
    }

    // requires a non existing template
    @Test(timeOut = DEFAULT_TEST_TIMEOUT * 4, enabled = false)
    public void testStorm() throws Exception {
        super.testStorm(localGitRepoPath + "/apps");
    }

    //should work
    @Test(timeOut = DEFAULT_TEST_TIMEOUT * 4, enabled = true)
    public void testTravelLb() throws Exception {
        super.testTravelLb(localGitRepoPath + "/apps");
    }

    @Test(timeOut = DEFAULT_TEST_TIMEOUT * 4, enabled = true)
    public void testPuppet() throws Exception {
        super.testPuppet(localGitRepoPath + "/apps");
    }

    @Test(timeOut = DEFAULT_TEST_TIMEOUT * 4, enabled = true)
    public void testMysqlJboss() throws Exception {
        super.testMysqlJboss(localGitRepoPath + "/apps");
    }

    @AfterClass(alwaysRun = true)
    protected void teardown() throws Exception {
        super.teardown();
    }


    public static void main(String[] args) throws GitAPIException, IOException {
        localGitRepoPath = ScriptUtils.getBuildPath() + "/git-recipes-" + "Ec2GitApplicationsTest";
        BRANCH_NAME = "2_5_1";
//        if (!new File(localGitRepoPath).exists()) {
//            String remotePath = "https://github.com/CloudifySource/cloudify-recipes.git";

        if (BRANCH_NAME.equalsIgnoreCase("master")) {
            Git.cloneRepository()
//                        .setURI(remotePath)
                    .setDirectory(new File(localGitRepoPath))
                    .call();
        } else {
//                Git.cloneRepository()
//                        .setURI(remotePath)
//                        .setDirectory(new File(localGitRepoPath))
//                        .call();
            Git git = Git.open(new File(localGitRepoPath));
            System.out.println(git.getRepository().getFullBranch());
            CheckoutCommand checkout = git.checkout();
            checkout.setCreateBranch(true)
                    .setName(BRANCH_NAME)
                    .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK).
                    setStartPoint("origin/" + BRANCH_NAME)
                    .call();
            System.out.println(git.getRepository().getFullBranch());
        }
    }
//    }
}
