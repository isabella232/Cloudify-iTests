package framework.utils;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.openspaces.admin.Admin;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsa.events.GridServiceAgentAddedEventListener;
import org.openspaces.admin.gsa.events.GridServiceAgentRemovedEventListener;
import org.openspaces.admin.internal.admin.InternalAdmin;

import framework.utils.AssertUtils.RepetitiveConditionProvider;

public class GridServiceAgentsCounter implements GridServiceAgentAddedEventListener, GridServiceAgentRemovedEventListener {

	Admin admin;
	private final AtomicInteger numberOfAddedGSAs = new AtomicInteger(0);
    private final AtomicInteger numberOfRemovedGSAs = new AtomicInteger(0);
    
	
	public GridServiceAgentsCounter(final Admin admin) {
		final CountDownLatch addedLatch = new CountDownLatch(1);
		((InternalAdmin)admin).scheduleNonBlockingStateChange(new Runnable() {
	    	//TODO: Fix all openspaces eventmanagers to schedule at the correct thread, and use it also to send events when include=true. 
	    	public void run() {
	    		admin.getGridServiceAgents().getGridServiceAgentAdded().add(GridServiceAgentsCounter.this, true);
	            admin.getGridServiceAgents().getGridServiceAgentRemoved().add(GridServiceAgentsCounter.this);
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
        admin.getGridServiceAgents().getGridServiceAgentAdded().remove(this);
        admin.getGridServiceAgents().getGridServiceAgentRemoved().remove(this);
    }
    
    public int getNumberOfGSAsAdded() {
        return numberOfAddedGSAs.get();
    }
    
    public int getNumberOfGSAsRemoved() {
        return numberOfRemovedGSAs.get();
    }

	public void gridServiceAgentAdded(
			GridServiceAgent gridServiceAgent) {
		numberOfAddedGSAs.incrementAndGet();
		LogUtils.log("GridServiceAgentAdded on machine " + gridServiceAgent.getMachine().getHostAddress());
		
	}

	public void gridServiceAgentRemoved(
			GridServiceAgent gridServiceAgent) {
		numberOfRemovedGSAs.incrementAndGet();
		LogUtils.log("GridServiceAgentRemoved from machine " + gridServiceAgent.getMachine().getHostAddress());
	}
	
	public void repetitiveAssertNumberOfGridServiceAgentsAdded(final int expected, long timeoutMilliseconds) {
		if (numberOfAddedGSAs.get() > expected) {
			AssertUtils.AssertFail("Expected " + expected +" GSAs Added. actual " + numberOfAddedGSAs.get());
		}
		AssertUtils.repetitiveAssertTrue("Expected " + expected +" GSAs Added.", new RepetitiveConditionProvider() {
			
			@Override
			public boolean getCondition() {
				int actual = numberOfAddedGSAs.get();
				boolean condition = expected == actual;
				if (!condition) {
					LogUtils.log("Expected " + expected +" GSAs Added. actual " + actual);
				}
				return condition;
			}
		}, 
		timeoutMilliseconds);
	}
	
	public void repetitiveAssertNumberOfGridServiceAgentsRemoved(final int expected, long timeoutMilliseconds) {
		if (numberOfRemovedGSAs.get() > expected) {
			AssertUtils.AssertFail("Expected " + expected +" GSAs Removed. actual " + numberOfRemovedGSAs.get());
		}
		AssertUtils.repetitiveAssertTrue("Expected " + expected +" GSAs Removed.", new RepetitiveConditionProvider() {
			
			@Override
			public boolean getCondition() {
				int actual = numberOfRemovedGSAs.get();
				boolean condition = expected == actual;
				if (!condition) {
					LogUtils.log("Expected " + expected +" GSAs Removed. actual " + actual);
				}
				return condition;
			}
		}, 
		timeoutMilliseconds);
	}
}
