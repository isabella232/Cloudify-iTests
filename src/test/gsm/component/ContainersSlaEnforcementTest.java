package test.gsm.component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.openspaces.admin.Admin;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.internal.admin.InternalAdmin;
import org.openspaces.admin.internal.pu.elastic.GridServiceContainerConfig;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.openspaces.admin.space.SpaceDeployment;
import org.openspaces.grid.gsm.capacity.CapacityRequirements;
import org.openspaces.grid.gsm.capacity.ClusterCapacityRequirements;
import org.openspaces.grid.gsm.capacity.MemoryCapacityRequirement;
import org.openspaces.grid.gsm.containers.ContainersSlaEnforcement;
import org.openspaces.grid.gsm.containers.ContainersSlaEnforcementEndpoint;
import org.openspaces.grid.gsm.containers.ContainersSlaPolicy;
import org.openspaces.grid.gsm.containers.exceptions.ContainersSlaEnforcementInProgressException;
import org.openspaces.grid.gsm.containers.exceptions.ContainersSlaEnforcementPendingProcessingUnitDeallocationException;
import org.openspaces.grid.gsm.sla.exceptions.SlaEnforcementEndpointDestroyedException;
import org.openspaces.grid.gsm.sla.exceptions.SlaEnforcementException;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.gsm.AbstractGsmTest;
import test.utils.AdminUtils;
import test.utils.LogUtils;
import test.utils.ToStringUtils;

    /*
     * Tests the Containers SLA enforcement component
     *
     * Before running this test locally open a command prompt and run:
     * set LOOKUPGROUPS=itaif-laptop
     * set JSHOMEDIR=d:\eclipse_workspace3\SGTest\tools\gigaspaces
     * start cmd /c "%JSHOMEDIR%\bin\gs-agent.bat gsa.global.esm 0 gsa.gsc 0 gsa.global.gsm 0 gsa.global.lus 0 gsa.lus 2"
     *
     * on linux:
     * export LOOKUPGROUPS=itaif-laptop
     * export JSHOMEDIR=~/gigaspaces
     * nohup ${JSHOMEDIR}/bin/gs-agent.sh gsa.global.esm 0 gsa.gsc 0 gsa.global.gsm 0 gsa.global.lus 0 gsa.lus 2 &
     */

    public class ContainersSlaEnforcementTest  extends AbstractGsmTest {
        
        private final String zone = "testzone";
        private ProcessingUnit pu;
        
        private ContainersSlaEnforcementEndpoint endpoint;
        private ContainersSlaEnforcement containersSlaEnforcement;


        @Override
        protected Admin newAdmin() {
            return AdminUtils.createSingleThreadAdmin();
        }
        
        @Override
		@BeforeMethod
        public void beforeTest() {
            super.beforeTest();
            gsa.getMachine().getOperatingSystem().startStatisticsMonitor();
            //deploy data grid with maxpervm=0
            pu = gsm.deploy(new SpaceDeployment("myspace").addZone(zone).partitioned(1,1).maxInstancesPerVM(0).maxInstancesPerMachine(0));
            containersSlaEnforcement = new ContainersSlaEnforcement(admin);
            endpoint = containersSlaEnforcement.createEndpoint(pu);
        }

        
        @Override
		@AfterMethod
        public void afterTest() {
            containersSlaEnforcement.destroyEndpoint(pu);
            containersSlaEnforcement.destroy();
            gsa.getMachine().getOperatingSystem().stopStatisticsMonitor();
            super.afterTest();
        }
        
        @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
        public void oneContainerTest() throws InterruptedException {
            
            Assert.assertEquals(getNumberOfGSCsAdded()   ,0);
            Assert.assertEquals(getNumberOfGSCsRemoved() ,0);
            
            enforceNumberOfContainers(1);
            
            Assert.assertEquals(getNumberOfGSCsAdded()   ,1);
            Assert.assertEquals(getNumberOfGSCsRemoved() ,0);
            
            enforceUndeploy();
            
            Assert.assertEquals(getNumberOfGSCsAdded()   ,1);
            Assert.assertEquals(getNumberOfGSCsRemoved() ,1);
        }

        @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
        public void twoContainersTest() throws InterruptedException {
            
            Assert.assertEquals(getNumberOfGSCsAdded()   ,0);
            Assert.assertEquals(getNumberOfGSCsRemoved() ,0);
            
            enforceNumberOfContainers(2);
            
            Assert.assertEquals(getNumberOfGSCsAdded()   ,2);
            Assert.assertEquals(getNumberOfGSCsRemoved() ,0);
            
            enforceUndeploy();
                
            Assert.assertEquals(getNumberOfGSCsAdded()   ,2);
            Assert.assertEquals(getNumberOfGSCsRemoved() ,2);

        }
        
        @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "2", enabled = true)
        public void twoMachinesTest() throws InterruptedException {
            
            enforceTwoMachines();
            
            enforceUndeploy();
            
            Assert.assertEquals(getNumberOfGSCsAdded()   ,2);
            Assert.assertEquals(getNumberOfGSCsRemoved() ,2);
        }

        private void enforceTwoMachines() throws InterruptedException {
            admin.getGridServiceAgents().waitFor(2);
            
            final GridServiceAgent[] agents = admin.getGridServiceAgents().getAgents();
            final GridServiceAgent gsa1 = gsa;
            final GridServiceAgent gsa2 = gsa.equals(agents[1]) ? agents[0] : agents[1];
            
            Assert.assertEquals(getNumberOfGSCsAdded()   ,0);
            Assert.assertEquals(getNumberOfGSCsRemoved() ,0);
            
            final ContainersSlaPolicy sla = new ContainersSlaPolicy();
            
            final ClusterCapacityRequirements aggregatedCapacityRequirements = 
                            new ClusterCapacityRequirements()
                            .add(gsa1.getUid(),
                                 new CapacityRequirements(
                                        new MemoryCapacityRequirement(
                                        getContainerConfig().getMaximumJavaHeapSizeInMB())))
                            .add(gsa2.getUid(),
                            		new CapacityRequirements(
                                            new MemoryCapacityRequirement(
                                            getContainerConfig().getMaximumJavaHeapSizeInMB())));
            
            sla.setClusterCapacityRequirements(aggregatedCapacityRequirements);
            sla.setNewContainerConfig(getContainerConfig());
            enforceSlaAndWait(sla);
            
            Assert.assertEquals(getNumberOfGSCsAdded()   ,2);
            Assert.assertEquals(getNumberOfGSCsRemoved() ,0);
            final GridServiceContainer[] containers = admin.getGridServiceContainers().getContainers();
            Assert.assertEquals(containers.length,2);
            Assert.assertFalse(containers[0].getGridServiceAgent().equals(containers[1].getGridServiceAgent()));
        }
        
        @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
        public void scaleOutContainerTest() throws InterruptedException {
            
            Assert.assertEquals(getNumberOfGSCsAdded()   ,0);
            Assert.assertEquals(getNumberOfGSCsRemoved() ,0);
            
            enforceNumberOfContainers(1);
            
            Assert.assertEquals(getNumberOfGSCsAdded()   ,1);
            Assert.assertEquals(getNumberOfGSCsRemoved() ,0);
            
            enforceNumberOfContainers(2);
            
            Assert.assertEquals(getNumberOfGSCsAdded()   ,2);
            Assert.assertEquals(getNumberOfGSCsRemoved() ,0);
            
            enforceUndeploy();
            
            Assert.assertEquals(getNumberOfGSCsAdded()   ,2);
            Assert.assertEquals(getNumberOfGSCsRemoved() ,2);
        }
        
        /**
         * This tests starts with 2 machines, 1 container each, and scales into one machine with 2 containers.
         * @throws InterruptedException
         */
        @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "2", enabled = true)
        public void scaleInMachinesTest() throws InterruptedException {
            
            Assert.assertEquals(getNumberOfGSCsAdded()   ,0);
            Assert.assertEquals(getNumberOfGSCsRemoved() ,0);
            
            enforceTwoMachines();
            
            Assert.assertEquals(getNumberOfGSCsAdded()   ,2);
            Assert.assertEquals(getNumberOfGSCsRemoved() ,0);
            
            enforceNumberOfContainers(2);
            
            Assert.assertEquals(getNumberOfGSCsAdded()   ,3);
            Assert.assertEquals(getNumberOfGSCsRemoved() ,1);
            
            enforceUndeploy();
            
            Assert.assertEquals(getNumberOfGSCsAdded()   ,3);
            Assert.assertEquals(getNumberOfGSCsRemoved() ,3);
        }

        /**
         * This test deploys 2 containers on 1 machine. Then scales in to one container.
         * It then performs rebalancing to evacuate the container that is not "approved".
         * The expected result is that the containers component will terminate the empty container.
         * @throws InterruptedException
         */
        @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
        public void scaleInContainersWithProcessingUnitsTest() throws InterruptedException {
            
            Assert.assertEquals(getNumberOfGSCsAdded()   ,0);
            Assert.assertEquals(getNumberOfGSCsRemoved() ,0);
                
            enforceNumberOfContainers(2);
            
            Assert.assertEquals(getNumberOfGSCsAdded()   ,2);
            
            final GridServiceContainer[] containers = admin.getGridServiceContainers().getContainers();

            ProcessingUnit processingUnit = admin.getProcessingUnits().getProcessingUnit("myspace");
            processingUnit.waitFor(2);
            
            //scale in
            final ContainersSlaPolicy sla = createMemorySla(1);
            final AtomicBoolean evacuated = new AtomicBoolean(false);
            final CountDownLatch latch = new CountDownLatch(1);
            ScheduledFuture<?> scheduledTask = 
                ((InternalAdmin)admin).scheduleWithFixedDelayNonBlockingStateChange(
                new Runnable() {
    
                    public void run() {
                      try {
                        endpoint.enforceSla(sla);
                        latch.countDown();
                      }
                      catch (ContainersSlaEnforcementPendingProcessingUnitDeallocationException e) {
                        if (!evacuated.get()) {
                        try {
                        
                            Assert.assertEquals(getNumberOfGSCsAdded()   ,2);
                            Assert.assertEquals(getNumberOfGSCsRemoved() ,0);

                            // move the pu instances from the approved (source) 
                            // to the non-approved containers. 
                            
                            Assert.assertEquals(1, endpoint.getContainers().length);
							
                            
                            
                            int targetIndex = containers[0].equals(endpoint.getContainers()[0]) ? 0 : 1;
                            int sourceIndex = 1 - targetIndex;
                                                            
                            final GridServiceContainer source = containers[sourceIndex];
                            final GridServiceContainer target = containers[targetIndex];
                                                            
                            // relocate all pu instances from source to target
                            for (final ProcessingUnitInstance instance : source.getProcessingUnitInstances()) {
                                ((InternalAdmin)admin).scheduleAdminOperation(new Runnable() {
                                    public void run() {
                                        ProcessingUnitInstance relocatedInstance = instance.relocateAndWait(target);
                                        LogUtils.log("Relocated: "    + ToStringUtils.puInstanceToString(relocatedInstance)
                                        		+ " from container: " + ToStringUtils.gscToString(target)
                                        		+ " to container: "   + ToStringUtils.gscToString(relocatedInstance.getGridServiceContainer()));
                                    }
                                });
                            }
                            
                            evacuated.set(true);
                        } catch (SlaEnforcementEndpointDestroyedException e1) {
							AssertFail("endpoint destroyed unexpectedly", e1);
						}
                        }
                      }
                      catch (ContainersSlaEnforcementInProgressException e) {
  						//try again next time
  					  }
                      catch (Exception e) {
                          Assert.fail("enforce sla failed", e);
                      }
                    }
                    
                }, 
            
                0, 10, TimeUnit.SECONDS);
            
            try {
                latch.await();
            }
            finally {
                scheduledTask.cancel(false);
            }

            Assert.assertTrue(evacuated.get());
            Assert.assertEquals(getNumberOfGSCsAdded()   ,2);
            Assert.assertEquals(getNumberOfGSCsRemoved() ,1);
            
            enforceUndeploy();
            
            Assert.assertEquals(getNumberOfGSCsAdded()   ,2);
            Assert.assertEquals(getNumberOfGSCsRemoved() ,2);
        }
    
        private GridServiceContainerConfig getContainerConfig() {
            GridServiceContainerConfig containerConfig = new GridServiceContainerConfig(new HashMap<String,String>());
            containerConfig.addCommandLineArgument("-Xmx250m");
            containerConfig.addCommandLineArgument("-Xms250m");
            containerConfig.addCommandLineArgument("-Dcom.gs.zones="+this.zone);
            Assert.assertEquals(containerConfig.getMaximumJavaHeapSizeInMB(), 250);
            containerConfig.setMaximumMemoryCapacityInMB(containerConfig.getMaximumJavaHeapSizeInMB());
            return containerConfig;
        }
        
        @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", expectedExceptions=SlaEnforcementEndpointDestroyedException.class, enabled = true)
        void destroyTest() throws InterruptedException, SlaEnforcementException {
            this.containersSlaEnforcement.destroyEndpoint(pu);
            ContainersSlaPolicy createSla = createMemorySla(1);
            endpoint.enforceSla(createSla);
        }
        
        private void enforceNumberOfContainers(int numberOfContainers) throws InterruptedException {
            final ContainersSlaPolicy sla = createMemorySla(numberOfContainers);
            enforceSlaAndWait(sla);
        }
        
        private void enforceSlaAndWait(ContainersSlaPolicy sla) throws InterruptedException {
            SlaEnforcementTestUtils.enforceSlaAndWait(admin, endpoint, sla);
            try {
				Assert.assertEquals(toSet(admin.getGridServiceContainers().getContainers()),toSet(endpoint.getContainers()));
			} catch (SlaEnforcementEndpointDestroyedException e) {
				AssertFail("endpoint destroyed unexpectedly.", e);
			}
        }
        
        ContainersSlaPolicy createMemorySla(int numberOfContainers) {
            final ContainersSlaPolicy sla = new ContainersSlaPolicy();
            ClusterCapacityRequirements aggregatedCapacityRequirements = 
            new ClusterCapacityRequirements()
                    .add(gsa.getUid(),
                         new CapacityRequirements(
                                new MemoryCapacityRequirement(
                                numberOfContainers * getContainerConfig().getMaximumJavaHeapSizeInMB())));
            sla.setClusterCapacityRequirements(aggregatedCapacityRequirements);
            sla.setNewContainerConfig(getContainerConfig());
            return sla;
        }
        
        @SuppressWarnings("unchecked")
        Set<GridServiceContainer> toSet(GridServiceContainer[] containers) {
            return new HashSet<GridServiceContainer>(
                    Arrays.asList(containers));
        }
        

        private void enforceUndeploy() throws InterruptedException {
            pu.undeploy();
            final ContainersSlaPolicy sla = new ContainersSlaPolicy();
            sla.setClusterCapacityRequirements(new ClusterCapacityRequirements());
            sla.setNewContainerConfig(getContainerConfig());
            enforceSlaAndWait(sla);            
        }
    }
