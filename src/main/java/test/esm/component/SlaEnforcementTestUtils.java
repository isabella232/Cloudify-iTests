package test.esm.component;

import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openspaces.admin.Admin;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.internal.admin.InternalAdmin;
import org.openspaces.admin.internal.pu.elastic.ProcessingUnitSchemaConfig;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.grid.gsm.containers.ContainersSlaEnforcementEndpoint;
import org.openspaces.grid.gsm.containers.ContainersSlaPolicy;
import org.openspaces.grid.gsm.machines.AbstractMachinesSlaPolicy;
import org.openspaces.grid.gsm.machines.CapacityMachinesSlaPolicy;
import org.openspaces.grid.gsm.machines.EagerMachinesSlaPolicy;
import org.openspaces.grid.gsm.machines.MachinesSlaEnforcementEndpoint;
import org.openspaces.grid.gsm.machines.MachinesSlaUtils;
import org.openspaces.grid.gsm.machines.plugins.ElasticMachineProvisioning;
import org.openspaces.grid.gsm.rebalancing.RebalancingSlaEnforcementEndpoint;
import org.openspaces.grid.gsm.rebalancing.RebalancingSlaPolicy;
import org.openspaces.grid.gsm.sla.exceptions.SlaEnforcementFailure;
import org.openspaces.grid.gsm.sla.exceptions.SlaEnforcementInProgressException;
import org.openspaces.grid.gsm.strategy.DiscoveredMachinesCache;
import org.testng.Assert;

import framework.utils.DumpUtils;

public class SlaEnforcementTestUtils {

	static Log logger = LogFactory.getLog(SlaEnforcementTestUtils.class);
	
	public static void enforceSlaAndWait(
			final ProcessingUnit pu, 
			final RebalancingSlaEnforcementEndpoint endpoint, 
			final RebalancingSlaPolicy sla,
			final ProcessingUnitSchemaConfig schema,
			final int minimumNumberOfInstances, long timeout, TimeUnit timeunit) throws InterruptedException, TimeoutException {
		
		    enforceSlaAndWait(pu.getAdmin(), new Callable<Void>() {

			boolean reachedMinimumNumberOfInstances = false;
			@Override
			public Void call() throws Exception {
				
				if (!reachedMinimumNumberOfInstances && pu.getNumberOfInstances() >= minimumNumberOfInstances) {
					reachedMinimumNumberOfInstances = true;
				}
				try {
					endpoint.enforceSla(sla);
				}
				finally {
					if (!reachedMinimumNumberOfInstances && pu.getNumberOfInstances() >= minimumNumberOfInstances) {
						reachedMinimumNumberOfInstances = true;
					}
					else if (reachedMinimumNumberOfInstances && pu.getNumberOfInstances() < minimumNumberOfInstances) {
						DumpUtils.dumpProcessingUnit(pu.getAdmin());
						Assert.fail("Failed to maintain minimum number of instances " + minimumNumberOfInstances);
					}
				}
				return null;
			}
			
		},timeout, timeunit);
	}
	
	public static void enforceSlaAndWait(
			final Admin admin, 
			final ContainersSlaEnforcementEndpoint endpoint, 
			final ContainersSlaPolicy sla) throws InterruptedException {
		
		enforceSlaAndWait(admin, new Callable<Void>() {

			@Override
			public Void call() throws Exception {
				endpoint.enforceSla(sla);
				return null;
			}
		});
	}

	public static void enforceSlaAndWait(
			final Admin admin, 
			final MachinesSlaEnforcementEndpoint endpoint, 
			final EagerMachinesSlaPolicy sla,
			final ElasticMachineProvisioning machineProvisioning) throws InterruptedException {
		
		enforceSlaAndWait(admin, new Callable<Void>() {

			@Override
			public Void call() throws Exception {
				updateSlaWithProvisionedAgents(admin, sla, machineProvisioning);
				endpoint.recoverStateOnEsmStart(sla);
				endpoint.enforceSla(sla);
				return null;
			}
		});
	}

	public static void enforceSlaAndWait(
			final Admin admin, 
			final MachinesSlaEnforcementEndpoint endpoint, 
			final CapacityMachinesSlaPolicy sla,
			final ElasticMachineProvisioning machineProvisioning) throws InterruptedException {
		
		enforceSlaAndWait(admin, new Callable<Void>() {

			@Override
			public Void call() throws Exception {
				updateSlaWithProvisionedAgents(admin, sla, machineProvisioning);
				endpoint.recoverStateOnEsmStart(sla);
				endpoint.enforceSla(sla);
				return null;
			}
		});
	}
	
	public static void enforceSla(
			final Admin admin, 
			final RebalancingSlaEnforcementEndpoint endpoint, 
			final RebalancingSlaPolicy sla) throws InterruptedException {
		enforceSla(admin, new Callable<Void>() {

			@Override
			public Void call() throws Exception {
				endpoint.enforceSla(sla);
				return null;
			}
			
		});
	}
	
	public static void enforceSla(
			final Admin admin, 
			final ContainersSlaEnforcementEndpoint endpoint, 
			final ContainersSlaPolicy sla) throws InterruptedException {
		enforceSla(admin, new Callable<Void>() {

			@Override
			public Void call() throws Exception {
				endpoint.enforceSla(sla);
				return null;
			}
		});
	}

	public static void enforceSla(
			final Admin admin, 
			final MachinesSlaEnforcementEndpoint endpoint, 
			final EagerMachinesSlaPolicy sla,
			final ElasticMachineProvisioning machineProvisioning) throws InterruptedException {
		
		enforceSla(admin, new Callable<Void>() {

			@Override
			public Void call() throws Exception {
				updateSlaWithProvisionedAgents(admin, sla, machineProvisioning);
				endpoint.recoverStateOnEsmStart(sla);
				endpoint.enforceSla(sla);
				return null;
			}
		});
	}

	public static void enforceSla(
			final Admin admin, 
			final MachinesSlaEnforcementEndpoint endpoint, 
			final CapacityMachinesSlaPolicy sla,
			final ElasticMachineProvisioning machineProvisioning) throws InterruptedException {
		
		enforceSla(admin, new Callable<Void>() {

			@Override
			public Void call() throws Exception {
				updateSlaWithProvisionedAgents(admin, sla, machineProvisioning);
				endpoint.enforceSla(sla);
				return null;
			}
		});
	}

	private static void enforceSlaAndWait(
			final Admin admin, 
			final Callable<Void> enforceSla) throws InterruptedException {
		
		long timeout = (long) Integer.MAX_VALUE;
		TimeUnit timeunit = TimeUnit.MILLISECONDS;
		try {
			enforceSlaAndWait(admin, enforceSla, timeout, timeunit);
		}
		catch(TimeoutException e) {
			throw new IllegalStateException("cannot timeout with infinite timeout value",e);
		}
	}
	
	private static void enforceSlaAndWait(
			final Admin admin, 
			final Callable<Void> enforceSla,
			long timeout,
			TimeUnit timeunit) 
	throws InterruptedException, TimeoutException {
		 
		final long end = System.currentTimeMillis() + timeunit.toMillis(timeout);
		
	     final CountDownLatch latch = new CountDownLatch(1);
	     final AtomicReference<Throwable> ex = new AtomicReference<Throwable>();
	     ScheduledFuture<?> scheduledTask = 
	               ((InternalAdmin)admin).scheduleWithFixedDelayNonBlockingStateChange(
	               new Runnable() {
	     
	                   public void run() {
	                      try {
	                    	  if (System.currentTimeMillis() > end) {
	                    		  throw new TimeoutException("enforceSlaAndWait timed out");
	                    	  }
	                    	enforceSla.call();
	                    	logger.info("enforceSlaAndWait done.");
	                    	// done
	                    	latch.countDown();
	                      }
	                      catch (SlaEnforcementInProgressException e) {
	                    	  if (e instanceof SlaEnforcementFailure) {
	                    		  logger.warn("enforceSlaAndWait:",e);  
	                    	  }
	                    	  else {
	                    		  logger.info("enforceSlaAndWait:",e);
	                    	  }
	                      }
	                      catch (Throwable e) {
	                    	  ex.set(e);
	                    	  logger.info("enforceSlaAndWait unhandled exception",e);
	                    	  latch.countDown();
	                      }
	                        
	                    }
	                    
	                }, 
	            
	                0, 100, TimeUnit.MILLISECONDS);
	            
	            try {
	                latch.await();
	                if (ex.get() != null) {
	                    Assert.fail("Exception in enforceSla",ex.get());
	                }
	            }
	            finally {
	                scheduledTask.cancel(false);
	            }
	}

	public static <T> void updateSlaWithProvisionedAgents(final Admin admin,
			final T sla, final ElasticMachineProvisioning machineProvisioning) {
		
		((AbstractMachinesSlaPolicy)sla).setDiscoveredMachinesCache(new DiscoveredMachinesCache() {
            @Override
            public Collection<GridServiceAgent> getDiscoveredAgents() {
                return MachinesSlaUtils.sortAndFilterAgents(admin.getGridServiceAgents().getAgents(), machineProvisioning.getConfig(), logger);               
            }
        });
	}

	
	public static void enforceSla(
			final Admin admin,
			final Callable<Void> enforceSla) throws InterruptedException {

		final CountDownLatch latch = new CountDownLatch(1);
	    final AtomicReference<Throwable> ex = new AtomicReference<Throwable>();
           ((InternalAdmin)admin).scheduleNonBlockingStateChange(
           new Runnable() {
 
               public void run() {
                  try {
                	 enforceSla.call();
                  }
                  catch (SlaEnforcementInProgressException e) {
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
}
