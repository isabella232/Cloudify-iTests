package framework.utils;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.openspaces.jee.sessions.jetty.SessionData;

import framework.utils.AssertUtils.RepetitiveConditionProvider;

public class WebUtils {

	/***
	 *  Used to get the session attributeMap which is protected
	 */
	public static ConcurrentHashMap<String, Object> getSessionDataAttributeMap(
			SessionData data) throws Exception {

		Class clazz = data.getClass();
		Class[] paramTypes = new Class[] {};
		Method m = clazz.getDeclaredMethod("getAttributeMap", paramTypes);
		m.setAccessible(true);
		ConcurrentHashMap<String, Object> result = 
				(ConcurrentHashMap<String, Object>) m.invoke(data, new Object[] {});

		return result;
	}

	/***
	 * used to get the message from an object using reflection
	 */
	public static String getMessage(Object message) throws Exception {
		Class clazz = message.getClass();
		Class[] paramTypes = new Class[] {};
		Method m = clazz.getDeclaredMethod("getMessage", paramTypes);
		String result = (String) m.invoke(message, new Object[] {});

		return result;
	}


	public static void repetitiveAssertWebUrlAvailable(final String applicationUrl, long timeout, TimeUnit timeUnit) {
		AssertUtils.repetitiveAssertTrue("Cannot access " + applicationUrl, new RepetitiveConditionProvider() {
			
			@Override
			public boolean getCondition() {
				try {
					final HttpClient client = new DefaultHttpClient();
					try {
						final HttpGet get = new HttpGet(new URL(applicationUrl).toURI());
						
							final HttpResponse response = client.execute(get);
							if (response.getStatusLine().getStatusCode() != 200) {
								LogUtils.log("Failed to access " + applicationUrl + " response:"+ response.getStatusLine().getReasonPhrase() + " status code:" + response.getStatusLine().getStatusCode());
							}
							return true;
						
					} finally {
						client.getConnectionManager().shutdown();
					}
				} catch (final Exception e) {
					LogUtils.log("Failed to access " + applicationUrl ,e);
					return false;
				}
			}
		}, timeUnit.toMillis(timeout));
	}

	public static boolean isURLAvailable(URL url) throws Exception{
		HttpClient client = new DefaultHttpClient();
		// Do not use HEAD here! The spring framework we use does not like it. 
		HttpGet get = new HttpGet(url.toURI());
		try {
			HttpResponse response = client.execute(get);
			if (response.getStatusLine().getStatusCode() != 200) {
				return false;
			}
			return true;
		} catch (Exception e) {
			return false;
		} finally {
			client.getConnectionManager().shutdown();
		}
	}

	public static boolean waitForHost(String url, int timeout) throws Exception {
		InetAddress address = InetAddress.getByName(url);
		return address.isReachable(timeout);
	}

	/**
	 * 
	 * @return the content of the given url or an empty if not found
	 * Use #getURLContent(URL) or repetitiveAssertWebUrlAvailable(URL) instead
	 */
	@Deprecated
	public static String getURLContentSwallowExceptions(URL url) throws Exception {
		HttpClient client = new DefaultHttpClient();
		HttpGet get = new HttpGet(url.toURI());
		try {
			return client.execute(get, new BasicResponseHandler());
		} catch (Exception e) {
			return "";
		} finally {
			client.getConnectionManager().shutdown();
		}        
	}

	public static String getURLContent(URL url) throws ClientProtocolException, IOException, URISyntaxException {
		HttpClient client = new DefaultHttpClient();
		HttpGet get = new HttpGet(url.toURI());
		try {
			return client.execute(get, new BasicResponseHandler());
		} 
		finally {
			client.getConnectionManager().shutdown();
		}        
	}
}
