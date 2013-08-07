package org.cloudifysource.quality.iTests.test.cli.cloudify.util;

import java.io.File;
import java.io.IOException;

public interface RepositoryMirrorHandler {

	File getHostsFile();

	void revertHostsFile(File hostsFile, File backupHostsFile, boolean reverting) throws IOException;

	void modifyHostsFile(File hostsFile, File modifiedFile, File backupHostsFile) throws IOException;

}
