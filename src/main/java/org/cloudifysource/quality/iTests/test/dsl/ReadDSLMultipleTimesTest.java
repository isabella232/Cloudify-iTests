package org.cloudifysource.quality.iTests.test.dsl;

import iTests.framework.utils.LogUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.cloudifysource.domain.Service;
import org.cloudifysource.dsl.internal.DSLReader;
import org.cloudifysource.dsl.internal.DSLUtils;
import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
import org.cloudifysource.quality.iTests.test.cli.cloudify.CommandTestUtils;
import org.testng.annotations.Test;

/**
 * This test tries to reproduce an issue in Groovy that causes a PermGen OOME Exception.
 * Multithrading is used to simulate a specific scenario where a tomcat service receives
 * multiple parallel requests, each for parsing a large amount of scripts. Basically what that means
 * is that having a single process running multiple threads would accelerate the leak and OOME.
 * 
 * To debug this test it is recommended to set vm arguments "-Xms256m -Xmx256m -XX:PermSize=35m -XX:MaxPermSize=35m"
 * 
 * @author adaml
 *
 */
public class ReadDSLMultipleTimesTest extends AbstractTestSupport{

	private static final int ETERNITY = 100000;
	private static final String OUT_OF_MEMORY_MESSAGE = "OOME";
	private static final int NUMBER_OF_THREADS = 3;
	private static final long TWO_SECONDS_MILLIS = 2000;
	private static final String APPLICATIONS_PATH = CommandTestUtils.getBuildApplicationsPath();

	private ExecutorService executor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);
	private List<FutureTask<Void>> taskList = new ArrayList<FutureTask<Void>>();

	// test executes for 10 minutes.
	private final long TEN_MINUTE_MILLIS = TimeUnit.MINUTES.toMillis(10);

	// Tests leak over a long period of time.
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = false)
	public void testReadDSLMultipleTimes() throws Exception {
		startReadDSLMultipleTimes(TEN_MINUTE_MILLIS, 10);
	}

	public void startReadDSLMultipleTimes(final long duration, final int numberOfThreads) throws Exception {
		LogUtils.log("Using " + APPLICATIONS_PATH + " as Groovy Service files home dir.");

		final long startTime = System.currentTimeMillis();
		LogUtils.log("Creating " + NUMBER_OF_THREADS + " task threads.");
		for (int i = 0; i < NUMBER_OF_THREADS; i++) {
			final FutureTask<Void> task = createTask();
			taskList.add(task);
			executor.execute(task);
		}

		LogUtils.log("Task threads created and running. Waiting for OutOfMemoryError to occur...");
		while ((System.currentTimeMillis() - startTime) < TEN_MINUTE_MILLIS) {
			try {
				for (FutureTask<Void> runningTask : taskList) {
					runningTask.get(1, TimeUnit.SECONDS);
				}
				Thread.sleep(TWO_SECONDS_MILLIS);
			} catch (final TimeoutException e) {
				// timeout exception is ignored.
			} catch (final Throwable e) {
				LogUtils.log(ExceptionUtils.getFullStackTrace(e));
				if (e.getMessage().contains(OUT_OF_MEMORY_MESSAGE)) {
					AssertFail("PermGen OOME thrown. This is an indication of a memory leak.");
				}
				AssertFail("a Runtime exception terminated this test unexpectedly.");
			}
		}
		LogUtils.log("Execution duration ended. Terminating executor service.");
		executor.shutdownNow();
		executor.awaitTermination(5, TimeUnit.MINUTES);
		assertTrue("Executor was not terminated as expected.", executor.isShutdown());
		executor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);
		taskList = new ArrayList<FutureTask<Void>>();
	}

	private FutureTask<Void> createTask() { 
		return new FutureTask<Void>(
				new Callable<Void>() {
					@Override
					public Void call() throws Exception {
						for (int i = 0; i < ETERNITY; i++) {
							Iterator<File> groovyFiles = FileUtils.iterateFiles(new File(APPLICATIONS_PATH), new String[]{"groovy"}, true);
							if (!groovyFiles.hasNext()) {
								throw new AssertionError("Destination folder must contain Groovy service files.");
							}
							//						 Run forever.
							while (groovyFiles.hasNext()) {
								File recipeFile = groovyFiles.next();
								if (recipeFile.getName().contains("-service")) {
									try {
										DSLReader dslReader = new DSLReader();
										dslReader.setRunningInGSC(false);
										dslReader.setPropertiesFileName(null); //find default
										dslReader.setDslFile(recipeFile);
										dslReader.setWorkDir(recipeFile.getParentFile());
										dslReader.setDslFileNameSuffix(DSLUtils.SERVICE_DSL_FILE_NAME_SUFFIX);
										Service service = dslReader.readDslEntity(Service.class);
										System.out.println("Service File read: " + service.getName());
										Thread.sleep(1000);
									} catch (final Error e) {
										if (e instanceof OutOfMemoryError) {
											throw new RuntimeException(OUT_OF_MEMORY_MESSAGE, e);
										} 
										throw new RuntimeException(e);
									}
								}
							}
						}
						return null;
					}
				});
	}
}
