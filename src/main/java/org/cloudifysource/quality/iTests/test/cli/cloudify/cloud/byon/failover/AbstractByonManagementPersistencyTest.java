package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.byon.failover;

import org.apache.commons.io.FileUtils;
import org.cloudifysource.quality.iTests.framework.tools.SGTestHelper;
import org.cloudifysource.quality.iTests.framework.utils.*;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.byon.AbstractByonCloudTest;
import org.openspaces.admin.pu.ProcessingUnit;

import java.io.File;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 *
 * User: nirb, sagib
 * Date: 05/03/13
 *
 **/

public abstract class AbstractByonManagementPersistencyTest extends AbstractByonCloudTest{

    private static final String TOMCAT_SERVICE_PATH = SGTestHelper.getBuildDir() + "/recipes/services/tomcat";
    private static final String TOMCAT_SERVICE_NAME = "tomcat";
    public static final String BOOTSTRAP_SUCCEEDED_STRING = "Successfully created Cloudify Manager";
    public static final String APPLICATION_NAME = "default";
    private final static String SERVICE_REST_URL = "ProcessingUnits/Names/" + APPLICATION_NAME + "." + TOMCAT_SERVICE_NAME;
    protected String backupFilePath = SGTestHelper.getBuildDir() + "/backup-details.txt";

    private int numOfManagementMachines = 2;
    private int numOfServiceInstances = 2;

    private List<String> attributesList = new LinkedList<String>();

    public void prepareTest() throws Exception {


        super.bootstrap();
        super.installServiceAndWait(TOMCAT_SERVICE_PATH, TOMCAT_SERVICE_NAME, SERVICE_INSTALLATION_TIMEOUT_IN_MINUTES, numOfServiceInstances);

        Bootstrapper bootstrapper = new CloudBootstrapper();
        bootstrapper.setRestUrl(getRestUrl());
        attributesList = new LinkedList<String>();

        for(int i=1; i <= numOfServiceInstances; i++){
            String attributes = bootstrapper.listServiceInstanceAttributes(APPLICATION_NAME, TOMCAT_SERVICE_NAME, i, false);
            attributesList.add(attributes.substring(attributes.indexOf("home")));
        }

    }

    public void afterTest() throws Exception {
        super.teardown();
        FileUtils.deleteQuietly(new File(backupFilePath));
    }

    public void shutdownManagement() throws Exception{

        CloudBootstrapper bootstrapper = new CloudBootstrapper();
        bootstrapper.setRestUrl(getRestUrl());

        LogUtils.log("shutting down managers");
        bootstrapper.shutdownManagers("default", backupFilePath, false);
    }

    public void testManagementPersistency() throws Exception{

        shutdownManagement();

        CloudBootstrapper bootstrapper = new CloudBootstrapper();
        bootstrapper.useExistingFilePath(backupFilePath);
        bootstrapper.killJavaProcesses(false);
        super.bootstrap(bootstrapper);
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
        ProcessingUnit processingUnit = admin.getProcessingUnits().waitFor(APPLICATION_NAME + "." + TOMCAT_SERVICE_NAME);
        processingUnit.waitFor(numOfServiceInstances);
        processingUnit.getInstances()[0].getGridServiceContainer().getGridServiceAgent().shutdown();



        AssertUtils.repetitiveAssertTrue("service didn't break", new AssertUtils.RepetitiveConditionProvider() {
            @Override
            public boolean getCondition() {
                int totalNumberOfInstances = admin.getProcessingUnits().getProcessingUnit(APPLICATION_NAME + "." + TOMCAT_SERVICE_NAME).getTotalNumberOfInstances();
                LogUtils.log("Total number of instances after gsa shutdown : " + totalNumberOfInstances);
                return totalNumberOfInstances < numOfServiceInstances;
            }
        } , OPERATION_TIMEOUT*4);

        LogUtils.log("waiting for service to restart on a new machine");
        ProcessingUnit tomcat = admin.getProcessingUnits().getProcessingUnit(APPLICATION_NAME + "." + TOMCAT_SERVICE_NAME);
        AssertUtils.assertTrue("Timed out waiting for " + numOfServiceInstances + " instances of tomcat", tomcat.waitFor(numOfServiceInstances, OPERATION_TIMEOUT, TimeUnit.MILLISECONDS));
    }

    public void testBadPersistencyFile() throws Exception {

        shutdownManagement();

        IOUtils.replaceTextInFile(backupFilePath, "instanceId", "instnceId");

        CloudBootstrapper bootstrapper = new CloudBootstrapper();
        bootstrapper.useExistingFilePath(backupFilePath);
        bootstrapper.setBootstrapExpectedToFail(true);
        super.bootstrap(bootstrapper);

        String output = bootstrapper.getLastActionOutput();

        AssertUtils.assertTrue("bootstrap succeeded with a defective persistency file", !output.contains(BOOTSTRAP_SUCCEEDED_STRING));
    }


    public void testRepetitiveShutdownManagersBootstrap() throws Exception {

        // retrieve the rest url's before we start the chaos.
        final Set<String> originalRestUrls = new HashSet<String>();
        for (String url : getService().getRestUrls()) {
            originalRestUrls.add(url);
        }

        int repetitions = 4;

        for(int i=0; i < repetitions; i++){

            shutdownManagement();

            CloudBootstrapper bootstrapper = new CloudBootstrapper();
            bootstrapper.scanForLeakedNodes(false);
            bootstrapper.useExisting(true);
            super.bootstrap(bootstrapper);

            String output = bootstrapper.getLastActionOutput();

            AssertUtils.assertTrue("bootstrap failed", output.contains("Successfully created Cloudify Manager"));

            // check the rest urls are the same;
            final Set<String> newRestUrls = new HashSet<String>();
            for (String url : getService().getRestUrls()) {
                newRestUrls.add(url);
            }
            AssertUtils.assertEquals("Expected rest url's not to change after re-bootstrapping", originalRestUrls, newRestUrls);
        }
    }

    @Override
    protected void customizeCloud() throws Exception {
        super.customizeCloud();
        getService().setNumberOfManagementMachines(numOfManagementMachines);
        getService().getProperties().put("persistencePath", "/tmp/byon/persistency");

    }
}
