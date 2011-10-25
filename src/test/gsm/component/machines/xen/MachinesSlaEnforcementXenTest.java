package test.gsm.component.machines.xen;

import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsa.GridServiceContainerOptions;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.internal.admin.InternalAdmin;
import org.openspaces.grid.gsm.machines.CapacityMachinesSlaPolicy;
import org.openspaces.grid.gsm.machines.exceptions.MachinesSlaEnforcementInProgressException;
import org.openspaces.grid.gsm.machines.exceptions.MachinesSlaEnforcementPendingContainerDeallocationException;
import org.openspaces.grid.gsm.sla.exceptions.SlaEnforcementEndpointDestroyedException;
import org.openspaces.grid.gsm.sla.exceptions.SlaEnforcementException;
import org.testng.Assert;
import org.testng.annotations.Test;

import test.gsm.GsmTestUtils;
import test.gsm.component.SlaEnforcementTestUtils;
import test.utils.AssertUtils.RepetitiveConditionProvider;

public class MachinesSlaEnforcementXenTest extends AbstractMachinesSlaEnforcementTest {

	@Test(timeOut = DEFAULT_TEST_TIMEOUT)
    public void oneMachineTest() throws InterruptedException  {
        endpoint = createEndpoint(pu, machinesSlaEnforcement);
        
        // the first GSA is already started in BeginTest
	    Assert.assertEquals(admin.getGridServiceAgents().getAgents().length,1);
		
		Assert.assertEquals(getNumberOfGSAsAdded()   ,1);
    	Assert.assertEquals(getNumberOfGSAsRemoved() ,0);
    	
    	enforceNumberOfMachines(1);
    	
    	// there was already one GSA running
    	Assert.assertEquals(getNumberOfGSAsAdded()   ,2);
    	Assert.assertEquals(getNumberOfGSAsRemoved() ,0);
    	Assert.assertEquals(admin.getGridServiceAgents().getAgents().length,2);
    	
    	enforceUndeploy();
    	
    	Assert.assertEquals(getNumberOfGSAsAdded()   ,2);
    	// the request to destroy the last GSA was ignored since this GSA runs also the LUS and GSM
        Assert.assertEquals(getNumberOfGSAsRemoved() ,1);
	}
	
    @Test(timeOut = DEFAULT_TEST_TIMEOUT)
    public void oneMachineNonDedicatedManagementMachinesTest() throws InterruptedException  {
        endpoint = createEndpoint(pu, machinesSlaEnforcement);
        
        // the first GSA is already started in BeginTest
	    Assert.assertEquals(admin.getGridServiceAgents().getAgents().length,1);
		
		Assert.assertEquals(getNumberOfGSAsAdded()   ,1);
    	Assert.assertEquals(getNumberOfGSAsRemoved() ,0);
    	
    	CapacityMachinesSlaPolicy sla = createSla(1);
    	SlaEnforcementTestUtils.enforceSlaAndWait(admin, endpoint, sla, machineProvisioning);
    	
    	// there was already one GSA running
    	Assert.assertEquals(getNumberOfGSAsAdded()   ,1);
    	Assert.assertEquals(getNumberOfGSAsRemoved() ,0);
    	Assert.assertEquals(admin.getGridServiceAgents().getAgents().length,1);
    	
       enforceUndeploy();
        
        Assert.assertEquals(getNumberOfGSAsAdded()   ,1);
        // the request to destroy the last GSA was ignored since this GSA runs also the LUS and GSM
        Assert.assertEquals(getNumberOfGSAsRemoved() ,0);
	}
	
    @Test(timeOut = DEFAULT_TEST_TIMEOUT)
    public void oneMachineTestWithContainerWithWrongZone() throws InterruptedException  {
       
       // the first GSA is already started in BeginTest
        Assert.assertEquals(admin.getGridServiceAgents().getAgents().length,1);
        GridServiceAgent agent2 = super.startNewVM(OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
        GridServiceContainer container2 = agent2.startGridServiceAndWait(new GridServiceContainerOptions().
                vmInputArgument("-Dcom.gs.zones=" + WRONG_ZONE));
        
        endpoint = createEndpoint(pu, machinesSlaEnforcement);

        // the first GSA is already started in BeginTest
        Assert.assertEquals(admin.getGridServiceAgents().getAgents().length,2);
        
        Assert.assertEquals(getNumberOfGSAsAdded()   ,2);
        Assert.assertEquals(getNumberOfGSAsRemoved() ,0);
        
        enforceNumberOfMachines(1);
        
        // there was already one non-management GSA running, but its dedicated to management
        // and one machine with wrong zone
        // so a new machine was started
        Assert.assertEquals(getNumberOfGSAsAdded()   ,3);
        Assert.assertEquals(getNumberOfGSAsRemoved() ,0);
        Assert.assertEquals(admin.getGridServiceAgents().getAgents().length,3);
        
        GsmTestUtils.killContainer(container2);
        enforceUndeploy();
        
        Assert.assertEquals(getNumberOfGSAsAdded()   ,3);
        // ESM does not shutdown machines it hasn't started. And it started only one machine
        Assert.assertEquals(getNumberOfGSAsRemoved() ,1);
  
    }
	
   @Test(timeOut = DEFAULT_TEST_TIMEOUT)
   public void oneMachineTestWithContainerWithZone() throws InterruptedException  {
      
      // the first GSA is already started in BeginTest
       Assert.assertEquals(admin.getGridServiceAgents().getAgents().length,1);
       GridServiceAgent agent2 = super.startNewVM(OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
       GridServiceContainer container = agent2.startGridServiceAndWait(new GridServiceContainerOptions().
               vmInputArgument("-Dcom.gs.zones=" + pu.getRequiredZones()[0]));
       

       endpoint = createEndpoint(pu, machinesSlaEnforcement);
       
       Assert.assertEquals(admin.getGridServiceContainers().getContainers().length,1);
       Assert.assertEquals(getNumberOfGSAsAdded()   ,2);
       Assert.assertEquals(getNumberOfGSAsRemoved() ,0);
       
       enforceNumberOfMachines(1);
       
       // there was already one GSA running with a container with the correct zone.
       Assert.assertEquals(getNumberOfGSAsAdded()   ,2);
       Assert.assertEquals(getNumberOfGSAsRemoved() ,0);
       Assert.assertEquals(admin.getGridServiceAgents().getAgents().length,2);
       
       GsmTestUtils.killContainer(container);
       enforceUndeploy();
       
       Assert.assertEquals(getNumberOfGSAsAdded()   ,2);
       // ESM doent shutdown machines it hasnt started
       Assert.assertEquals(getNumberOfGSAsRemoved() ,0);
    
   }
   
    @Test(timeOut = DEFAULT_TEST_TIMEOUT)
    public void minimumNumberOfMachinesTest() throws InterruptedException  {
		
        endpoint = createEndpoint(pu, machinesSlaEnforcement);
        
        Assert.assertEquals(admin.getGridServiceAgents().getAgents().length,1);
		Assert.assertEquals(getNumberOfGSAsAdded()   ,1);
    	Assert.assertEquals(getNumberOfGSAsRemoved() ,0);
    	/*
    	CapacityMachinesSlaPolicy sla = createSla(0);
    	sla.setMinimumNumberOfMachines(1);
		SlaEnforcementTestUtils.enforceSlaAndWait(admin, endpoint, sla);
    	// the request to destroy the last GSA was ignored since this GSA runs also the LUS and GSM
    	Assert.assertEquals(admin.getGridServiceAgents().getAgents().length,2);
    	Assert.assertEquals(getNumberOfGSAsAdded()   ,2);
    	Assert.assertEquals(getNumberOfGSAsRemoved() ,0);
    	*/
    	enforceUndeploy();
        /*
        Assert.assertEquals(getNumberOfGSAsAdded()   ,2);
        // the request to destroy the last GSA was ignored since this GSA runs also the LUS and GSM
        Assert.assertEquals(getNumberOfGSAsRemoved() ,1);
        */
	}
	
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT)
    public void twoMachinesTest() throws InterruptedException  {
		
        endpoint = createEndpoint(pu, machinesSlaEnforcement);
        
        Assert.assertEquals(admin.getGridServiceAgents().getAgents().length,1);
		Assert.assertEquals(getNumberOfGSAsAdded()   ,1);
    	Assert.assertEquals(getNumberOfGSAsRemoved() ,0);
    	
    	enforceNumberOfMachines(2);
    	
    	Assert.assertEquals(admin.getGridServiceAgents().getAgents().length,3);
    	Assert.assertEquals(getNumberOfGSAsAdded()   ,3);
    	Assert.assertEquals(getNumberOfGSAsRemoved() ,0);
    	
        enforceUndeploy();
        
        Assert.assertEquals(getNumberOfGSAsAdded()   ,3);
        // the request to destroy the last GSA was ignored since this GSA runs also the LUS and GSM
        Assert.assertEquals(getNumberOfGSAsRemoved() ,2);
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT)
	public void scaleOutMachinesTest() throws InterruptedException {
    
    	endpoint = createEndpoint(pu, machinesSlaEnforcement);
        
        Assert.assertEquals(admin.getGridServiceAgents().getAgents().length,1);
        Assert.assertEquals(getNumberOfGSAsAdded()   ,1);
        Assert.assertEquals(getNumberOfGSAsRemoved() ,0);
        
        enforceNumberOfMachines(2);
        
        Assert.assertEquals(admin.getGridServiceAgents().getAgents().length,3);
        Assert.assertEquals(getNumberOfGSAsAdded()   ,3);
        Assert.assertEquals(getNumberOfGSAsRemoved() ,0);
        
        enforceNumberOfMachines(3);
    	
    	Assert.assertEquals(admin.getGridServiceAgents().getAgents().length,4);
    	Assert.assertEquals(getNumberOfGSAsAdded()   ,4);
    	Assert.assertEquals(getNumberOfGSAsRemoved() ,0);
    	
        enforceUndeploy();
        
        Assert.assertEquals(getNumberOfGSAsAdded()   ,4);
        // the request to destroy the last GSA was ignored since this GSA runs also the LUS and GSM
        Assert.assertEquals(getNumberOfGSAsRemoved() ,3);
    }

    @Test(timeOut = DEFAULT_TEST_TIMEOUT)
	public void scaleInMachinesTest() throws InterruptedException {
    	
    	endpoint = createEndpoint(pu, machinesSlaEnforcement);
        
        Assert.assertEquals(admin.getGridServiceAgents().getAgents().length,1);
        Assert.assertEquals(getNumberOfGSAsAdded()   ,1);
        Assert.assertEquals(getNumberOfGSAsRemoved() ,0);
        
        enforceNumberOfMachines(2);
        
        Assert.assertEquals(admin.getGridServiceAgents().getAgents().length,3);
        Assert.assertEquals(getNumberOfGSAsAdded()   ,3);
        Assert.assertEquals(getNumberOfGSAsRemoved() ,0);
        
        enforceNumberOfMachines(1);
    	
    	Assert.assertEquals(admin.getGridServiceAgents().getAgents().length,2);
    	Assert.assertEquals(getNumberOfGSAsAdded()   ,3);
    	Assert.assertEquals(getNumberOfGSAsRemoved() ,1);
    	
    	super.repetitiveAssertTrue("Number of machines must be 2", 
    			new RepetitiveConditionProvider() {

					public boolean getCondition() {
						return MachinesSlaEnforcementXenTest.super.countMachines()==2;
					}}
    			, DEFAULT_TEST_TIMEOUT);
    	
    	enforceUndeploy();
        
        Assert.assertEquals(getNumberOfGSAsAdded()   ,3);
        // the request to destroy the last GSA was ignored since this GSA runs also the LUS and GSM
        Assert.assertEquals(getNumberOfGSAsRemoved() ,2);
    }

	
    
    @Test(timeOut = DEFAULT_TEST_TIMEOUT)
	public void scaleInMachinesWithContainersTest() throws InterruptedException {
    	
    	endpoint = createEndpoint(pu, machinesSlaEnforcement);
        
        Assert.assertEquals(admin.getGridServiceAgents().getAgents().length,1);
        Assert.assertEquals(getNumberOfGSAsAdded()   ,1);
        Assert.assertEquals(getNumberOfGSAsRemoved() ,0);
        
        enforceNumberOfMachines(2);
        
        Assert.assertEquals(admin.getGridServiceAgents().getAgents().length,3);
        Assert.assertEquals(getNumberOfGSAsAdded()   ,3);
        Assert.assertEquals(getNumberOfGSAsRemoved() ,0);
        
        // must set correct zone or machine is restricted for PU
         
        for (GridServiceAgent gsa : admin.getGridServiceAgents()) {
        	startContainerOnAgent(gsa);
    	}
    	
    	//scale in
    	final CapacityMachinesSlaPolicy sla = createSla(1);
    	final AtomicBoolean evacuated = new AtomicBoolean(false);
    	final AtomicReference<Throwable> ex = new AtomicReference<Throwable>();
    	final CountDownLatch latch = new CountDownLatch(1);
    	ScheduledFuture<?> scheduledTask = 
			((InternalAdmin)admin).scheduleWithFixedDelayNonBlockingStateChange(
			new Runnable() {

				public void run() {
					
					
					try {
						SlaEnforcementTestUtils.updateSlaWithProvisionedAgents(admin, sla, machineProvisioning);
						endpoint.enforceSla(sla);
						latch.countDown();
					}
					catch (MachinesSlaEnforcementPendingContainerDeallocationException e) {
						if(!evacuated.get()) {
							
							Assert.assertEquals(getNumberOfGSAsAdded()   ,3);
							Assert.assertEquals(getNumberOfGSAsRemoved() ,0);

							Collection<String> allocatedAgentsUids = null;
							try {
								allocatedAgentsUids = endpoint.getAllocatedCapacity().getAgentUids();
								Assert.assertEquals(allocatedAgentsUids.size(),1);
							} catch (SlaEnforcementEndpointDestroyedException destroyedEx) {
								AssertFail("endpoint destroyed unexpectedly", destroyedEx);
							}
                            
							
							for (final GridServiceContainer container : admin.getGridServiceContainers()) {
								if (!allocatedAgentsUids.contains(container.getGridServiceAgent().getUid())) {
									((InternalAdmin)admin).scheduleAdminOperation(new Runnable() {
										
										public void run() {
											container.kill();
										}
									});
								}
							}
			    			evacuated.set(true);
			    		}
					}
					catch (MachinesSlaEnforcementInProgressException e) {
						//try again next time
					}
					catch (Throwable e) {
                      ex.set(e);
                      latch.countDown();
                   }
				}
			}, 
		
			0, 10, TimeUnit.SECONDS);
		
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

    	Assert.assertTrue(evacuated.get());
    	Assert.assertEquals(admin.getGridServiceAgents().getAgents().length,2);
    	Assert.assertEquals(getNumberOfGSAsAdded()   ,3);
    	Assert.assertEquals(getNumberOfGSAsRemoved() ,1);
    	
    	super.repetitiveAssertTrue("Number of machines must be 2", 
    			new RepetitiveConditionProvider() {

					public boolean getCondition() {
						return MachinesSlaEnforcementXenTest.super.countMachines()==2;
					}}
    			, DEFAULT_TEST_TIMEOUT);
        
    	while(admin.getGridServiceContainers().getSize() > 0) {
    		GridServiceContainer container = admin.getGridServiceContainers().iterator().next();
			GsmTestUtils.killContainer(container);
        }
    	
    	enforceUndeploy();
        
    	Assert.assertEquals(admin.getGridServiceAgents().getAgents().length,1);
    	
        Assert.assertEquals(getNumberOfGSAsAdded()   ,3);
        // the request to destroy the last GSA was ignored since this GSA runs also the LUS and GSM
        Assert.assertEquals(getNumberOfGSAsRemoved() ,2);
    }

    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", expectedExceptions=SlaEnforcementEndpointDestroyedException.class)
	void destroyTest() throws InterruptedException, SlaEnforcementException {
	    endpoint = createEndpoint(pu, machinesSlaEnforcement);
		this.machinesSlaEnforcement.destroyEndpoint(pu);
		endpoint.enforceSla(createSla(1));
	}
  

    
    
	
}
