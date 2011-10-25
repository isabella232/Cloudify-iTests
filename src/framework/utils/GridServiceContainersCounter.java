package framework.utils;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.openspaces.admin.Admin;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.gsc.events.GridServiceContainerAddedEventListener;
import org.openspaces.admin.gsc.events.GridServiceContainerRemovedEventListener;
import org.openspaces.admin.internal.admin.InternalAdmin;

public class GridServiceContainersCounter implements GridServiceContainerAddedEventListener, GridServiceContainerRemovedEventListener {

	Admin admin;
	private final AtomicInteger numberOfAddedGSCs = new AtomicInteger(0);
    private final AtomicInteger numberOfRemovedGSCs = new AtomicInteger(0);
    
	
	public GridServiceContainersCounter(final Admin admin) {
		final CountDownLatch addedLatch = new CountDownLatch(1);
	    ((InternalAdmin)admin).scheduleNonBlockingStateChange(new Runnable() {
	    	//TODO: Fix all openspaces eventmanagers to schedule at the correct thread, and use it also to send events when include=true. 
	    	public void run() {
				admin.getGridServiceContainers().getGridServiceContainerAdded().add(GridServiceContainersCounter.this, true);
		        admin.getGridServiceContainers().getGridServiceContainerRemoved().add(GridServiceContainersCounter.this);
		        addedLatch.countDown();
			}});
		
        this.admin = admin;
        try {
			addedLatch.await();
		} catch (InterruptedException e) {
			throw new RuntimeException("Interrupted while waiting for added scheduled runnable", e);
		}
    }
    
    public void close() {
        admin.getGridServiceContainers().getGridServiceContainerAdded().remove(this);
        admin.getGridServiceContainers().getGridServiceContainerRemoved().remove(this);
    }
    
    public int getNumberOfGSCsAdded() {
        return numberOfAddedGSCs.get();
    }
    
    public int getNumberOfGSCsRemoved() {
        return numberOfRemovedGSCs.get();
    }

	public void gridServiceContainerAdded(
			GridServiceContainer gridServiceContainer) {
		numberOfAddedGSCs.incrementAndGet();
		
	}

	public void gridServiceContainerRemoved(
			GridServiceContainer gridServiceContainer) {
		numberOfRemovedGSCs.incrementAndGet();		
	}
}
