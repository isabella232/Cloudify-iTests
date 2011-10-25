package test.utils;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.openspaces.admin.Admin;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsa.events.GridServiceAgentAddedEventListener;
import org.openspaces.admin.gsa.events.GridServiceAgentRemovedEventListener;
import org.openspaces.admin.internal.admin.InternalAdmin;

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
}
