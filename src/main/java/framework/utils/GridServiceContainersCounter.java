package framework.utils;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.openspaces.admin.Admin;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.gsc.events.GridServiceContainerAddedEventListener;
import org.openspaces.admin.gsc.events.GridServiceContainerRemovedEventListener;
import org.openspaces.admin.internal.admin.InternalAdmin;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.grid.gsm.containers.ContainersSlaUtils;

import framework.utils.AssertUtils.RepetitiveConditionProvider;

public class GridServiceContainersCounter implements GridServiceContainerAddedEventListener, GridServiceContainerRemovedEventListener {

	private final Admin admin;
	private final AtomicInteger numberOfAddedGSCs = new AtomicInteger(0);
    private final AtomicInteger numberOfRemovedGSCs = new AtomicInteger(0);
	private final String zone;
    
    public GridServiceContainersCounter(final ProcessingUnit pu) {
    	this(pu.getAdmin(),ContainersSlaUtils.getContainerZone(pu));
    }
    public GridServiceContainersCounter(final Admin admin) {
    	this(admin,null);
    }
    
	public GridServiceContainersCounter(final Admin admin, final String zone) {
		this.zone = zone;
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
			GridServiceContainer container) {
		if (isContainerMatchesZone(container)) {
			numberOfAddedGSCs.incrementAndGet();
		}
		
	}

	public void gridServiceContainerRemoved(
			GridServiceContainer container) {
		if (isContainerMatchesZone(container)) {
			numberOfRemovedGSCs.incrementAndGet();
		}
	}
	
	private boolean isContainerMatchesZone(GridServiceContainer container) {
		return zone == null || ContainersSlaUtils.isContainerMatchesZone(container, zone);
	}
	
	public void repetitiveAssertNumberOfGridServiceContainersAdded(final int expected, long timeoutMilliseconds) {
		if (numberOfAddedGSCs.get() > expected) {
			AssertUtils.AssertFail("Expected " + expected +" GSCs Added. actual " + numberOfAddedGSCs.get());
		}
		
		AssertUtils.repetitiveAssertTrue("Expected " + expected +" GSCs Added.", new RepetitiveConditionProvider() {
			
			@Override
			public boolean getCondition() {
				int actual = numberOfAddedGSCs.get();
				boolean condition = expected == actual;
				if (!condition) {
					LogUtils.log("Expected " + expected +" GSCs Added. actual " + actual);
				}
				return condition;
			}
		}, 
		timeoutMilliseconds);
	}
	
	public void repetitiveAssertNumberOfGridServiceContainersRemoved(final int expected, long timeoutMilliseconds) {
		
		if (numberOfRemovedGSCs.get() > expected) {
			AssertUtils.AssertFail("Expected " + expected +" GSCs Removed. actual " + numberOfRemovedGSCs.get());
		}
		
		AssertUtils.repetitiveAssertTrue("Expected " + expected +" GSCs Removed.", new RepetitiveConditionProvider() {
			
			@Override
			public boolean getCondition() {
				int actual = numberOfRemovedGSCs.get();
				boolean condition = expected == actual;
				if (!condition) {
					LogUtils.log("Expected " + expected +" GSCs Removed. actual " + actual);
				}
				return condition;
			}
		}, 
		timeoutMilliseconds);
	}
}
