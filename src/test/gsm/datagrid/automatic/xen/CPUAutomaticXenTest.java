package test.gsm.datagrid.automatic.xen;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.openspaces.admin.pu.elastic.config.EagerScaleConfigurer;
import org.openspaces.admin.space.ElasticSpaceDeployment;
import org.openspaces.admin.vm.VirtualMachineStatistics;
import org.openspaces.cloud.xenserver.XenServerMachineProvisioningConfig;
import org.openspaces.core.GigaSpace;
import org.openspaces.core.executor.Task;
import org.openspaces.core.util.MemoryUnit;
import org.testng.annotations.Test;
import test.gsm.AbstractXenGSMTest;
import test.gsm.GsmTestUtils;
import test.utils.AssertUtils.RepetitiveConditionProvider;
import test.utils.LogUtils;
import com.j_spaces.kernel.GSThread;


/**
 * 
 * Test automatic scale-out based on CPU Threshold.
 * Testing getCpuPercAverage of VirtualMachineStatistics
 * For the test to end we have to have 4 Machines started and running.
 * In order for us to obtain this, 4 partitions is the minimal number of partitions possible.
 * Since the feature is not yet released, a simulation is made instead by manually activating new VMs. 
 * @author Adaml
 *
 */
public class CPUAutomaticXenTest extends AbstractXenGSMTest {

	//Window size
	private static final int CPU_AVERAGE_PERIOD_SEC = 40;
	private static final int TASK_PERIOD_SEC = 10;
	private static final int SAMPLE_INTERVAL_SEC = 3;
	private static final int NUM_OF_CPU_CORES = 1;
	private static final int NUM_PARTITIONS = 4;
	private static final float CONTAINER_THRESHOLD = .60f;
	private static final long SLEEP_PERIOD_AFTER_NEW_MACHINE_SEC = 30;
	//Utilization Grow constant is multiplied by the number of partitions and therefore must NOT be too large. 
	private static final double UTILIZATION_GROW_CONST = .0075;
	private ProcessingUnit pu;
	private ScheduledThreadPoolExecutor scheduledExecutorService = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(
			1, new ThreadFactory() {

				public Thread newThread(Runnable r) {

					return new GSThread(r, this.getClass().getName());

				}});

	@Override protected XenServerMachineProvisioningConfig getMachineProvisioningConfig() {
		XenServerMachineProvisioningConfig config = super.getMachineProvisioningConfig();
		config.setNumberOfCpuCores(NUM_OF_CPU_CORES);
		return config;
	};  

	/**
	 * GS-9077
	 */
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2 , groups="1", enabled = false)
	public void test() throws InterruptedException, ExecutionException {

		admin.setStatisticsInterval(SAMPLE_INTERVAL_SEC, TimeUnit.SECONDS);
		admin.startStatisticsMonitor();
		//We want each pu instance to occupy one core
		LogUtils.log("Deploying space partitioned " + NUM_PARTITIONS + ",0");

		this.pu = gsm.deploy(
				new ElasticSpaceDeployment("eagerspace")
				.memoryCapacityPerContainer(500, MemoryUnit.MEGABYTES)
				.numberOfPartitions(NUM_PARTITIONS)
				.singleMachineDeployment()
				.numberOfBackupsPerPartition(0)
				.dedicatedMachineProvisioning(super.getDiscoveredMachineProvisioningConfig())
				.scale(new EagerScaleConfigurer()
				.atMostOneContainerPerMachine()
				.create())
		);

		LogUtils.log("Waiting for space instances");
		GsmTestUtils.waitForScaleToComplete(pu, 1, 1, OPERATION_TIMEOUT); 
		LogUtils.log("Waiting for space");

		//A new task is being created every 10 seconds. 
		scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
			private double dynamicUtilization = 0;
			public void run() {
				//With each run, the utilization of the task will grow in a linear manner.
				//The maximal utilization will not exceed the 4 machine utilization limit.
				if (dynamicUtilization * NUM_PARTITIONS < (NUM_PARTITIONS - .5) * CONTAINER_THRESHOLD){
					dynamicUtilization += UTILIZATION_GROW_CONST;
				}
				
				for (GridServiceContainer gsc : admin.getGridServiceContainers()){
					ProcessingUnitInstance[] instances = gsc.getProcessingUnitInstances();
					if (instances.length != 0){
						GigaSpace gigaSpace = instances[0].getSpaceInstance().getSpace().getGigaSpace();
						LogUtils.log("Expecting " + dynamicUtilization * instances.length + " CPU utilization with " + instances.length + " Instances");
						gigaSpace.execute(new BusyWorkerTask(dynamicUtilization * instances.length));
					}
				}
			}
		}
		,0,TASK_PERIOD_SEC,TimeUnit.SECONDS);

		LogUtils.log("waiting for enough statistics to accumulate");
		
		waitForCPUSamplesToAccumulate();
		//We have to wait for results to reach a stable state
		Thread.sleep(SLEEP_PERIOD_AFTER_NEW_MACHINE_SEC * 1000);

		while(!isTestComplete()) {
			//While there are still machines that have CPU Usage higher than the threshold.
			if (machineCrossedThreshold()){
				startNewVM();
				waitForCPUSamplesToAccumulate();
				//wait for cpu's to stabilize.
				Thread.sleep(SLEEP_PERIOD_AFTER_NEW_MACHINE_SEC * 1000);
			}
			Thread.sleep(SAMPLE_INTERVAL_SEC * 1000);
		}

	}

	private boolean isTestComplete(){
		if (admin.getGridServiceContainers().getContainers().length >= 4){
			return true;
		}else{
		return false;
		}
	}
	
	private void waitForCPUSamplesToAccumulate() {
		repetitiveAssertTrue("Failed waiting for statistics to accumulate", new RepetitiveConditionProvider() {
			public boolean getCondition() {
				boolean condition = true;
				for (GridServiceContainer gsc : admin.getGridServiceContainers().getContainers() ){
					if (gsc.getVirtualMachine().getStatistics().getCpuPercAverage(CPU_AVERAGE_PERIOD_SEC, TimeUnit.SECONDS) == -1){
						condition = false;
						break;
					}
				}
				return condition;
			}
		}, OPERATION_TIMEOUT);
	}
	
	private boolean machineCrossedThreshold() {
		boolean condition = false;
		for (GridServiceContainer gsc : admin.getGridServiceContainers().getContainers() ){
			VirtualMachineStatistics stats = gsc.getVirtualMachine().getStatistics();
			LogUtils.log("Current avg: " + stats.getCpuPercAverage(CPU_AVERAGE_PERIOD_SEC, TimeUnit.SECONDS) + "for " + gsc.getUid());
			if (stats.getCpuPercAverage(CPU_AVERAGE_PERIOD_SEC, TimeUnit.SECONDS) > CONTAINER_THRESHOLD){
				condition = true;
				break;
			}
		}
		return condition;
	}
	
	private void startNewVM() {
		int numberOfContainers = admin.getGridServiceContainers().getSize();
		assertTrue(admin.getGridServiceContainers().getContainers().length + 1 <= 4);
		super.startNewVM(1, 1000, OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
		GsmTestUtils.waitForScaleToComplete(pu, numberOfContainers + 1, OPERATION_TIMEOUT);
	}



	static class BusyWorkerTask implements Task<Double>  {
		private static final long serialVersionUID = -1021175197916725203L;
		double utilization;

		public BusyWorkerTask(double utilization) {
			this.utilization = utilization;
		}

		public Double execute() throws Exception {
			double unitTimeInMillis = TASK_PERIOD_SEC * 1000;
			double workTime = unitTimeInMillis * utilization;
			long innerStartTime = System.currentTimeMillis();
			
			//100% CPU Usage.
			while(System.currentTimeMillis() < innerStartTime + workTime);
			
			return (double) 0;
		}

		


	}

}

