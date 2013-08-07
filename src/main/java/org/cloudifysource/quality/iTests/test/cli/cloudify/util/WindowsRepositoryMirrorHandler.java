package org.cloudifysource.quality.iTests.test.cli.cloudify.util;

import iTests.framework.utils.LogUtils;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.testng.Assert;

public class WindowsRepositoryMirrorHandler implements RepositoryMirrorHandler {

	@Override
	public File getHostsFile() {

		// %systemroot%\system32\drivers\etc\
		File hostsFile;
		final String systemRootEnvVar = System.getenv("systemroot");
		Assert.assertNotNull(systemRootEnvVar, "Missing environment variable on windows: systemroot");
		final File systemRoot = new File(systemRootEnvVar);
		Assert.assertTrue(systemRoot.exists(), "System root directory not found");
		final File system32 = new File(systemRoot, "system32");
		Assert.assertTrue(system32.exists(), "system32 directory is missing");
		final File drivers = new File(system32, "drivers");
		Assert.assertTrue(drivers.exists(), "drivers directory is missing");
		final File etc = new File(drivers, "etc");
		Assert.assertTrue(etc.exists(), "etc directory is missing");
		hostsFile = new File(etc, "hosts");
		Assert.assertTrue(hostsFile.exists(), "hosts file is missing");
		return hostsFile;

	}

	@Override
	public void revertHostsFile(final File hostsFile, final File backupHostsFile, final boolean reverting)
			throws IOException {
		if (backupHostsFile.exists()) {
			if (!reverting) {
				LogUtils.log("Warning - found an old sgtest hosts file backup. This indicates an abnormal termination of a previous suite. hosts file will be reverted to the backup version");
			}
			FileUtils.copyFile(backupHostsFile, hostsFile);
			final boolean deleteResult = FileUtils.deleteQuietly(backupHostsFile);
			if (!deleteResult) {
				throw new IOException("Failed to delete old backup hosts file: " + backupHostsFile);
			}
		}

	}

	@Override
	public void modifyHostsFile(File hostsFile, File modifiedFile, final File backupHostsFile) throws IOException {
		FileUtils.copyFile(hostsFile, backupHostsFile);
		FileUtils.copyFile(modifiedFile, hostsFile);
	}
}
