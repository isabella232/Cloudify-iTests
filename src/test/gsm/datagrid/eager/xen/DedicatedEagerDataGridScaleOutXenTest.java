package test.gsm.datagrid.eager.xen;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.elastic.config.DiscoveredMachineProvisioningConfigurer;
import org.openspaces.admin.pu.elastic.config.EagerScaleConfig;
import org.openspaces.admin.space.ElasticSpaceDeployment;
import org.openspaces.core.util.MemoryUnit;
import org.testng.annotations.Test;

import test.gsm.AbstractXenGSMTest;
import test.gsm.GsmTestUtils;
import test.utils.AssertUtils;
import test.utils.LogUtils;
import test.utils.AssertUtils.RepetitiveConditionProvider;

public class DedicatedEagerDataGridScaleOutXenTest extends AbstractXenGSMTest {

		@Test(timeOut=DEFAULT_TEST_TIMEOUT, groups="1")
		public void testElasticDataGridGracefulScaleOut() {
			
			startNewVM(OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
	        
			final int containerCapacityInMB = 256;
			int numberOfContainers = 4;
			ProcessingUnit pu = gsm.deploy(
					new ElasticSpaceDeployment("eagerspace")
					.maxMemoryCapacity(numberOfContainers*containerCapacityInMB, MemoryUnit.MEGABYTES)
					.memoryCapacityPerContainer(containerCapacityInMB, MemoryUnit.MEGABYTES)
					.dedicatedMachineProvisioning(
						new DiscoveredMachineProvisioningConfigurer()
						.reservedMemoryCapacityPerMachine(128, MemoryUnit.MEGABYTES)
						.create())
				    .scale(new EagerScaleConfig())
			);
			
			GsmTestUtils.waitForScaleToComplete(pu, numberOfContainers, 2, OPERATION_TIMEOUT);
			waitUntilAtLeastOneContainerPerMachine();
			
			int numberOfObjects = 1000;
			GsmTestUtils.writeData(pu, numberOfObjects);
			
			// start first VM and wait
			startNewVM(OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
            waitUntilContainersRemovedEquals(1);
			GsmTestUtils.waitForScaleToComplete(pu, numberOfContainers, 3, OPERATION_TIMEOUT);
			waitUntilAtLeastOneContainerPerMachine();
			GsmTestUtils.writeData(pu, numberOfObjects, numberOfObjects*2);		
			
			// start second VM
			startNewVM(OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
            waitUntilContainersRemovedEquals(2);
			GsmTestUtils.waitForScaleToComplete(pu, numberOfContainers, 4, OPERATION_TIMEOUT);	
           
			waitUntilAtLeastOneContainerPerMachine();
			GsmTestUtils.writeData(pu, numberOfObjects, numberOfObjects*3);
		}

        private void waitUntilContainersRemovedEquals(final int numberOfContainersRemoved) {
            super.repetitiveAssertTrue(
                    "waiting for a container to be removed (meaning esm detected the new machine)", 
                    new RepetitiveConditionProvider() {
                        
                        public boolean getCondition() {
                            return getNumberOfGSCsRemoved() == numberOfContainersRemoved;
                        }
                    }, 
                    OPERATION_TIMEOUT);
        }
		
		@Test(timeOut=DEFAULT_TEST_TIMEOUT, groups={"1", "xen"})
		public void testElasticDataGridFastScaleOut() throws InterruptedException {
			
			startNewVM(OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
	        
			final int containerCapacityInMB = 256;
			int numberOfContainers = 4;
			ProcessingUnit pu = gsm.deploy(
					new ElasticSpaceDeployment("eagerspace")
					.maxMemoryCapacity(numberOfContainers*containerCapacityInMB, MemoryUnit.MEGABYTES)
					.memoryCapacityPerContainer(containerCapacityInMB, MemoryUnit.MEGABYTES)
					.dedicatedMachineProvisioning(
						new DiscoveredMachineProvisioningConfigurer()
						.reservedMemoryCapacityPerMachine(128, MemoryUnit.MEGABYTES)
						.create())
				    .scale(new EagerScaleConfig())
			);
			
			GsmTestUtils.waitForScaleToComplete(pu, numberOfContainers, 2, OPERATION_TIMEOUT);
			waitUntilAtLeastOneContainerPerMachine();
			
			int numberOfObjects = 1000;
			GsmTestUtils.writeData(pu, numberOfObjects);
			
			//start 2 machines concurrently
			final CountDownLatch latch = new CountDownLatch(2);
			new Thread(new Runnable() {

                public void run() {
                    startNewVM(OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
                    latch.countDown();
                }
            }).start();
			startNewVM(OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
			latch.countDown();
			latch.await();
			GsmTestUtils.waitForScaleToComplete(pu, numberOfContainers, 4, OPERATION_TIMEOUT);
			waitUntilAtLeastOneContainerPerMachine();
			assertEquals(numberOfObjects,GsmTestUtils.countData(pu));
		}

		private void waitUntilAtLeastOneContainerPerMachine() {
			
			final RepetitiveConditionProvider condition = new RepetitiveConditionProvider() {
				String lastMessage = "";
				public boolean getCondition() {
					
					boolean oneContainerPerMachine = true;
					String message = "";
					
					for (GridServiceAgent gsa : admin.getGridServiceAgents()) {
						int containersOnMachine = gsa.getMachine().getGridServiceContainers().getSize();
						if(containersOnMachine==0) {
							message = message + " " + gsa.getMachine().getHostName();
							oneContainerPerMachine = false;
						}
					}
					if (!oneContainerPerMachine) {
						message = "Waiting until the following machines have at least one container: " + message;
					
						if (!lastMessage.equals(message)) {
							LogUtils.log(message);
							lastMessage = message;
						}
					}
					return oneContainerPerMachine;

				}
			};
			
			AssertUtils.repetitiveAssertTrue("Waiting for container deployment to complete",
					condition, OPERATION_TIMEOUT);
		}
	

}
