package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud;

import com.j_spaces.kernel.PlatformVersion;
import org.cloudifysource.quality.iTests.framework.tools.SGTestHelper;
import org.cloudifysource.quality.iTests.framework.utils.*;
import org.cloudifysource.restclient.GSRestClient;
import org.cloudifysource.restclient.RestException;
import org.jclouds.compute.domain.NodeMetadata;

import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

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
    private int numOfServiceInstances = 3;

    private List<String> attributesList = new LinkedList<String>();

    public void bootstrapAndInit() throws Exception{
        super.bootstrap();
        super.installServiceAndWait(TOMCAT_SERVICE_PATH, TOMCAT_SERVICE_NAME, SERVICE_INSTALLATION_TIMEOUT_IN_MINUTES, numOfServiceInstances);

        Bootstrapper bootstrapper = new CloudBootstrapper();
        bootstrapper.setRestUrl(getRestUrl());

        for(int i=1; i <= numOfServiceInstances; i++){
            String attributes = bootstrapper.listServiceInstanceAttributes(APPLICATION_NAME, TOMCAT_SERVICE_NAME, i, false);
            attributesList.add(attributes.substring(attributes.indexOf("home")));
        }
    }

    public void afterTest() throws Exception{
        super.teardown();
//        FileUtils.deleteQuietly(new File(backupFilePath));
    }

    protected void shutdownManagement() throws Exception{

        CloudBootstrapper bootstrapper = new CloudBootstrapper();
        bootstrapper.setRestUrl(getRestUrl());

        LogUtils.log("shutting down managers");
        bootstrapper.shutdownManagers(APPLICATION_NAME, false);
    }

    public void testManagementPersistency() throws Exception {

//        shutdownManagement();
//
//        CloudBootstrapper bootstrapper = new CloudBootstrapper();
//        bootstrapper.scanForLeakedNodes(false);
//        bootstrapper.useExisting(true);
//        super.bootstrap(bootstrapper);
//        bootstrapper.setRestUrl(getRestUrl());
//
//        List<String> newAttributesList = new LinkedList<String>();
//
//        for(int i=1; i <= numOfServiceInstances; i++){
//            String attributes = bootstrapper.listServiceInstanceAttributes(APPLICATION_NAME, TOMCAT_SERVICE_NAME, i, false);
//            newAttributesList.add(attributes.substring(attributes.indexOf("home")));
//        }
//
//        List<String> differenceAttributesList = new LinkedList<String>(attributesList);
//        differenceAttributesList.removeAll(newAttributesList);
//
//        AssertUtils.assertTrue("the service attributes post management restart are not the same as the attributes pre restart", differenceAttributesList.isEmpty());

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
        final AtomicReference<Integer> atomicNumOfInstances = new AtomicReference<Integer>();

        LogUtils.log("waiting for service to break due to machine shutdown");
        AssertUtils.repetitiveAssertTrue("service didn't break", new AssertUtils.RepetitiveConditionProvider() {
            @Override
            public boolean getCondition() {
                try {
                    String numOfInstString = (String) client.getAdminData(SERVICE_REST_URL).get("NumberOfInstances");
                    atomicNumOfInstances.set(Integer.parseInt(numOfInstString));
                } catch (RestException e) {
                    throw new RuntimeException("caught a RestException", e);
                }
                return numOfServiceInstances > atomicNumOfInstances.get();
            }
        } , OPERATION_TIMEOUT*4);

        AssertUtils.repetitiveAssertTrue("service didn't recover", new AssertUtils.RepetitiveConditionProvider() {
            @Override
            public boolean getCondition() {
                boolean result = false;
                try {
                    String numOfInstString = (String) client.getAdminData(SERVICE_REST_URL).get("NumberOfInstances");
                    atomicNumOfInstances.set(Integer.parseInt(numOfInstString));
                    result = numOfServiceInstances == atomicNumOfInstances.get();
                    numOfInstString = (String) client.getAdminData(SERVICE_REST_URL).get("PlannedNumberOfInstances");
                    atomicNumOfInstances.set(Integer.parseInt(numOfInstString));
                    result = result && numOfServiceInstances == atomicNumOfInstances.get();
                } catch (RestException e) {
                    throw new RuntimeException("caught a RestException", e);
                }
                return result;
            }
        } , OPERATION_TIMEOUT*3);
    }

    public void testRepetitiveShutdownManagersBootstrap() throws Exception {

        int repetitions = 4;
        String output = "";

        for(int i=0; i < repetitions; i++){

            shutdownManagement();

            CloudBootstrapper bootstrapper = new CloudBootstrapper();
            bootstrapper.scanForLeakedNodes(false);
            bootstrapper.useExisting(true);
            super.bootstrap(bootstrapper);

            output = bootstrapper.getLastActionOutput();

            AssertUtils.assertTrue("bootstrap failed", output.contains("Successfully created Cloudify Manager"));

        }
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
