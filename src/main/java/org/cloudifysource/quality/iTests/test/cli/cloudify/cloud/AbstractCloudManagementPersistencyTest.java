package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud;

import java.net.URL;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.cloudifysource.quality.iTests.framework.tools.SGTestHelper;
import org.cloudifysource.quality.iTests.framework.utils.AssertUtils;
import org.cloudifysource.quality.iTests.framework.utils.Bootstrapper;
import org.cloudifysource.quality.iTests.framework.utils.CloudBootstrapper;
import org.cloudifysource.quality.iTests.framework.utils.JCloudsUtils;
import org.cloudifysource.quality.iTests.framework.utils.LogUtils;
import org.cloudifysource.restclient.GSRestClient;
import org.cloudifysource.restclient.RestException;
import org.jclouds.compute.domain.NodeMetadata;

import com.j_spaces.kernel.PlatformVersion;

/**
 * User: nirb
 * Date: 06/03/13
 */
public abstract class AbstractCloudManagementPersistencyTest extends NewAbstractCloudTest{

    protected static final String TOMCAT_SERVICE_PATH = SGTestHelper.getBuildDir() + "/recipes/services/tomcat";
    protected static final String TOMCAT_SERVICE_NAME = "tomcat";
    protected static final String APPLICATION_NAME = "default";
    protected final static String SERVICE_REST_URL = "ProcessingUnits/Names/" + APPLICATION_NAME + "." + TOMCAT_SERVICE_NAME;

    private int numOfManagementMachines = 2;
    private final int numOfServiceInstances = 3;

    private List<String> attributesList = new LinkedList<String>();

    /**
     * 1. Shutdown management machines.
     * 2. Bootstrap using the persistence file.
     * 3. Retrieve attributes from space and compare with the ones before the shutdown.
     * 4. Shutdown an instance agent and wait for recovery.
     * @throws Exception
     */
    public void testManagementPersistency() throws Exception {

        shutdownManagement();

        CloudBootstrapper bootstrapper = getService().getBootstrapper();
        bootstrapper.scanForLeakedNodes(false);
        bootstrapper.useExisting(true);
        bootstrapper.bootstrap();
        bootstrapper.setRestUrl(getRestUrl());

        List<String> newAttributesList = new LinkedList<String>();

        for(int i=1; i <= numOfServiceInstances; i++){
            String attributes = bootstrapper.listServiceInstanceAttributes(APPLICATION_NAME, TOMCAT_SERVICE_NAME, i, false);
            newAttributesList.add(attributes.substring(attributes.indexOf("home")));
        }

        List<String> differenceAttributesList = new LinkedList<String>(attributesList);
        differenceAttributesList.removeAll(newAttributesList);

        AssertUtils.assertTrue("the service attributes post management restart are not the same as the attributes pre restart", differenceAttributesList.isEmpty());

        LogUtils.log("shutting down one of the service's GSAs");

        JCloudsUtils.createContext(getService());
        Set<? extends NodeMetadata> machines = JCloudsUtils.getAllRunningNodes();
        String agentServerId = "no agent server found";

        for(NodeMetadata node : machines){
            if(node.getName() != null && !node.getName().isEmpty() && node.getName().contains(getService().getMachinePrefix()) && node.getName().contains("agent")){
                agentServerId = node.getId();
                break;
            }
        }

        JCloudsUtils.shutdownServer(agentServerId);
        JCloudsUtils.closeContext();

        LogUtils.log("waiting for service to restart on a new machine");
        final GSRestClient client = new GSRestClient("", "", new URL(getRestUrl()), PlatformVersion.getVersionNumber());

        LogUtils.log("waiting for service to break due to machine shutdown");
        AssertUtils.repetitiveAssertTrue("service didn't break", new AssertUtils.RepetitiveConditionProvider() {
            @Override
            public boolean getCondition() {
                try {
                    int numOfInst = Integer.parseInt((String) client.getAdminData(SERVICE_REST_URL).get("NumberOfInstances"));
                    return numOfServiceInstances > numOfInst;
                } catch (RestException e) {
                    throw new RuntimeException("caught a RestException", e);
                }

            }
        } , OPERATION_TIMEOUT*4);

        AssertUtils.repetitiveAssertTrue("service didn't recover", new AssertUtils.RepetitiveConditionProvider() {
            @Override
            public boolean getCondition() {
                try {
                    int numOfInst = Integer.parseInt((String) client.getAdminData(SERVICE_REST_URL).get("NumberOfInstances"));
                    return numOfServiceInstances == numOfInst;
/*

not for GA
                    int numOfPlannedInst = Integer.parseInt((String) client.getAdminData(SERVICE_REST_URL).get("PlannedNumberOfInstances"));
                    result = result && numOfServiceInstances == numOfPlannedInst;
*/
                } catch (RestException e) {
                    throw new RuntimeException("caught a RestException", e);
                }
            }
        } , OPERATION_TIMEOUT*3);
    }

    /**
     * 1. Shutdown management machines.
     * 2. Bootstrap without persistence file. (Only for DefaultProvisioningDriver)
     * 3. Check management machines are the same.
     * 4. repeat 1-3, 4 times.
     * @throws Exception
     */
    protected void testRepetitiveShutdownManagersBootstrap() throws Exception {

        // retrieve the rest url's before we start the chaos.
        final Set<String> originalRestUrls = toSet(getService().getRestUrls());

        int repetitions = 4;

        for(int i=0; i < repetitions; i++){

            shutdownManagement();

            CloudBootstrapper bootstrapper = getService().getBootstrapper();
            bootstrapper.scanForLeakedNodes(false);
            bootstrapper.useExisting(true);
            bootstrapper.bootstrap();

            String output = bootstrapper.getLastActionOutput();

            AssertUtils.assertTrue("bootstrap failed", output.contains("Successfully created Cloudify Manager"));

            // check the rest urls are the same;
            final Set<String> newRestUrls = new HashSet<String>();
            for (URL url : getService().getBootstrapper().getRestAdminUrls()) {
                newRestUrls.add(url.toString());
            }
            AssertUtils.assertEquals("Expected rest url's not to change after re-bootstrapping", originalRestUrls, newRestUrls);
        }
    }

    public void boostrapAndInstallService() throws Exception{
        super.bootstrap();
        super.installServiceAndWait(TOMCAT_SERVICE_PATH, TOMCAT_SERVICE_NAME, SERVICE_INSTALLATION_TIMEOUT_IN_MINUTES, numOfServiceInstances);

        Bootstrapper bootstrapper = new CloudBootstrapper();
        bootstrapper.setRestUrl(getRestUrl());

        for(int i=1; i <= numOfServiceInstances; i++){
            String attributes = bootstrapper.listServiceInstanceAttributes(APPLICATION_NAME, TOMCAT_SERVICE_NAME, i, false);
            attributesList.add(attributes.substring(attributes.indexOf("home")));
        }
    }

    protected void shutdownManagement() throws Exception{

        CloudBootstrapper bootstrapper = new CloudBootstrapper();
        bootstrapper.setRestUrl(getRestUrl());

        LogUtils.log("shutting down managers");
        bootstrapper.shutdownManagers(APPLICATION_NAME, false);
    }

    private Set<String> toSet(final String[] array) {
        final Set<String> set = new HashSet<String>();
        for (String s : array) {
            set.add(s);
        }
        return set;
    }

    @Override
    protected void customizeCloud() throws Exception {
        super.customizeCloud();
        getService().setNumberOfManagementMachines(numOfManagementMachines);
        getService().getProperties().put("persistencePath", "/home/ec2-user/persistence");
    }

    @Override
    protected abstract String getCloudName();

    @Override
    protected abstract boolean isReusableCloud();

    public int getNumOfManagementMachines() {
        return numOfManagementMachines;
    }
}
