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
 * this test tried to reproduce a known issue in groovy that causes a PermGen Exception.
 * it is disabled by default since it takes 2 hours to run and should only be executed pre-release.
 * 
 * @author adaml
 *
 */
public class ReadDSLMultipleTimesTest extends AbstractTestSupport{

	private static final long TWO_SECONDS_MILLIS = 2000;
	private final String APPLICATIONS_PATH = CommandTestUtils.getBuildApplicationsPath();
	private final ExecutorService executor = Executors.newFixedThreadPool(10);
	private final List<FutureTask<Void>> taskList = new ArrayList<FutureTask<Void>>();
	
	// test executes for 2 hours.
	private final long EXECUTION_TIME_MILLIS = 7200000;

	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void testReadDSLMultipleTimes() throws Exception {
		final long startTime = System.currentTimeMillis();
		for (int i = 0; i < 10; i++) {
			final FutureTask<Void> task = createTask();
			taskList.add(task);
			executor.execute(task);
			
		}
		while ((System.currentTimeMillis() - startTime) < EXECUTION_TIME_MILLIS) {

			try {
				for (FutureTask<Void> runningTask : taskList) {
					runningTask.get(1, TimeUnit.SECONDS);
				}
				Thread.sleep(TWO_SECONDS_MILLIS);
			} catch (TimeoutException e) {
				// timeout exception is ignored.
			} catch (final Throwable e) {
				if (e.getMessage().contains("Out of memory")) {
					LogUtils.log("PermGen exception thrown. exception message is: " + e.getMessage());
					LogUtils.log("full stack trace is: " + ExceptionUtils.getFullStackTrace(e));
					AssertFail("test failed. class cache leak found");
				}
			}

		}
	}
	
	private FutureTask<Void> createTask() { 
		return new FutureTask<Void>(
				new Callable<Void>() {
					@Override
					public Void call() throws Exception {
						while (true) {
							Iterator<File> groovyFiles = FileUtils.iterateFiles(new File(APPLICATIONS_PATH), new String[]{"groovy"}, true);
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
										dslReader.readDslEntity(Service.class);
									} catch (final Error e) {
										if (e instanceof OutOfMemoryError) {
											throw new RuntimeException("Out of memory.", e);
										}
									}
								}
							}
						}
					}
				});
	}



}
