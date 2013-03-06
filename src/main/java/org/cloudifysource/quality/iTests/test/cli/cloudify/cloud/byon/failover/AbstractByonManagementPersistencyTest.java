package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.byon.failover;

import com.j_spaces.kernel.PlatformVersion;
import org.apache.commons.io.FileUtils;
import org.cloudifysource.quality.iTests.framework.tools.SGTestHelper;
import org.cloudifysource.quality.iTests.framework.utils.*;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.byon.AbstractByonCloudTest;
import org.cloudifysource.restclient.GSRestClient;
import org.cloudifysource.restclient.RestException;

import java.io.File;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 *
 * User: nirb, sagib
 * Date: 05/03/13
 *
 **/

public abstract class AbstractByonManagementPersistencyTest extends AbstractByonCloudTest{

    private static final String TOMCAT_SERVICE_PATH = SGTestHelper.getBuildDir() + "/recipes/services/tomcat";
    private static final String TOMCAT_SERVICE_NAME = "tomcat";
    protected static final String BACKUP_FILE_PATH = SGTestHelper.getBuildDir() + "/backup-details.txt";
    public static final String BOOTSTRAP_SUCCEEDED_STRING = "Successfully created Cloudify Manager";
    public static final String APPLICATION_NAME = "default";
    private final static String SERVICE_REST_URL = "ProcessingUnits/Names/" + APPLICATION_NAME + "." + TOMCAT_SERVICE_NAME;

    private int numOfManagementMachines = 2;
    private int numOfServiceInstances = 0;

    private List<String> attributesList = new LinkedList<String>();

    public void prepareTest() throws Exception {

        super.bootstrap();
        super.installServiceAndWait(TOMCAT_SERVICE_PATH, TOMCAT_SERVICE_NAME, SERVICE_INSTALLATION_TIMEOUT_IN_MINUTES, numOfServiceInstances);

        Bootstrapper bootstrapper = new CloudBootstrapper();
        bootstrapper.setRestUrl(getRestUrl());

        for(int i=1; i <= numOfServiceInstances; i++){
            String attributes = bootstrapper.listServiceInstanceAttributes("default", TOMCAT_SERVICE_NAME, i, false);
            attributesList.add(attributes);
        }

    }

    public void afterTest() throws Exception {
        super.teardown();
        FileUtils.deleteQuietly(new File(BACKUP_FILE_PATH));
    }

    public void shutdownManagement() throws Exception{

        CloudBootstrapper bootstrapper = new CloudBootstrapper();
        bootstrapper.setRestUrl(getRestUrl());

        LogUtils.log("shutting down managers");
        bootstrapper.shutdownManagers("default", BACKUP_FILE_PATH, false);
    }

    public void testManagementPersistency() throws Exception{

        shutdownManagement();

        CloudBootstrapper bootstrapper = new CloudBootstrapper();
        bootstrapper.useExistingFilePath(BACKUP_FILE_PATH);
        super.bootstrap(bootstrapper);

        List<String> newAttributesList = new LinkedList<String>();

        for(int i=1; i <= numOfServiceInstances; i++){
            String attributes = bootstrapper.listServiceInstanceAttributes(APPLICATION_NAME, TOMCAT_SERVICE_NAME, i, false);
            newAttributesList.add(attributes);
        }

        List<String> differenceAttributesList = new LinkedList<String>(attributesList);
        differenceAttributesList.removeAll(newAttributesList);

        AssertUtils.assertTrue("the service attributes post management restart are not the same as the attributes pre restart", differenceAttributesList.isEmpty());

        LogUtils.log("shutting down one of the service's GSAs");
        admin.getProcessingUnits().waitFor(APPLICATION_NAME + "." + TOMCAT_SERVICE_NAME);
        admin.getProcessingUnits().getProcessingUnit(APPLICATION_NAME + "." + TOMCAT_SERVICE_NAME).getInstances()[0].getGridServiceContainer().getGridServiceAgent().shutdown();

        LogUtils.log("waiting for service to restart on a new machine");
        final GSRestClient client = new GSRestClient("", "", new URL(getRestUrl()), PlatformVersion.getVersionNumber());
        final AtomicReference<String> atomicServiceStatus = new AtomicReference<String>();

        AssertUtils.repetitiveAssertTrue("service didn't recover", new AssertUtils.RepetitiveConditionProvider() {
            @Override
            public boolean getCondition() {
                String intactString = "intact";
                try {
                    atomicServiceStatus.set((String) client.getAdminData(SERVICE_REST_URL).get("Status-Enumerator"));
                } catch (RestException e) {
                    throw new RuntimeException("caught a RestException", e);
                }
                return intactString.equalsIgnoreCase(atomicServiceStatus.get());
            }
        } , OPERATION_TIMEOUT*3);
    }

    public void testBadPersistencyFile() throws Exception {

        shutdownManagement();

        IOUtils.replaceTextInFile(BACKUP_FILE_PATH, "instanceId", "instnceId");

        CloudBootstrapper bootstrapper = new CloudBootstrapper();
        bootstrapper.useExistingFilePath(BACKUP_FILE_PATH);
        bootstrapper.setBootstrapExpectedToFail(true);
        super.bootstrap(bootstrapper);

        String output = bootstrapper.getLastActionOutput();

        AssertUtils.assertTrue("bootstrap succeeded with a defective persistency file", !output.contains(BOOTSTRAP_SUCCEEDED_STRING));
    }


    @Override
    protected void customizeCloud() throws Exception {
        super.customizeCloud();
        getService().setNumberOfManagementMachines(numOfManagementMachines);
    }
}
