package org.cloudifysource.quality.iTests.test.cli.cloudify.util;

import iTests.framework.utils.LogUtils;

import java.io.File;
import java.io.IOException;

import org.cloudifysource.quality.iTests.test.cli.cloudify.CommandTestUtils;
import org.testng.Assert;

public class LinuxRepositoryMirrorHelper implements RepositoryMirrorHandler {

	@Override
	public File getHostsFile() {
		final File hostsFile = new File("/etc/hosts");
		Assert.assertTrue(hostsFile.exists(), "hosts file is missing");
		return hostsFile;
	}

	@Override
	public void modifyHostsFile(File hostsFile, File modifiedFile, File backupHostsFile) throws IOException {
		final File scriptFile = getScriptFileAndSetExecutable("src/main/resources/scripts/modifyHostsFile.sh");
		String output = null;
		final String commandLine =
				scriptFile.getAbsolutePath() + " " + modifiedFile.getAbsolutePath() + " " + hostsFile.getAbsolutePath()
						+ " " + backupHostsFile.getAbsolutePath();
		try {
			output = CommandTestUtils.runLocalCommand(commandLine, true, false);
		} catch (InterruptedException e) {
			throw new IOException("Failed to execute linux modify hosts script: " + output);
		}
	}

	private File getScriptFileAndSetExecutable(final String path) {
		final String fullPath = CommandTestUtils.getPath(path);
		final File scriptFile = new File(fullPath);
		scriptFile.setExecutable(true);
		return scriptFile;
	}

	@Override
	public void revertHostsFile(File hostsFile, File backupHostsFile, boolean reverting) throws IOException {
		if (backupHostsFile.exists()) {
			if (!reverting) {
				LogUtils.log("Warning - found an old sgtest hosts file backup. This indicates an abnormal termination of a previous suite. hosts file will be reverted to the backup version");
			}
			final File scriptFile = getScriptFileAndSetExecutable("src/main/resources/scripts/revertHostsFile.sh");
			String output = null;
			final String commandLine =
					scriptFile.getAbsolutePath() + " " + hostsFile.getAbsolutePath() + " "
							+ backupHostsFile.getAbsolutePath();
			try {
				output = CommandTestUtils.runLocalCommand(commandLine, true, false);
			} catch (InterruptedException e) {
				throw new IOException("Failed to execute linux modify hosts script: " + output);
			}

		}

	}
}
