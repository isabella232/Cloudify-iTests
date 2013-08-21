package org.cloudifysource.quality.iTests.test.cli.cloudify;

import iTests.framework.utils.LogUtils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.http.client.ClientProtocolException;
import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
import org.cloudifysource.restclient.RestClient;
import org.cloudifysource.restclient.exceptions.RestClientException;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.littleshoot.proxy.DefaultHttpProxyServer;
import org.littleshoot.proxy.HttpProxyServer;
import org.littleshoot.proxy.HttpRequestFilter;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.j_spaces.kernel.PlatformVersion;

/**
 * Tests HTTP proxy related functionality.
 * 
 * @author barakme
 * 
 */
public class HttpProxyTest extends AbstractLocalCloudTest {

	private static final int PROXY_PORT = 18080;
	private static final String HTTP_PROXY_PORT = "http.proxyPort";
	private static final String HTTP_PROXY_HOST = "http.proxyHost";

	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
	public void test() throws RestClientException, ClientProtocolException, IOException {
		final AtomicInteger counter = new AtomicInteger();
		final HttpProxyServer server = new DefaultHttpProxyServer(PROXY_PORT, new HttpRequestFilter() {

			@Override
			public void filter(final HttpRequest arg0) {
				counter.incrementAndGet();

			}
		});
		server.start();
		final String proxyHostBefore = System.getProperty(HTTP_PROXY_HOST);
		final String proxyPortBefore = System.getProperty(HTTP_PROXY_PORT);

		try {
			final String hostAddress = InetAddress.getLocalHost().getHostAddress();
			if (hostAddress.equals("127.0.0.1")) {
				throw new IllegalStateException(
						"The default host address is 127.0.0.1, which can't be used with the default proxy settings");
			}

			final String urlString = AbstractLocalCloudTest.restUrl.replace("127.0.0.1", hostAddress);
			final URL url = new URL(urlString);

			System.setProperty(HTTP_PROXY_HOST, "localhost");
			System.setProperty(HTTP_PROXY_PORT, "" + PROXY_PORT);

			LogUtils.log("Using proxy: " + System.getProperty(HTTP_PROXY_HOST) + ":"
					+ System.getProperty(HTTP_PROXY_PORT));

			final RestClient client = new RestClient(url, "", "", PlatformVersion.getVersion());

			client.connect();

			final int currentValue = counter.get();
			Assert.assertEquals(currentValue, 1);

		} finally {
			if (proxyHostBefore == null) {
				System.clearProperty(HTTP_PROXY_HOST);
			} else {
				System.setProperty(HTTP_PROXY_HOST, proxyHostBefore);
			}

			if (proxyPortBefore == null) {
				System.clearProperty(HTTP_PROXY_PORT);
			} else {
				System.setProperty(HTTP_PROXY_PORT, proxyPortBefore);
			}

		}
	}
}
