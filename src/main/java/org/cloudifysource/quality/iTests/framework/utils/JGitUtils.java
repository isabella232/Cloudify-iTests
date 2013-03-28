package org.cloudifysource.quality.iTests.framework.utils;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;

import java.io.File;

public class JGitUtils {

    public static void clone(String localGitRepoPath, String repositoryUrl) {
            if (!new File(localGitRepoPath).exists()) {
                try {
                    Git.cloneRepository()
                            .setURI(repositoryUrl)
                            .setDirectory(new File(localGitRepoPath))
                            .call();
                } catch (Exception e) {
                    throw new RuntimeException("Failed to clone "+repositoryUrl, e);
                }
            }
    }

    public static void commit(Repository localRepo, String msg) {
        Git git = null;
        try{
            git = new Git(localRepo);
            git.commit()
                    .setMessage(msg)
                    .call();
        }catch (Exception e){
            throw new RuntimeException("Failed to commit with messgae " + msg + " to local repository " + localRepo, e);
        }finally {
            git = null;
        }
    }

    public static void push(Repository localRepo) {
        Git git = null;
        try{
            git = new Git(localRepo);
            git.push().call();
        }catch (Exception e){
            throw new RuntimeException("Failed to push to local repository " + localRepo, e);
        }finally {
            git = null;
        }
    }

    public static void pull(Repository localRepo) {
        Git git = null;
        try{
            git = new Git(localRepo);
            git.pull().call();
        }catch (Exception e){
            throw new RuntimeException("Failed to pull to local repository " + localRepo, e);
        }finally {
            git = null;
        }
    }

}
