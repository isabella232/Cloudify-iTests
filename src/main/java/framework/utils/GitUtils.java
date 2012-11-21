package framework.utils;

import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import java.io.File;

public class GitUtils {

    public static void pull(String repositoryUrl, File targetFolder) {
        try {
            FileRepositoryBuilder builder = new FileRepositoryBuilder();
            Repository repository = null;
            repository = builder.setGitDir(targetFolder).readEnvironment().findGitDir().build();


            Git git = new Git(repository);
            CloneCommand clone = git.cloneRepository();
            clone.setDirectory(targetFolder).setURI(repositoryUrl);
            clone.call();
        } catch (Exception e) {
            throw new RuntimeException("pull from GitHub failed", e);
        }
    }
}
