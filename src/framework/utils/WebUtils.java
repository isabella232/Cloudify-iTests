package framework.utils;

import java.lang.reflect.Method;
import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.openspaces.jee.sessions.jetty.SessionData;

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
    
    public static boolean isURLAvailable(URL url) throws Exception {
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

    /**
     * @param url
     * @return the content of the given url or an empty if not found
     * @throws Exception
     */
    public static String getURLContent(URL url) throws Exception {
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
    
}
