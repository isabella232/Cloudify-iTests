package test.utils;

import java.util.ArrayList;
import java.util.Arrays;

import junit.framework.AssertionFailedError;

/**
 * Allows placing a "catch all" breakpoint before all assert failures   
 * 
 * @author giladh
 *
 */
public class AssertUtils {

	public static boolean skipException() {
		boolean skip = false; 
		return  skip;  // PLACE BREAKPOINT HERE
	}
	
    public interface RepetitiveConditionProvider {
        boolean getCondition();
    }
	
    static public void repetitiveAssertTrue(String message,
            								RepetitiveConditionProvider condition, 
            								long timeoutMilliseconds) {
        long end = System.currentTimeMillis() + timeoutMilliseconds;
        while (System.currentTimeMillis() < end) {
            try {
                _assertTrue(message, condition.getCondition(), false);
                return;
            } catch (AssertionError e) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e1) { }
            }
        }
        assertTrue(message, condition.getCondition());
    }    

    /**
     * @param message the message to log on fail
     * @param condition the condition to check
     * @param timeoutMilliseconds the time the condition should hold
     * @param intervalMilliseconds the time interval between assertions
     */
    static public void repetitiveAssertConditionHolds(String message,
    		RepetitiveConditionProvider condition, 
    		long timeoutMilliseconds, long intervalMilliseconds) {
    	long end = System.currentTimeMillis() + timeoutMilliseconds;
    	while (System.currentTimeMillis() < end) {
    			_assertTrue(message, condition.getCondition(), false);
    				try {
						Thread.sleep(intervalMilliseconds);
					} catch (InterruptedException e) {}
    	}
    	assertTrue(message, condition.getCondition());
    }    

    public static void assertEquals(Object expected, Object actual) {
    	assertEquals("", expected, actual);
    }

    public static void assertEquals(String msg, Object expected, Object actual) {
		try {
			org.testng.AssertJUnit.assertEquals(msg, expected, actual);
		}
		catch (AssertionFailedError e) {
			if (!skipException()) {
				throw e;
			}
		}
    }
    
	public static void assertEquals(int expected, int actual) {
		assertEquals("", expected, actual);
	}

	public static void assertEquals(String msg, int expected, int actual) {
		if (expected != actual) {
			if (!skipException()) {
				org.testng.AssertJUnit.assertEquals(msg, expected, actual);				
			}
		}
	}

	public static void AssertFail(String msg) {
		AssertFail(msg, null);
	}
	
	public static void AssertFail(String msg, Exception e) {
		if (!skipException()) {
			if (e != null) {
				org.testng.Assert.fail(msg, e);
			} else {
				org.testng.Assert.fail(msg);
			}
		}
	}

	public static void assertNotNull(Object obj) {
		assertNotNull("", obj);
	}

	public static void assertNull(Object obj) {
	    assertNull("", obj);
	}
	
	
	public static void assertNotNull(String msg, Object obj) {
		if (obj == null) {
			if (!skipException()) {
				org.testng.AssertJUnit.assertNotNull(msg, obj);
			}
		}		
	}

	public static void assertNull(String msg, Object obj) {
	    if (obj != null) {
	        if (!skipException()) {
	            org.testng.AssertJUnit.assertNull(msg, obj);
	        }
	    }		
	}

	public static  void assertTrue(boolean cond) {
		_assertTrue("", cond, true);
	}

	public static  void assertTrue(String msg, boolean cond) {
		_assertTrue(msg, cond, true);
	}
	
	public static void _assertTrue(String msg, boolean cond, boolean checkForSkip) {
		if (!cond) {
			boolean skip = checkForSkip ? skipException() : false; 
			if (!skip) {
				org.testng.AssertJUnit.assertTrue(msg, cond);				
			}
		}
	}
	
	static public void assertEquivalenceArrays(String message, Object[] expected, Object[] actual)

    {

                    if (Arrays.equals(expected, actual)) {

                                    return;

                    }



                    String formatted = format(message);



                    org.testng.AssertJUnit.assertNotNull(formatted + "expected array: <not null> but was <null>", expected);

                    org.testng.AssertJUnit.assertNotNull(formatted + "expected array: <not null> but was <null>", actual);



                    ArrayList<Object> missing = new ArrayList<Object>();

                    for (int i = 0; i < expected.length; i++) {

                                    missing.add(expected[i]);

                    }



                    ArrayList<Object> extra = new ArrayList<Object>();

                    for (int i = 0; i < actual.length; i++) {

                                    extra.add(actual[i]);

                    }



                    @SuppressWarnings("unchecked")
					ArrayList<Object> missingClone = (ArrayList<Object>) missing.clone();

                    missing.removeAll(extra);

                    extra.removeAll(missingClone);



                    org.testng.AssertJUnit.assertTrue(formatted + "[ " + missing.size() + " Missing elements: " + missing + "]", missing.size() == 0);

                    org.testng.AssertJUnit.assertTrue(formatted + "[ " + extra.size() + " Extra elements: " + extra + "]", extra.size() == 0);

    }
	
	static String format(String message){

        if (message != null)

                        return message+" - ";

        else

                        return "";
}



	private AssertUtils() {}

	public static void repetitiveAssertFalse(
			String message,
			final RepetitiveConditionProvider condition,
			long timeoutMilliseconds) {
		repetitiveAssertTrue(
				message, 
				new RepetitiveConditionProvider() {
			
					@Override
					public boolean getCondition() {
						return !condition.getCondition();
					}
				}, 
				timeoutMilliseconds);
	}
}
