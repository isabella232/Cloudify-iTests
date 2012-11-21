package test.cli.cloudify.recipes;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;

import org.junit.Assert;

import framework.utils.TestUtils;


public class RecipeTestUtil {
	
	public static String DEFAULT_RECIPE_TEST_TIMEOUT = "360";
	
	public RecipeTestUtil(){
		
	}

	public static class AsinchronicPortCheck implements Runnable{
		int port;
		

		public AsinchronicPortCheck(int port){
			this.port = port;	
		}
		
		@Override
		public void run() {
			waitForPortToBeTaken();
			waitForPortToBeReleased();
		}
		
		private void waitForPortToBeTaken(){
			TestUtils.repetitive(new Runnable(){
				@Override
				public void run() {
					Assert.assertTrue(!portIsAvailible(port));
					RecipeTest.portTakenBeforTimeout = true;
				}
			}, Integer.parseInt(DEFAULT_RECIPE_TEST_TIMEOUT) * 1000);
		}
		
		private void waitForPortToBeReleased(){
			TestUtils.repetitive(new Runnable(){
				@Override
				public void run() {
					Assert.assertTrue(portIsAvailible(port));
					RecipeTest.portReleasedBeforTimeout = true;
				}
			}, Integer.parseInt(DEFAULT_RECIPE_TEST_TIMEOUT) * 1000);
		}
	}
	
	public static boolean portIsAvailible(int port){
		
	    ServerSocket ss = null;
	    DatagramSocket ds = null;
	    try {
	        ss = new ServerSocket(port);
	        ss.setReuseAddress(true);
	        ds = new DatagramSocket(port);
	        ds.setReuseAddress(true);
	        return true;
	    } catch (IOException e) {
	    } finally {
	        if (ds != null) {
	            ds.close();
	        }
	        if (ss != null) {
	            try {
	                ss.close();
	            } catch (IOException e) {
	                /* should not be thrown */
	            }
	        }
	    }
	    return false;	
	}
}
