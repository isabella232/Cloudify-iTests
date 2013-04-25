package org.cloudifysource.quality.iTests.test.esm.datagrid.eager;

import junit.framework.Assert;
import org.apache.commons.io.FileUtils;
import org.cloudifysource.esc.driver.provisioning.byon.ByonProvisioningDriver;
import iTests.framework.tools.SGTestHelper;
import org.cloudifysource.quality.iTests.framework.utils.DeploymentUtils;
import org.cloudifysource.quality.iTests.framework.utils.GsmTestUtils;
import org.cloudifysource.quality.iTests.framework.utils.IOUtils;
import org.cloudifysource.quality.iTests.test.esm.AbstractFromXenToByonGSMTest;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.elastic.config.DiscoveredMachineProvisioningConfigurer;
import org.openspaces.admin.pu.elastic.config.EagerScaleConfig;
import org.openspaces.admin.pu.elastic.config.EagerScaleConfigurer;
import org.openspaces.admin.space.ElasticSpaceDeployment;
import org.openspaces.core.util.MemoryUnit;
import org.testng.annotations.*;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class DedicatedEagerDataGridTwoIndependentDeploymentsByonTest extends AbstractFromXenToByonGSMTest {

    @BeforeMethod
    public void beforeTest() {
        super.beforeTestInit();
    }

    @BeforeClass
    protected void bootstrap() throws Exception {
        super.bootstrap();
    }

    @AfterMethod
    public void afterTest() {
        super.afterTest();
    }

    @AfterClass(alwaysRun = true)
    protected void teardownAfterClass() throws Exception {
        super.teardownAfterClass();
    }
    private static final String CLOUD_PREFIX = "__cloud.zone.";
    private static final String ZONE1 = "zone1";
    private static final String ZONE2 = "zone2";

    @Test(timeOut=DEFAULT_TEST_TIMEOUT, groups="1")
    public void testElasticSpaceDeployment() throws Exception {

        String[] zones1 = new String[] { CLOUD_PREFIX+ZONE1};
        String[] zones2 = new String[] { CLOUD_PREFIX+ZONE2};
        //TODO make start machines with zones be concurrent
        GridServiceAgent[] agents = new GridServiceAgent[]
                {startNewByonMachineWithZones(getElasticMachineProvisioningCloudifyAdapter(), zones1, OPERATION_TIMEOUT, TimeUnit.MILLISECONDS),
                 startNewByonMachineWithZones(getElasticMachineProvisioningCloudifyAdapter(), zones1, OPERATION_TIMEOUT, TimeUnit.MILLISECONDS),
                 startNewByonMachineWithZones(getElasticMachineProvisioningCloudifyAdapter(), zones2, OPERATION_TIMEOUT,  TimeUnit.MILLISECONDS),
                 startNewByonMachineWithZones(getElasticMachineProvisioningCloudifyAdapter(), zones2, OPERATION_TIMEOUT,  TimeUnit.MILLISECONDS)};

        final int containerCapacityInMB = 250;
        ProcessingUnit pu1 = super.deploy(
                new ElasticSpaceDeployment("eagerspace1")
                        .maxMemoryCapacity(2*containerCapacityInMB, MemoryUnit.MEGABYTES)
                        .memoryCapacityPerContainer(containerCapacityInMB, MemoryUnit.MEGABYTES)
                        .dedicatedMachineProvisioning(
                                new DiscoveredMachineProvisioningConfigurer()
                                        .addGridServiceAgentZone(CLOUD_PREFIX+ZONE1)
                                        .dedicatedManagementMachines()
                                        .create())

                        .scale(new EagerScaleConfigurer()
                                .create())
        );

        GsmTestUtils.waitForScaleToComplete(pu1, 2, 2, OPERATION_TIMEOUT);

        assertNumberOfContainersOnMachine(agents[0],1);
        assertNumberOfContainersOnMachine(agents[1],1);
        assertNumberOfContainersOnMachine(agents[2],0);
        assertNumberOfContainersOnMachine(agents[3],0);
        Assert.assertEquals(1, countProcessingUnitInstancesOnMachine(agents[0], pu1));
        Assert.assertEquals(1, countProcessingUnitInstancesOnMachine(agents[1], pu1));
        Assert.assertEquals(0, countProcessingUnitInstancesOnMachine(agents[2], pu1));
        Assert.assertEquals(0, countProcessingUnitInstancesOnMachine(agents[3], pu1));


        ProcessingUnit pu2 = super.deploy(
                new ElasticSpaceDeployment("eagerspace2")
                        .maxMemoryCapacity(2*containerCapacityInMB, MemoryUnit.MEGABYTES)
                        .memoryCapacityPerContainer(containerCapacityInMB, MemoryUnit.MEGABYTES)
                        .dedicatedMachineProvisioning(
                                new DiscoveredMachineProvisioningConfigurer()
                                        .dedicatedManagementMachines()
                                        .addGridServiceAgentZone(CLOUD_PREFIX+ZONE2)
                                        .create())
                        .scale(new EagerScaleConfig())
        );

        GsmTestUtils.waitForScaleToComplete(pu2, 2, 2, OPERATION_TIMEOUT);

        assertNumberOfContainersOnMachine(agents[0],1);
        assertNumberOfContainersOnMachine(agents[1],1);
        assertNumberOfContainersOnMachine(agents[2],1);
        assertNumberOfContainersOnMachine(agents[3],1);
        Assert.assertEquals(1, countProcessingUnitInstancesOnMachine(agents[0], pu1));
        Assert.assertEquals(1, countProcessingUnitInstancesOnMachine(agents[1], pu1));
        Assert.assertEquals(0, countProcessingUnitInstancesOnMachine(agents[2], pu1));
        Assert.assertEquals(0, countProcessingUnitInstancesOnMachine(agents[3], pu1));
        Assert.assertEquals(0, countProcessingUnitInstancesOnMachine(agents[0], pu2));
        Assert.assertEquals(0, countProcessingUnitInstancesOnMachine(agents[1], pu2));
        Assert.assertEquals(1, countProcessingUnitInstancesOnMachine(agents[2], pu2));
        Assert.assertEquals(1, countProcessingUnitInstancesOnMachine(agents[3], pu2));

        assertUndeployAndWait(pu1);
        assertUndeployAndWait(pu2);
    }

    private void assertNumberOfContainersOnMachine(GridServiceAgent gsa, int expected) {
        int actual = countGridServiceContainersOnMachine(gsa);
        String message =
                "Wrong number of containers on machine " + gsa.getMachine().getHostAddress() + " " +
                        "expected "+ expected + " "+
                        "actual " + actual;
        Assert.assertEquals(message, expected, actual);
    }

    int countProcessingUnitInstancesOnMachine(GridServiceAgent agent, ProcessingUnit pu) {
        return agent.getMachine().getProcessingUnitInstances(pu.getName()).length;
    }

    int countGridServiceContainersOnMachine(GridServiceAgent agent) {
        return agent.getMachine().getGridServiceContainers().getSize();
    }

    @Override
    public void beforeBootstrap() throws IOException {
        String s = System.getProperty("file.separator");
        String repoQualityItests = DeploymentUtils.getQualityItestsPath(s);
        // copy custom location aware driver to cloudify-overrides
        File locationAwareDriver = new File (repoQualityItests +s+"location-aware-provisioning-byon"+s+"1.0-SNAPSHOT"+s+"location-aware-provisioning-byon-1.0-SNAPSHOT.jar");
        File uploadOverrides =
                new File(getService().getPathToCloudFolder() + "/upload/cloudify-overrides/");
        if (!uploadOverrides.exists()) {
            uploadOverrides.mkdir();
        }

        File uploadEsmDir = new File(uploadOverrides.getAbsoluteFile() + "/lib/platform/esm");
        File localEsmFolder = new File(SGTestHelper.getBuildDir() + "/lib/platform/esm");

        FileUtils.copyFileToDirectory(locationAwareDriver, uploadEsmDir, true);
        FileUtils.copyFileToDirectory(locationAwareDriver, localEsmFolder, false);

        final Map<String, String> propsToReplace = new HashMap<String, String>();

        final String oldCloudDriverClazz = ByonProvisioningDriver.class.getName();
        String newCloudDriverClazz = "org.cloudifysource.quality.iTests.BasicLocationAwareByonProvisioningDriver" ;

        propsToReplace.put(toClassName(oldCloudDriverClazz),toClassName(newCloudDriverClazz));
        IOUtils.replaceTextInFile(getService().getPathToCloudGroovy(), propsToReplace);
    }

    public String toClassName(String className) {
        return "className \""+className+"\"";
    }
}