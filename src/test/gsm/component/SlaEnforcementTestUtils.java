package test.gsm.component;

import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.openspaces.admin.Admin;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.internal.admin.InternalAdmin;
import org.openspaces.cloud.xenserver.XenServerMachineProvisioning;
import org.openspaces.grid.gsm.containers.ContainersSlaEnforcementEndpoint;
import org.openspaces.grid.gsm.containers.ContainersSlaPolicy;
import org.openspaces.grid.gsm.machines.AbstractMachinesSlaPolicy;
import org.openspaces.grid.gsm.machines.MachinesSlaUtils;
import org.openspaces.grid.gsm.machines.plugins.ElasticMachineProvisioning;
import org.openspaces.grid.gsm.rebalancing.RebalancingSlaEnforcementEndpoint;
import org.openspaces.grid.gsm.rebalancing.RebalancingSlaPolicy;
import org.openspaces.grid.gsm.sla.ServiceLevelAgreementEnforcementEndpoint;
import org.openspaces.grid.gsm.sla.ServiceLevelAgreementPolicy;
import org.openspaces.grid.gsm.sla.exceptions.SlaEnforcementException;
import org.testng.Assert;

import java.util.logging.Level;
import java.util.logging.Logger;

public class SlaEnforcementTestUtils {

	static Logger logger = Logger.getLogger(SlaEnforcementTestUtils.class.getName());
	
	public static void enforceSlaAndWait(
			final Admin admin, 
			final RebalancingSlaEnforcementEndpoint endpoint, 
			final RebalancingSlaPolicy sla) throws InterruptedException {
		enforceSlaAndWait(admin, endpoint, sla, null);
	}
	
	public static void enforceSlaAndWait(
			final Admin admin, 
			final ContainersSlaEnforcementEndpoint endpoint, 
			final ContainersSlaPolicy sla) throws InterruptedException {
		enforceSlaAndWait(admin, endpoint, sla, null);
	}
	
	public static <T extends ServiceLevelAgreementPolicy> void enforceSlaAndWait(
			final Admin admin, 
			final ServiceLevelAgreementEnforcementEndpoint<T> endpoint, 
			final T sla, 
			final ElasticMachineProvisioning machineProvisioning) 
	throws InterruptedException {
		
	     final CountDownLatch latch = new CountDownLatch(1);
	     final AtomicReference<Throwable> ex = new AtomicReference<Throwable>();
	     ScheduledFuture<?> scheduledTask = 
	               ((InternalAdmin)admin).scheduleWithFixedDelayNonBlockingStateChange(
	               new Runnable() {
	     
	                   public void run() {
	                      try {
	                    	  
	                    	enforceSlaInternal(admin, endpoint, sla, machineProvisioning);
	                    	logger.log(Level.INFO,"enforceSlaAndWait done.");
	                    	// done
	                    	latch.countDown();
	                      }
	                      catch (SlaEnforcementException e) {
	                    	  logger.log(Level.INFO,"enforceSlaAndWait:",e);
	                      }
	                      catch (Throwable e) {
	                    	  ex.set(e);
	                    	  logger.log(Level.INFO,"enforceSlaAndWait unhandled exception",e);
	                    	  latch.countDown();
	                      }
	                        
	                    }
	                    
	                }, 
	            
	                0, 1, TimeUnit.SECONDS);
	            
	            try {
	                latch.await();
	                if (ex.get() != null) {
	    				if (ex.get() instanceof java.lang.AssertionError) {
	    					throw (java.lang.AssertionError)ex.get();
	    				}
	                    Assert.fail("Exception in enforceSla",ex.get());
	                }
	            }
	            finally {
	                scheduledTask.cancel(false);
	            }
	}

	public static <T> void updateSlaWithProvisionedAgents(final Admin admin,
			final T sla, final ElasticMachineProvisioning machineProvisioning) {
		final Collection<GridServiceAgent> agents = new HashSet<GridServiceAgent>();
		  for (final GridServiceAgent agent : admin.getGridServiceAgents().getAgents()) {
			  if (MachinesSlaUtils.isAgentConformsToMachineProvisioningConfig(agent, machineProvisioning.getConfig())) {
				  agents.add(agent);
			  }
		  }
		((AbstractMachinesSlaPolicy)sla).setProvisionedAgents(agents);
	}

	public static <T extends ServiceLevelAgreementPolicy> void enforceSla(
			final Admin admin,
			final ServiceLevelAgreementEnforcementEndpoint<T> endpoint,
			final T sla,
			final XenServerMachineProvisioning machineProvisioning) throws InterruptedException {

		final CountDownLatch latch = new CountDownLatch(1);
	    final AtomicReference<Throwable> ex = new AtomicReference<Throwable>();
           ((InternalAdmin)admin).scheduleNonBlockingStateChange(
           new Runnable() {
 
               public void run() {
                  try {
                	 enforceSlaInternal(admin, endpoint, sla, machineProvisioning);
                  }
                  catch (SlaEnforcementException e) {
                	  // fall through
                  }
                  catch (Throwable e) {
                	  ex.set(e);
                  }
                  finally {
                	  latch.countDown();
                  }
                    
                }
                
            });
        

	    latch.await();
	    if (ex.get() != null) {
			if (ex.get() instanceof java.lang.AssertionError) {
				throw (java.lang.AssertionError)ex.get();
			}
	        Assert.fail("Exception in enforceSla",ex.get());
	    }
	        
		
	}
    
	private static <T extends ServiceLevelAgreementPolicy> void enforceSlaInternal(
			final Admin admin,
			final ServiceLevelAgreementEnforcementEndpoint<T> endpoint,
			final T sla,
			final ElasticMachineProvisioning machineProvisioning) throws SlaEnforcementException {
		
		if (sla instanceof AbstractMachinesSlaPolicy) {
  		  updateSlaWithProvisionedAgents(admin, sla,machineProvisioning);
  	  	}
		endpoint.enforceSla(sla);
	}
}
