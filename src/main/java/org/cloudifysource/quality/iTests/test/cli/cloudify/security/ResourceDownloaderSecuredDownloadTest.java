package org.cloudifysource.quality.iTests.test.cli.cloudify.security;

import org.apache.commons.io.FileUtils;
import org.cloudifysource.dsl.internal.tools.download.ResourceDownloadException;
import org.cloudifysource.dsl.internal.tools.download.ResourceDownloader;
import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
import org.cloudifysource.quality.iTests.test.cli.cloudify.AbstractSecuredLocalCloudTest;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.TimeoutException;


public class ResourceDownloaderSecuredDownloadTest extends
		AbstractSecuredLocalCloudTest {
	
	private final String USER_NAME = "Dana";
	private final String PASSWORD = "Dana";
	
	@BeforeClass
	public void bootsrap() throws Exception {
		super.bootstrap();
	}
	
	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT, enabled = true)
	public void testSecureDownloadCredsInURLTest() throws IOException, ResourceDownloadException, TimeoutException {
		final ResourceDownloader downloader = new ResourceDownloader();
		final URL url = new URL("https://" + USER_NAME + ":" + PASSWORD + "@localhost:8100/resources/restdoclet/restdoclet.html");
		downloader.setUrl(url);
		downloader.setResourceDest(new File(FileUtils.getTempDirectoryPath(), "temp"));
		downloader.download();
	}
	
	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT, enabled = true)
	public void testSecureDownloadCredsTest() throws IOException, ResourceDownloadException, TimeoutException {
		final ResourceDownloader downloader = new ResourceDownloader();
		final URL url = new URL("https://localhost:8100/resources/restdoclet/restdoclet.html");
		downloader.setUrl(url);
		downloader.setResourceDest(new File(FileUtils.getTempDirectoryPath(), "temp"));
		downloader.setUserName(USER_NAME);
		downloader.setPassword(PASSWORD);
		downloader.download();
	}

    @AfterClass
    public void teardown() throws IOException, InterruptedException {
        super.teardown();
    }

}
