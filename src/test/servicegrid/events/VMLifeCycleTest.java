package test.servicegrid.events;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.openspaces.admin.vm.VirtualMachine;
import org.openspaces.admin.vm.events.VirtualMachineLifecycleEventListener;
import org.testng.annotations.Test;

public class VMLifeCycleTest extends EventTestSkel {

	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "2")
	public void test() throws Exception {
		final CountDownLatch addedVMLatch=new CountDownLatch(8); //2 lus, 2 gsa, 2 gsms, 2 gscs
		final CountDownLatch removedVMLatch=new CountDownLatch(4);
		admin.getVirtualMachines().addLifecycleListener(new VirtualMachineLifecycleEventListener(){

			public void virtualMachineAdded(VirtualMachine virtualMachine) {
				System.out.println(virtualMachine.getUid());
				addedVMLatch.countDown();
				
			}

			public void virtualMachineRemoved(VirtualMachine virtualMachine) {
				removedVMLatch.countDown();
			}			
		});
		loadGS();
		assertTrue("failed to receive virtualMachineAdded notification",addedVMLatch.await(60, TimeUnit.SECONDS));
		testCleanup();
		assertTrue("failed to receive virtualMachineRemoved notification",removedVMLatch.await(60, TimeUnit.SECONDS));
	}

}
