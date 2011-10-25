package com.gigaspaces.ps.hibernateimpl.worker;

import org.openspaces.events.adapter.SpaceDataEvent;

import com.gigaspaces.ps.hibernateimpl.common.Account;
import com.gigaspaces.ps.hibernateimpl.common.ProcessedAccount;

/**
 * <p>A simple bean that is run within a Spring context that acts
 * as the processing unit context as well (when executed within a
 * processing unit container).
 *
 * <p>Is handed SpaceDataEvents from a polling container.<br /> 
 * This removes the need to use space.take and space.write in the code.
 *
 * <p>The template for the polling container is defined in the pu.xml.
 */
public class MyWorker {
	
    /**
     * This method is called any time the matching event is produced by the space.
     * 
     * <p>The OpenSpaces EventContainer will automagically call this method.
     *
     * @param obj The object that matches the prescribed template defined for this bean.    
     */
    @SpaceDataEvent
    public ProcessedAccount objProcessed(Account obj) {
        //process object. . .<YOUR CODE GOES HERE>
        System.out.println(this.getClass().getSimpleName()+" Received Account: "+obj);
        ProcessedAccount pa = new ProcessedAccount();
        pa.setId(obj.getId() + 9000);
        pa.setLastName(obj.getLastName() + "Processed");
        pa.setFirstName(obj.getFirstName() + "Processed");
        
        //return null if you do *not* want to write a result back into the space.
        //return an instance of Account if you *do* want to write a result back to the space.
      return pa;
    }
    
    // constructor designed to prove we exist
    public MyWorker(){
      System.out.println("MyWorker constructed...");
    }  
}