/*******************************************************************************
 * Copyright (c) 2012 GigaSpaces Technologies Ltd. All rights reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/

package org.cloudifysource.quality.iTests.test;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import iTests.framework.utils.SSHUtils;
import org.cloudifysource.quality.iTests.test.cli.cloudify.CommandTestUtils;
import org.cloudifysource.quality.iTests.test.data.Person;
import org.cloudifysource.usm.shutdown.DefaultProcessKiller;
import org.openspaces.admin.Admin;
import org.openspaces.admin.AdminFactory;
import org.openspaces.admin.internal.InternalAdminFactory;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitInstance;

import iTests.framework.utils.AssertUtils;
import iTests.framework.utils.LogUtils;
import iTests.framework.utils.AssertUtils.RepetitiveConditionProvider;
import iTests.framework.utils.DumpUtils;
import org.openspaces.core.GigaSpace;


public abstract class AbstractTestSupport {

    static AtomicLong idGenerator = new AtomicLong();

    public static final long DEFAULT_TEST_TIMEOUT = 15 * 60 * 1000;
	public static final long EXTENDED_TEST_TIMEOUT = 25 * 60 * 1000;
	public static final long OPERATION_TIMEOUT = 5 * 60 * 1000;
	public static final String SUSPECTED = "SUSPECTED";
    private static final int SSH_TIMEOUT = 60000;
    private static final String LIST_JAVA_PROCESSES_CMD = "ps -U tgrid | grep java | grep -v PID | awk '{ print $1 }'";

    public static final String USERNAME = "tgrid";
    public static final String PASSWORD = "tgrid";
	
	private static final int CREATE_ADMIN_TIMEOUT = 10 * 60 * 1000;

	protected Admin createAdminAndWaitForManagement() throws TimeoutException, InterruptedException {
		
		long endTime = System.currentTimeMillis() + CREATE_ADMIN_TIMEOUT;

		while (System.currentTimeMillis() < endTime) {
			Admin admin = createAdminFactory().create();

			try {
				if (!isFilteredAdmin()) {
					AssertUtils.assertTrue("Failed to discover lookup service even though admin was created", admin.getLookupServices().waitFor(1, 2, TimeUnit.MINUTES));
					AssertUtils.assertTrue("Failed to discover rest service even though admin was created", admin.getProcessingUnits().waitFor("rest", 2, TimeUnit.MINUTES) != null);
					return admin;
				} else {
					return admin; // filtered, cant wait for anything
				}
			} catch (final AssertionError ae) {
				LogUtils.log("Failed to create admin succesfully --> " + ae.getMessage());
				LogUtils.log("Closing admin and retrying");
				if (admin != null) {
					admin.close();
					admin = null;
				}
				Thread.sleep(5000);
			}

		}
		throw new TimeoutException("Timed out while creating admin");
	}
	
	protected Admin createAdmin() {
		return createAdminFactory().create();
	}
	
	protected AdminFactory createAdminFactory() {
		return new InternalAdminFactory();
	}
	
	protected boolean isFilteredAdmin() {
		return false;
	}
	
	protected File getTestFolder() {
	    return DumpUtils.getTestFolder();
	}

	protected File getBuildFolder() {
	    return DumpUtils.getTestFolder().getParentFile();    
	}

	public static void repetitiveAssertTrue(String message, RepetitiveConditionProvider condition, long timeoutMilliseconds) {
		AssertUtils.repetitiveAssertTrue(message, condition, timeoutMilliseconds);
	}

	public static void assertEquals(int expected, int actual) {
		AssertUtils.assertEquals(expected, actual);
	}

	public static void assertEquals(String msg, Object expected, Object actual) {
		AssertUtils.assertEquals(msg, expected, actual);
	}

	public static void assertEquals(Object expected, Object actual) {
		AssertUtils.assertEquals(expected, actual);
	}

	public static ProcessingUnitInstance[] repetitiveAssertNumberOfInstances(ProcessingUnit pu, int expectedNumberOfInstances) {
		return AssertUtils.repetitiveAssertNumberOfInstances(pu,expectedNumberOfInstances);
	}

	public static void reptitiveCountdownLatchAwait(CountDownLatch latch, String name, long timeout, TimeUnit timeunit) {
		AssertUtils.reptitiveCountdownLatchAwait(latch, name, timeout, timeunit);
	}
	public static void AssertFail(String msg) {
		AssertUtils.assertFail(msg);
	}

	public static void AssertFail(String msg, Exception e) {
		AssertUtils.assertFail(msg, e);
	}

	public static void assertNotNull(String msg, Object obj) {
		AssertUtils.assertNotNull(msg, obj);
	}

	public static void assertNotNull(Object obj) {
		AssertUtils.assertNotNull(obj);
	}

	public static void assertTrue(String msg, boolean cond) {
		AssertUtils.assertTrue(msg, cond);
	}

	public static void assertTrue(boolean cond) {
		AssertUtils.assertTrue(cond);
	}

	public static void assertEquals(String msg, int a, int b) {
		AssertUtils.assertEquals(msg, a, b);
	}

	public static boolean sleep(long millisecs) {
		try {
			Thread.sleep(millisecs);
			return true;
		} catch (InterruptedException e) {
			// no op
		}
		return false;		
	}

    public static void killProcessesByIDs(final Set<String> processesIDs) {

        for (final String pid : processesIDs) {
        	boolean processKilled = killUsingSigar(pid);
        	if (!processKilled) {
        		killProcessUsingCommand(pid);
        	}
        }
    }

    private static boolean killProcessUsingCommand(String pid) {
    	try {
    		LogUtils.log("Attempting to kill leaking process " + pid + " using kill command");
    		final Runtime rt = Runtime.getRuntime();
    		if (System.getProperty("os.name").toLowerCase().indexOf("windows") > -1) {
    			rt.exec("taskkill /F /PID " + pid);
    		}
    		else {
    			rt.exec("kill -9 " + pid);
    		}
    		return true;
    	} catch (final Exception e) {
    		LogUtils.log("Failed to kill process " + pid, e);
    		e.printStackTrace();
    		return false;
    	}

    }

	private static boolean killUsingSigar(final String pid) {
		LogUtils.log("Attempting to kill leaking process " + pid + " using sigar");
		final DefaultProcessKiller dpk = new DefaultProcessKiller();
		dpk.setKillRetries(10);
		final long processID = Long.valueOf(pid);
		try {
		    dpk.killProcess(processID);
		    return true;
		} catch (final Exception e) {
			LogUtils.log("Failed to kill process " + pid, e);
		    e.printStackTrace();
		    return false;
		}
	}

    public static Set<String> getLocalProcesses() throws IOException, InterruptedException {
        final Set<String> result = new HashSet<String>();

        if (!isWindows()) {
            String output = null;
            try {
                output = SSHUtils.runCommand(InetAddress.getLocalHost().getHostName(), SSH_TIMEOUT, LIST_JAVA_PROCESSES_CMD, USERNAME, PASSWORD);
            } catch (final UnknownHostException e) {
                e.printStackTrace();
            }
            final String[] lines = output.split("\n");

            for (final String pid : lines) {
                result.add(pid);
            }
        } else {
            String s = CommandTestUtils.runLocalCommand("jps", true, false);
            Pattern p = Pattern.compile("\\d+ Jps");
            Matcher m = p.matcher(s);
            if (m.find()) {
                s = s.replace(m.group(), "");
            }

            p = Pattern.compile("\\d+");
            m = p.matcher(s);
            while (m.find()) {
                final String pid = m.group();
                if (pid != null && pid.length() > 0) {
                    result.add(m.group());
                }
            }
        }
        return result;
    }

    public static void writePersonBatch(GigaSpace gigaSpace, int numOfObjects) {
        List<Person> objList = new ArrayList<Person>(numOfObjects);
        for (int i = 0; i < numOfObjects; i++) {
            Person p=new Person();
            p.setId(idGenerator.getAndIncrement());
            objList.add(p);
        }
        gigaSpace.writeMultiple(objList.toArray());
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().startsWith("win");
    }

}
