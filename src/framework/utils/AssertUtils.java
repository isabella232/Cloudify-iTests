package framework.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import junit.framework.AssertionFailedError;

import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.testng.Assert;

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
	
	public static void AssertFail(String msg, Throwable t) {
		if (!skipException()) {
			if (t != null) {
				org.testng.Assert.fail(msg, t);
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
	public static void repetitive(IRepetitiveRunnable repeatedAssert, int timeout)
    {
        for(int delay = 0; delay < timeout; delay += 5)
        {
            try
            {
                repeatedAssert.run();
                return;
            }
            catch(Throwable e) //catch any exception and error
            {
                try
                {
                    Thread.sleep(5);
                }
                catch (InterruptedException e1)
                {
                }
            }
        }
        try
        {
            repeatedAssert.run();
        }
        catch (AssertionError e)
        {
            throw e;
        }
        catch (Exception e)
        {
        	AssertFail(e.getMessage(), e);
        }
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
	
	public static ProcessingUnitInstance[] repetitiveAssertNumberOfInstances(
			final ProcessingUnit pu, final int expectedNumberOfInstances) {
		
		final AtomicReference<ProcessingUnitInstance[]> instances = new AtomicReference<ProcessingUnitInstance[]>();
		repetitiveAssertTrue("Failed waiting for " + expectedNumberOfInstances +" " + pu.getName() + " instances.", 
			new RepetitiveConditionProvider() {
			
			@Override
			public boolean getCondition() {
				instances.set(pu.getInstances());
				final int numberOfInstances = instances.get().length;
				if (numberOfInstances != expectedNumberOfInstances) {
					LogUtils.log(
							"Waiting for " + expectedNumberOfInstances + " " + pu.getName() + " instances. "+
							"Actual " + numberOfInstances + " instances.");
				}
				return numberOfInstances == expectedNumberOfInstances;
			}
		}, TimeUnit.MILLISECONDS.convert(5, TimeUnit.MINUTES));
		return instances.get();
	}
	
	 
	 public static void reptitiveCountdownLatchAwait(final CountDownLatch countdownLatch, final String name, long timeout, TimeUnit timeunit) {
			repetitiveAssertTrue("latch count is not zero", new RepetitiveConditionProvider() {
				
				@Override
				public boolean getCondition() {
					try {
						LogUtils.log("Waiting for latch " + name + " . Current count: " + countdownLatch.getCount());
						return countdownLatch.await(0, TimeUnit.MILLISECONDS);
					} catch (final InterruptedException e) {
						Assert.fail("Interrupted while waiting for latch " + name, e);
						return false;
					}
				}
			}, timeunit.toMillis(timeout));
	}
}
