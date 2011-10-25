/**
 * 
 */
package test.cache;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.TimeUnit;

import net.jini.core.lease.Lease;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsa.GridServiceContainerOptions;
import org.openspaces.admin.gsa.GridServiceManagerOptions;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.space.SpaceDeployment;
import org.openspaces.admin.vm.VirtualMachine;
import org.openspaces.core.GigaSpace;
import org.openspaces.core.SpaceMemoryShortageException;
import org.testng.Assert;
import org.testng.annotations.Test;

import test.AbstractTest;
import test.utils.LogUtils;
import test.utils.TestUtils;
import test.utils.ThreadBarrier;

import com.gigaspaces.annotation.pojo.SpaceClass;
import com.gigaspaces.annotation.pojo.SpaceId;

/**
 * 
 * 
 * This test tests the memory manager synchronous watermark feature
 * 1. deploy space with lru and watermarks
 * 2. start writer threads that write 1MB objects in order to flood space
 * 3. assert the the memory level reached the high watermark 
 * 4. assert for a given time memory level doesn't breach the synchronous mark
 *
 * @author Sagi Bernstein
 * @since 8.0.5
 * 
 */
public class SynchronousWatermarkRemoteTest extends AbstractTest {
	private static final String SPACE_NAME = "qaSpace";
	private static final int CONDITION_TIMEOUT = 2 * 60 * 1000;
	private final int THREAD_SLEEP_TIME = 1;
	private double DEVIATION = 5;
	private final int OBJECT_SIZE = 1024 * 1024;
	private final int NUM_OF_THREADS = 8;
	private String highWatermark;
	private String lowWatermark;
	private String syncWatermark;
	private int id;
	private ThreadBarrier barrier = new ThreadBarrier(NUM_OF_THREADS + 1);
	private GigaSpace gigaSpace;
	private volatile boolean shutdown = false;
	private volatile boolean coughtSpaceMemoryShortageException = false;
	private final DecimalFormat df = new DecimalFormat("##.#");
	private double failedUsage;

	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups="1", enabled=true)
	public void test() throws Exception {

		GridServiceManager gsm = prepareEnvironment();

		lowWatermark = String.valueOf((int)getMemoryUsage() + 20);
		LogUtils.log("low watermark is: " + lowWatermark + "%");

		highWatermark = String.valueOf(Integer.valueOf(lowWatermark) + 10);
		LogUtils.log("high watermark is: " + highWatermark + "%");

		syncWatermark = String.valueOf(Integer.valueOf(highWatermark) + 5);
		LogUtils.log("synchronous watermark is: " + syncWatermark + "%");


		deploySpace(gsm);

		gigaSpace = admin.getSpaces().waitFor(SPACE_NAME).getGigaSpace();


		getGscMachine().setStatisticsInterval(10, TimeUnit.MILLISECONDS);

		getGscMachine().runGc();
		LogUtils.log("memory usage: " + df.format(getMemoryUsage()) + "%");



		LogUtils.log("starting write threads");
		for (int i = 0; i < NUM_OF_THREADS; i++) {
			new Thread(new Writer()).start();
		}

		LogUtils.log("assert memory usage is over the high watermark");
		try{
			TestUtils.repetitive(new Runnable() {
				@Override
				public void run() {
					Assert.assertTrue(getMemoryUsage() > Integer.valueOf(highWatermark));
				}       
			}, (int)DEFAULT_TEST_TIMEOUT);
		} catch (AssertionError e) {
			shutdown = true;
			Assert.fail("memory usage didn't pass the high watermark");
		}

		LogUtils.log("assert memory usage never crosses " + (Integer.valueOf(syncWatermark) + DEVIATION) + "%");
		long start = System.currentTimeMillis();
		try {
			while (System.currentTimeMillis() - start < CONDITION_TIMEOUT) {
				getGscMachine().runGc();
				Assert.assertFalse((failedUsage = getMemoryUsage()) > Integer.valueOf(syncWatermark) + DEVIATION);
				Thread.sleep(THREAD_SLEEP_TIME / 10);
			}
		} catch (AssertionError e) {
			shutdown = true;
			Assert.fail("Test terminated, memory usage is: " + df.format(failedUsage) + "%");
		}

		TestUtils.repetitive(new Runnable() {
			@Override
			public void run() {
				Assert.assertTrue(coughtSpaceMemoryShortageException);
			}       
		}, (int)DEFAULT_TEST_TIMEOUT);
		
		shutdown = true;
		barrier.await();
		barrier.inspect();
	}


	private GridServiceManager prepareEnvironment() {
		GridServiceAgent gsa = admin.getGridServiceAgents().waitForAtLeastOne();

		gsa.startGridServiceAndWait(new GridServiceManagerOptions());
		gsa.startGridServiceAndWait(new GridServiceContainerOptions());

		GridServiceManager gsm = admin.getGridServiceManagers().waitForAtLeastOne();
		return gsm;
	}


	private Map<String, String> deploySpace(GridServiceManager gsm) {
		Map<String, String> props = new HashMap<String, String>();
		props.put("space-config.engine.cache_policy", "0");
		props.put("space-config.engine.memory_usage.enabled", "true");
		props.put("space-config.engine.cache_size", "100000");
		props.put("space-config.engine.memory_usage.high_watermark_percentage", highWatermark);
		props.put("space-config.engine.memory_usage.low_watermark_percentage", lowWatermark);
		props.put("space-config.engine.memory_usage.write_only_block_percentage", String.valueOf(Integer.valueOf(highWatermark) - 5));
		props.put("space-config.engine.memory_usage.write_only_check_percentage", String.valueOf(Integer.valueOf(highWatermark) - 5));
		props.put("space-config.engine.memory_usage.synchronous_eviction_watermark", syncWatermark);
		props.put("space-config.engine.memory_usage.eviction_batch_size", "1");
		props.put("space-config.engine.memory_usage.explicit-gc", "true");

		SpaceDeployment spaceDeployment = new SpaceDeployment(SPACE_NAME);

		for (String key : props.keySet()) {
			spaceDeployment.setContextProperty(key, props.get(key));
		}

		gsm.deploy(spaceDeployment);
		return props;
	}



	private VirtualMachine getGscMachine() {
		return admin.getGridServiceContainers().getContainers()[0].getVirtualMachine();
	}


	private double getMemoryUsage() {
		return getGscMachine().getStatistics().getMemoryHeapUsedPerc();
	}


	private int getNextId() {
		return ++id;
	}


	private class Writer implements Runnable{

		@Override
		public void run() {
			while(!shutdown)
			{
				StringBuilder sb = new StringBuilder();
				for (int i = 0; i < OBJECT_SIZE; ++i) {
					sb.append('a');
				}
				try {
					gigaSpace.write(new LargeObject(getNextId(), sb.toString()), Lease.FOREVER);
					LogUtils.log("memory usage: " + df.format(getMemoryUsage()) + "%");
					try {
						Thread.sleep(THREAD_SLEEP_TIME);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				} catch (SpaceMemoryShortageException e) {
					coughtSpaceMemoryShortageException = true;
				}
			}
			try {
				barrier.await();
			} catch (InterruptedException e) {
				e.printStackTrace();
				barrier.reset();
			} catch (BrokenBarrierException e) {
				e.printStackTrace();
				barrier.reset();
			}
		}
	}

	@SpaceClass
	public static class LargeObject{
		private String payload;
		private int id;

		public LargeObject() {}

		public LargeObject(int id, String payload) {
			super();
			this.payload = payload;
			this.id = id;
		}

		public void setId(int id) {
			this.id = id;
		}

		@SpaceId
		public int getId() {
			return id;
		}

		public String getPayload() {
			return payload;
		}

		public void setPayload(String payload) {
			this.payload = payload;
		}
	}
}
