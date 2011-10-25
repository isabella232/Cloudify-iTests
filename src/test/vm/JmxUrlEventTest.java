package test.vm;

import static test.utils.AdminUtils.loadGSC;
import static test.utils.AdminUtils.loadGSM;
import static test.utils.DeploymentUtils.getArchive;
import static test.utils.LogUtils.log;

import org.openspaces.admin.AdminEventListener;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitDeployment;
import org.openspaces.admin.vm.VirtualMachine;
import org.openspaces.admin.vm.events.VirtualMachineAddedEventListener;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.AbstractTest;
import test.utils.AdminUtils;

public class JmxUrlEventTest extends AbstractTest{
	private Machine machine;
    private GridServiceManager gsm;
    
    //private Admin myAdmin;
    MyAdminListener listener;
    boolean fail=false;
	
    @Override
	@BeforeMethod
	public void beforeTest() {		
        super.beforeTest();               
		log("waiting for 1 GSA");
		GridServiceAgent gsa= admin.getGridServiceAgents().waitForAtLeastOne();
		machine = gsa.getMachine();
		
		log("loading GSM");
		gsm = loadGSM(machine);		
		log("loading 3 GSC on 1 machine");
		
		loadGSC(machine);
		loadGSC(machine);
		loadGSC(machine);
		ProcessingUnit pu = gsm.deploy(
                new ProcessingUnitDeployment(getArchive("processorPU.jar")).
                numberOfInstances(2).
                numberOfBackups(1).
                clusterSchema("partitioned-sync2backup").
                name("jmx-test"));
        pu.waitFor(pu.getTotalNumberOfInstances());
        admin = null;		
	}	
    
    
    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1")
	  public void test() throws Exception {			 	      
	        admin = AdminUtils.createAdmin();
	        listener = new MyAdminListener();				
	        admin.addEventListener(listener);
	        
	        //wait till all events arrived 
	        try {
				Thread.sleep(60000);
			}
			catch (Exception e){}
			Assert.assertEquals(fail, false);
			admin.removeEventListener(listener);
    }

    
    class   MyAdminListener implements AdminEventListener, VirtualMachineAddedEventListener{

		public void virtualMachineAdded(VirtualMachine virtualMachine) {
			if (virtualMachine.getGridServiceContainer() != null){
				String url = virtualMachine.getDetails().getJmxUrl();
				System.out.println("------------------------url="+ url);
				if (url==null)
					fail=true;
			}
			
		}
    }

}
