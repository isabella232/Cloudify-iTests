package test.admin.jvm.statistics;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.space.SpaceDeployment;
import org.openspaces.admin.vm.VirtualMachineStatistics;
import org.openspaces.core.GigaSpace;
import org.openspaces.core.executor.DistributedTask;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.AbstractTest;
import test.utils.AdminUtils;
import test.utils.AssertUtils.RepetitiveConditionProvider;
import test.utils.LogUtils;

import com.gigaspaces.async.AsyncResult;


/**
 * 
 * Testing getCpuPercAverage of VirtualMachineStatistics
 * @author dank
 *
 */
public class JVMCpuPercAverageStatisticsTest extends AbstractTest {

    private GridServiceAgent gsa;
    private GridServiceManager gsm;
    private GridServiceContainer gsc1;
    private ProcessingUnit pu;
    private GigaSpace gigaSpace;

    @Override
    @BeforeMethod
    public void beforeTest() {
        super.beforeTest();
        LogUtils.log("Waiting for GSA");
        gsa = admin.getGridServiceAgents().waitForAtLeastOne();
        LogUtils.log("Loading 1 GSM");
        gsm = AdminUtils.loadGSM(gsa);
        LogUtils.log("Loading 1 GSC");
        gsc1 = AdminUtils.loadGSC(gsa);
    }

    @Test(timeOut = DEFAULT_TEST_TIMEOUT , groups="1")
    public void test() throws InterruptedException, ExecutionException {

        int sampleIntervalInSeconds = 5;
        
        gsc1.getVirtualMachine().setStatisticsInterval(sampleIntervalInSeconds, TimeUnit.SECONDS);
        gsc1.getVirtualMachine().startStatisticsMonitor();

        int numberOfCPUs = gsc1.getOperatingSystem().getDetails().getAvailableProcessors();
        
        // we want each pu instance to occupy one core
        LogUtils.log("Deploying space partitioned " + numberOfCPUs + ",0");
        pu = gsm.deploy(new SpaceDeployment("A").partitioned(numberOfCPUs, 0));
        LogUtils.log("Waiting for space instances");
        pu.waitFor(pu.getTotalNumberOfInstances(), OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
        LogUtils.log("Waiting for space");
        pu.waitForSpace();
        gigaSpace = pu.getSpace().getGigaSpace();
        
        double utilization = 0.25;
        
        double allowedDeviation = 0.03 * numberOfCPUs;
        double expectedCpuAverage = utilization * numberOfCPUs;
        double minimumCpuAverage = expectedCpuAverage - allowedDeviation;
        double maximumCpuAverage = expectedCpuAverage + allowedDeviation;
        
        long waitBetweenCpuSample = sampleIntervalInSeconds * 1000;

        LogUtils.log("Executing BusyWorkerTask with " + utilization + " utilization");
        gigaSpace.execute(new BusyWorkerTask(utilization));
        
        LogUtils.log("waiting for enough statistics to accumulate");
        repetitiveAssertTrue("Failed waiting for statistics to accumulate", new RepetitiveConditionProvider() {
            public boolean getCondition() {
                return gsc1.getVirtualMachine().getStatistics().getCpuPercAverage(60, TimeUnit.SECONDS) != -1;
            }
        }, OPERATION_TIMEOUT);
        // we have to wait for results to reach a stable state
        Thread.sleep(waitBetweenCpuSample * 4);
        
        int totalNumberOfSamples = 10;
        int currentNumberOfSamples = 0;
        
        LogUtils.log("Expecting calculated averages to be in the range: [" + minimumCpuAverage + ", " + maximumCpuAverage + "]");
        
        while(currentNumberOfSamples < totalNumberOfSamples) {
            
            LogUtils.log("Iteration #" + (currentNumberOfSamples+1) + " out of " + totalNumberOfSamples);
            
            VirtualMachineStatistics stats = gsc1.getVirtualMachine().getStatistics();
                
            for (int numberOfSeconds = 10; numberOfSeconds <= 60; numberOfSeconds += 5) { 
                assertCPUAverage(numberOfSeconds, stats, minimumCpuAverage, maximumCpuAverage);
            }
           
            currentNumberOfSamples += 1;
            
            Thread.sleep(waitBetweenCpuSample);
        }
        
    }

    private void assertCPUAverage(int numberOfSeconds, VirtualMachineStatistics stats, double min, double max) {
        double cpuPercAverage = stats.getCpuPercAverage(numberOfSeconds, TimeUnit.SECONDS);
        assertTrue("Calculated average: " + cpuPercAverage + ", is not in the range [" + min + "," + max + "] as expected" ,
                cpuPercAverage >= min && cpuPercAverage <= max); 
    }
    
    static class BusyWorkerTask implements DistributedTask<Double,Double> {
        private static final long serialVersionUID = -1021175197916725203L;

        double utilization;
        
        public BusyWorkerTask(double utilization) {
            this.utilization = utilization;
        }
        
        public Double execute() throws Exception {
            double unitTimeInMillis = 100;
            double unitTimeUtilizationMillis = unitTimeInMillis * utilization;
            while(true) {
                long innerStartTime = System.currentTimeMillis();
                while(System.currentTimeMillis() < innerStartTime + unitTimeUtilizationMillis);
                Thread.sleep((long)(unitTimeInMillis - unitTimeUtilizationMillis));
            }
        }

        public Double reduce(List<AsyncResult<Double>> results) throws Exception {
            return 1.0;
        }
    }
    
}
