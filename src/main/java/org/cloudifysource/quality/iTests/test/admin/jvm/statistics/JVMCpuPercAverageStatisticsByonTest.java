package org.cloudifysource.quality.iTests.test.admin.jvm.statistics;

import com.gigaspaces.async.AsyncResult;
import iTests.framework.utils.AssertUtils;
import iTests.framework.utils.AssertUtils.RepetitiveConditionProvider;
import iTests.framework.utils.LogUtils;
import org.cloudifysource.quality.iTests.test.esm.AbstractFromXenToByonGSMTest;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsa.GridServiceContainerOptions;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.space.SpaceDeployment;
import org.openspaces.admin.vm.VirtualMachineStatistics;
import org.openspaces.core.GigaSpace;
import org.openspaces.core.executor.DistributedTask;
import org.testng.annotations.*;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;


/**
 *
 * Testing getCpuPercAverage of VirtualMachineStatistics
 * @author dank
 *
 */
public class JVMCpuPercAverageStatisticsByonTest extends AbstractFromXenToByonGSMTest {
    @BeforeMethod
    public void beforeTest() {
        super.beforeTestInit();
    }

    @BeforeClass
    protected void bootstrap() throws Exception {
        super.bootstrapBeforeClass();
    }

    @AfterMethod
    public void afterTest() {
        super.afterTest();
    }

    @AfterClass(alwaysRun = true)
    protected void teardownAfterClass() throws Exception {
        super.teardownAfterClass();
    }

    private static final double ALLOWED_CPU_NOISE_BEFORE_TEST = 0.02;
    private static final double ALLOWED_CPU_PERC_DEVIATION = 0.06;
    private static final int AVERAGE_WINDOW_SECONDS = 60;

    private ProcessingUnit pu;
    private GigaSpace gigaSpace;

    @Test(timeOut = DEFAULT_TEST_TIMEOUT , groups="1")
    public void test() throws InterruptedException, ExecutionException {

        LogUtils.log("Waiting for GSA running on new byon machine");
        //GridServiceAgent gsa = startNewVM( 4, 0,OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
        GridServiceAgent gsa = null;
        try {
            gsa = startNewByonMachine(getElasticMachineProvisioningCloudifyAdapter(), OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            AssertUtils.assertFail("Failed to start a new Machine: "+e.getMessage());
            e.printStackTrace();
        }
        AssertUtils.assertNotNull(gsa);
        LogUtils.log("Loading 1 GSC");
        final GridServiceContainer gsc = loadGSC(gsa);

        int sampleIntervalInSeconds = 5;

        gsc.getVirtualMachine().setStatisticsInterval(sampleIntervalInSeconds, TimeUnit.SECONDS);
        gsc.getVirtualMachine().startStatisticsMonitor();

        int numberOfCPUs = gsc.getOperatingSystem().getDetails().getAvailableProcessors();

        // we want each pu instance to occupy one core
        LogUtils.log("Deploying space partitioned " + numberOfCPUs + ",0");
        pu = super.deploy(new SpaceDeployment("A").partitioned(numberOfCPUs, 0));
        LogUtils.log("Waiting for space instances");
        pu.waitFor(pu.getTotalNumberOfInstances(), OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
        LogUtils.log("Waiting for space");
        pu.waitForSpace();
        gigaSpace = pu.getSpace().getGigaSpace();

        double utilization = 0.25;

        double allowedDeviation = ALLOWED_CPU_PERC_DEVIATION * numberOfCPUs;

        LogUtils.log("Calculating baseline CPU reading");
        repetitiveAssertTrue("Failed waiting for statistics to accumulate", new RepetitiveConditionProvider() {
            public boolean getCondition() {
                return getCpuPercAverage(gsc) != -1;
            }
        }, OPERATION_TIMEOUT);

        final AtomicReference<Double> baselineCpuPercAverage = new AtomicReference<Double>();
        repetitiveAssertTrue("Failed waiting for CPU to drop to " + ALLOWED_CPU_NOISE_BEFORE_TEST, new RepetitiveConditionProvider() {
            public boolean getCondition() {
                baselineCpuPercAverage.set(getCpuPercAverage(gsc));
                return baselineCpuPercAverage.get() < ALLOWED_CPU_NOISE_BEFORE_TEST;
            }
        }, OPERATION_TIMEOUT);

        double expectedCpuAverage = utilization * numberOfCPUs + baselineCpuPercAverage.get();
        double minimumCpuAverage = expectedCpuAverage - allowedDeviation;
        double maximumCpuAverage = expectedCpuAverage + allowedDeviation;

        LogUtils.log("Executing BusyWorkerTask with " + utilization + " utilization");
        gigaSpace.execute(new BusyWorkerTask(utilization));

        LogUtils.log("waiting for enough statistics to accumulate");
        Thread.sleep(TimeUnit.SECONDS.toMillis(AVERAGE_WINDOW_SECONDS+sampleIntervalInSeconds));

        int totalNumberOfSamples = 10;
        int currentNumberOfSamples = 0;

        LogUtils.log("Expecting calculated averages to be in the range: [" + minimumCpuAverage + ", " + maximumCpuAverage + "]");

        while(currentNumberOfSamples < totalNumberOfSamples) {

            LogUtils.log("Iteration #" + (currentNumberOfSamples+1) + " out of " + totalNumberOfSamples);

            final double cpuPercAverage = getCpuPercAverage(gsc);
            assertTrue("Calculated average: " + cpuPercAverage + ", is not in the range [" + minimumCpuAverage + "," + maximumCpuAverage + "] as expected" ,
                    cpuPercAverage >= minimumCpuAverage && cpuPercAverage <= maximumCpuAverage);

            currentNumberOfSamples += 1;

            Thread.sleep(TimeUnit.SECONDS.toMillis(sampleIntervalInSeconds));
        }

    }

    private GridServiceContainer loadGSC(GridServiceAgent gsa) {
        GridServiceContainerOptions options = new GridServiceContainerOptions();
        options.vmInputArgument("-Dcom.gs.transport_protocol.lrmi.bind-port="+LRMI_BIND_PORT_RANGE);
        return gsa.startGridServiceAndWait(options);
    }

    private double getCpuPercAverage(GridServiceContainer gsc) {
        final VirtualMachineStatistics stats = gsc.getVirtualMachine().getStatistics();

        final double cpuPercAverage = stats.getCpuPercAverage(AVERAGE_WINDOW_SECONDS, TimeUnit.SECONDS);
        LogUtils.log("cpuPercAverage="+cpuPercAverage);
        return cpuPercAverage;
    }

    static class BusyWorkerTask implements DistributedTask<Double,Double> {
        private static final long serialVersionUID = -1021175197916725203L;

        double utilization;

        public BusyWorkerTask(double utilization) {
            this.utilization = utilization;
        }

        public Double execute() throws Exception {
            double unitTimeInMillis = 1000;
            double unitTimeUtilizationMillis = unitTimeInMillis * utilization;
            while(true) {
                long innerStartTime = System.currentTimeMillis();
                long now = innerStartTime;
                while(now < innerStartTime + unitTimeUtilizationMillis) {
                    now = System.currentTimeMillis();
                }
                Thread.sleep((long)(unitTimeInMillis - unitTimeUtilizationMillis));
            }
        }

        public Double reduce(List<AsyncResult<Double>> results) throws Exception {
            return 1.0;
        }
    }

}
