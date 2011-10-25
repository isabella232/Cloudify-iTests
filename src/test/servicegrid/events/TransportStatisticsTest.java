package test.servicegrid.events;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import junit.framework.Assert;

import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.space.SpaceDeployment;
import org.openspaces.admin.transport.TransportStatistics;
import org.openspaces.admin.transport.TransportsStatistics;
import org.openspaces.admin.transport.events.TransportStatisticsChangedEvent;
import org.openspaces.admin.transport.events.TransportStatisticsChangedEventListener;
import org.openspaces.admin.transport.events.TransportsStatisticsChangedEvent;
import org.openspaces.admin.transport.events.TransportsStatisticsChangedEventListener;
import org.openspaces.core.GigaSpace;
import org.testng.annotations.Test;

import test.data.Person;

public class TransportStatisticsTest extends EventTestSkel {

    @Override
    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "2")
    public void test() throws Exception {
        final AtomicReference<Exception> ref = new AtomicReference<Exception>();
        
        final AtomicReference<TransportStatistics> transport_StatisticsRef = new AtomicReference<TransportStatistics>();
        final AtomicReference<TransportsStatistics> transports_StatisticsRef = new AtomicReference<TransportsStatistics>();
        
        final CountDownLatch transport_StatisticsChangedEvent = new CountDownLatch(50);
        final CountDownLatch transports_StatisticsChangedEvent = new CountDownLatch(50);
        
        loadGS();
        
        ProcessingUnit pu = admin.getGridServiceManagers().waitForAtLeastOne().deploy(new SpaceDeployment("transportDataGrid")
            .partitioned(2, 1)
            .maxInstancesPerMachine(1));
        
        admin.getTransports().setStatisticsInterval(100, TimeUnit.MILLISECONDS);
        admin.getTransports().startStatisticsMonitor();
        
        TransportStatisticsChangedEventListener transportEventListener = new TransportStatisticsChangedEventListener() {
            public void transportStatisticsChanged(TransportStatisticsChangedEvent event) {
                TransportStatistics stats = event.getStatistics();
                
                try {
                    // check nothing breaks
                    stats.getActiveThreadsCount();
                    stats.getActiveThreadsPerc();
                    stats.getAdminTimestamp();
                    stats.getCompletedTaskCount();
                    stats.getCompletedTaskPerSecond();
                    stats.getQueueSize();
                    stats.getTimestamp();
                    stats.getDetails().getBindHost();
                    stats.getDetails().getHostAddress();
                    stats.getDetails().getHostName();
                    stats.getDetails().getMaxThreads();
                    stats.getDetails().getMinThreads();
                    stats.getDetails().getPort();
                } catch (Exception e) {
                    ref.set(e);
                }
                
                transport_StatisticsChangedEvent.countDown();
                if (transport_StatisticsChangedEvent.getCount() == 0) {
                	transport_StatisticsRef.set(stats);
                }
            }
        };
        
        admin.getTransports().getTransportStatisticsChanged().add(transportEventListener);
        
        TransportsStatisticsChangedEventListener transportsEventListener = new TransportsStatisticsChangedEventListener() {
            public void transportsStatisticsChanged(TransportsStatisticsChangedEvent event) {
                TransportsStatistics stats = event.getStatistics();
                
                try {
                    // check nothing breaks
                    stats.getActiveThreadsCount();
                    stats.getActiveThreadsPerc();
                    stats.getCompletedTaskCount();
                    stats.getCompletedTaskPerSecond();
                    stats.getDetails().getMaxThreads();
                    stats.getDetails().getMinThreads();
                    stats.getQueueSize();
                    stats.getSize();
                    stats.getTimestamp();
                } catch (Exception e) {
                    ref.set(e);
                }
                
                transports_StatisticsChangedEvent.countDown();
                if (transports_StatisticsChangedEvent.getCount() == 0) {
                	transports_StatisticsRef.set(stats);
                }
            }
        };

        admin.getTransports().getStatisticsChanged().add(transportsEventListener);
        
        GigaSpace gigaSpace = pu.waitForSpace().getGigaSpace();
        Person[] persons = new Person[10000];
        for (int i=0; i<persons.length; i++) {
        	persons[i] = new Person((long)i, String.valueOf(i));
        }

        gigaSpace.writeMultiple(persons);

        assertTrue("Expecting transport events", transport_StatisticsChangedEvent.await(OPERATION_TIMEOUT, TimeUnit.MILLISECONDS));
        assertTrue("Expecting transports events", transports_StatisticsChangedEvent.await(OPERATION_TIMEOUT, TimeUnit.MILLISECONDS));
        
        if (ref.get() != null) {
            Assert.fail("Unexpected exception occured: " + ref.get().getMessage());
        }
        
        admin.getTransports().getTransportStatisticsChanged().remove(transportEventListener);
        admin.getTransports().getStatisticsChanged().remove(transportsEventListener);
        admin.getTransports().stopStatisticsMonitor();
        
        assertTrue(!transport_StatisticsRef.get().isNA());
        assertTrue(!transports_StatisticsRef.get().isNA());
    }

}
