package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.byon.persistence;

import org.apache.commons.io.FileUtils;
import org.cloudifysource.quality.iTests.framework.tools.SGTestHelper;
import org.cloudifysource.quality.iTests.framework.utils.*;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.byon.AbstractByonCloudTest;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.pu.ProcessingUnit;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;


/**
 *
 * User: nirb, sagib
 * Date: 05/03/13
 *
 **/

public abstract class AbstractByonManagementPersistencyTest extends AbstractByonCloudTest{

    private static final String TOMCAT_SERVICE_PATH = SGTestHelper.getBuildDir() + "/recipes/services/tomcat";
    private static final String TOMCAT_SERVICE_NAME = "tomcat";
    private static final String ACTIVEMQ_SERVICE_PATH = SGTestHelper.getBuildDir() + "/recipes/services/activemq";
    private static final String ACTIVEMQ_SERVICE_NAME = "activemq";
    public static final String BOOTSTRAP_SUCCEEDED_STRING = "Successfully created Cloudify Manager";
    public static final String APPLICATION_NAME = "default";
    public static final String USER_NAME = "tgrid";
    public static final String PASSWORD = "tgrid";
    protected String backupFilePath = SGTestHelper.getBuildDir() + "/backup-details.txt";

    private int numOfManagementMachines = 2;
    private int numOfServiceInstances = 2;
    private int numOfServices = 2;
    private boolean multipleServices = false;

    private List<String> attributesList = new LinkedList<String>();

    public void prepareTest() throws Exception {
        prepareTest(false);
    }

    public void prepareTest(boolean multipleServices) throws Exception {

        this.multipleServices = multipleServices;
        super.bootstrap();
        if(multipleServices){
            //TODO waiting for CLOUDIFY-1591
//            for(int i=1; i <= numOfServices; i++){
//                super.installServiceAndWait(TOMCAT_SERVICE_PATH, TOMCAT_SERVICE_NAME + "_" + i);
//            }
            super.installServiceAndWait(ACTIVEMQ_SERVICE_PATH, ACTIVEMQ_SERVICE_NAME, SERVICE_INSTALLATION_TIMEOUT_IN_MINUTES);
            super.installServiceAndWait(TOMCAT_SERVICE_PATH, TOMCAT_SERVICE_NAME, SERVICE_INSTALLATION_TIMEOUT_IN_MINUTES);

        }
        else{
            super.installServiceAndWait(TOMCAT_SERVICE_PATH, TOMCAT_SERVICE_NAME, SERVICE_INSTALLATION_TIMEOUT_IN_MINUTES, numOfServiceInstances);
        }

        Bootstrapper bootstrapper = new CloudBootstrapper();
        bootstrapper.setRestUrl(getRestUrl());
        attributesList = new LinkedList<String>();

        if(this.multipleServices){
//            for(int i=1; i <= numOfServices; i++){
//                String attributes = bootstrapper.listServiceInstanceAttributes(APPLICATION_NAME, TOMCAT_SERVICE_NAME + "_" + i, 1, false);
//                attributesList.add(attributes.substring(attributes.indexOf("home")));
//            }
            String attributes = bootstrapper.listServiceInstanceAttributes(APPLICATION_NAME, TOMCAT_SERVICE_NAME, 1, false);
            attributesList.add(attributes.substring(attributes.indexOf("home")));

        }

        else{
            for(int i=1; i <= numOfServiceInstances; i++){
                String attributes = bootstrapper.listServiceInstanceAttributes(APPLICATION_NAME, TOMCAT_SERVICE_NAME, i, false);
                attributesList.add(attributes.substring(attributes.indexOf("home")));
            }
        }

    }

    /**
     * 1. Shutdown management machines.
     * 2. Bootstrap using the persistence file.
     * 3. Retrieve attributes from space and compare with the ones before the shutdown.
     * 4. Shutdown an instance agent and wait for recovery.
     * @throws Exception
     */
    public void testManagementPersistency() throws Exception{

        shutdownManagement();

        CloudBootstrapper bootstrapper = getService().getBootstrapper();
        bootstrapper.useExistingFilePath(backupFilePath);
        bootstrapper.killJavaProcesses(false);
        bootstrapper.bootstrap();
        bootstrapper.setRestUrl(getRestUrl());

        List<String> newAttributesList = new LinkedList<String>();

        if(this.multipleServices){
//            for(int i=1; i <= numOfServices; i++){
//                String attributes = bootstrapper.listServiceInstanceAttributes(APPLICATION_NAME, TOMCAT_SERVICE_NAME + "_" + i, 1, false);
//                newAttributesList.add(attributes.substring(attributes.indexOf("home")));
//            }
            String attributes = bootstrapper.listServiceInstanceAttributes(APPLICATION_NAME, TOMCAT_SERVICE_NAME, 1, false);
            newAttributesList.add(attributes.substring(attributes.indexOf("home")));

        }
        else{
            for(int i=1; i <= numOfServiceInstances; i++){
                String attributes = bootstrapper.listServiceInstanceAttributes(APPLICATION_NAME, TOMCAT_SERVICE_NAME, i, false);
                newAttributesList.add(attributes.substring(attributes.indexOf("home")));
            }
        }

        List<String> differenceAttributesList = new LinkedList<String>(attributesList);
        differenceAttributesList.removeAll(newAttributesList);

        AssertUtils.assertTrue("the service attributes post management restart are not the same as the attributes pre restart", differenceAttributesList.isEmpty());

        if(multipleServices){

            LogUtils.log("shutting down one of the services");
//            ProcessingUnit processingUnit = admin.getProcessingUnits().waitFor(APPLICATION_NAME + "." + TOMCAT_SERVICE_NAME + "_" + 1);
            ProcessingUnit processingUnit = admin.getProcessingUnits().waitFor(APPLICATION_NAME + "." + TOMCAT_SERVICE_NAME);
            processingUnit.waitFor(1);
            processingUnit.getInstances()[0].getGridServiceContainer().getGridServiceAgent().shutdown();

            LogUtils.log("waiting for service to restart on a new machine");

            AssertUtils.repetitiveAssertTrue("service didn't break", new AssertUtils.RepetitiveConditionProvider() {
                @Override
                public boolean getCondition() {
//                    return admin.getProcessingUnits().getProcessingUnit(APPLICATION_NAME + "." + TOMCAT_SERVICE_NAME + "_" + 1).getTotalNumberOfInstances() < 1;
                    int totalNumberOfInstances = admin.getProcessingUnits().getProcessingUnit(APPLICATION_NAME + "." + TOMCAT_SERVICE_NAME).getTotalNumberOfInstances();
                    LogUtils.log("Total number of instances after gsa shutdown : " + totalNumberOfInstances);
                    return totalNumberOfInstances < 1;
                }
            } , OPERATION_TIMEOUT*3);

            AssertUtils.repetitiveAssertTrue("service didn't recover", new AssertUtils.RepetitiveConditionProvider() {
                @Override
                public boolean getCondition() {
//                    ProcessingUnit processingUnit = admin.getProcessingUnits().getProcessingUnit(APPLICATION_NAME + "." + TOMCAT_SERVICE_NAME + "_" + 1);
                    ProcessingUnit processingUnit = admin.getProcessingUnits().getProcessingUnit(APPLICATION_NAME + "." + TOMCAT_SERVICE_NAME);
                    LogUtils.log("total instances after break: " + processingUnit.getTotalNumberOfInstances());
                    LogUtils.log("planned instances after break: " + processingUnit.getPlannedNumberOfInstances());
                    return processingUnit.getTotalNumberOfInstances() == 1 &&
                            processingUnit.getPlannedNumberOfInstances() == 1;
                }
            } , OPERATION_TIMEOUT*3);
        }
        else{

            LogUtils.log("shutting down one of the service's GSAs");
            ProcessingUnit processingUnit = admin.getProcessingUnits().waitFor(APPLICATION_NAME + "." + TOMCAT_SERVICE_NAME);
            processingUnit.waitFor(numOfServiceInstances);
            processingUnit.getInstances()[0].getGridServiceContainer().getGridServiceAgent().shutdown();

            LogUtils.log("waiting for service to restart on a new machine");

            AssertUtils.repetitiveAssertTrue("service didn't break", new AssertUtils.RepetitiveConditionProvider() {
                @Override
                public boolean getCondition() {
                    int totalNumberOfInstances = admin.getProcessingUnits().getProcessingUnit(APPLICATION_NAME + "." + TOMCAT_SERVICE_NAME).getTotalNumberOfInstances();
                    LogUtils.log("Total number of instances after gsa shutdown : " + totalNumberOfInstances);
                    return totalNumberOfInstances < numOfServiceInstances;
                }
            } , OPERATION_TIMEOUT*4);

            AssertUtils.repetitiveAssertTrue("service didn't recover", new AssertUtils.RepetitiveConditionProvider() {
                @Override
                public boolean getCondition() {
                    ProcessingUnit processingUnit = admin.getProcessingUnits().getProcessingUnit(APPLICATION_NAME + "." + TOMCAT_SERVICE_NAME);
                    LogUtils.log("total instances after break: " + processingUnit.getTotalNumberOfInstances());
                    LogUtils.log("planned instances after break: " + processingUnit.getPlannedNumberOfInstances());
                    return processingUnit.getTotalNumberOfInstances() == numOfServiceInstances &&
                            processingUnit.getPlannedNumberOfInstances() == numOfServiceInstances;
                }
            } , OPERATION_TIMEOUT*3);
        }
    }

    /**
     * 1. Shutdown management machines.
     * 2. Corrupt the persistence file.
     * 3. Bootstrap with bad file.
     * @throws Exception
     */
    protected void testBadPersistencyFile() throws Exception {

        shutdownManagement();

        IOUtils.replaceTextInFile(backupFilePath, "instanceId", "instnceId");

        CloudBootstrapper bootstrapper = getService().getBootstrapper();
        bootstrapper.useExistingFilePath(backupFilePath);
        bootstrapper.setBootstrapExpectedToFail(true);
        bootstrapper.bootstrap();

        String output = bootstrapper.getLastActionOutput();

        AssertUtils.assertTrue("bootstrap succeeded with a defective persistence file", !output.contains(BOOTSTRAP_SUCCEEDED_STRING));
    }

    protected void bootstrapAndInstallService() throws Exception {

        super.bootstrap();
        super.installServiceAndWait(TOMCAT_SERVICE_PATH, TOMCAT_SERVICE_NAME, SERVICE_INSTALLATION_TIMEOUT_IN_MINUTES, numOfServiceInstances);

        Bootstrapper bootstrapper = getService().getBootstrapper();
        bootstrapper.setRestUrl(getRestUrl());
        attributesList = new LinkedList<String>();

        for(int i=1; i <= numOfServiceInstances; i++){
            String attributes = bootstrapper.listServiceInstanceAttributes(APPLICATION_NAME, TOMCAT_SERVICE_NAME, i, false);
            attributesList.add(attributes.substring(attributes.indexOf("home")));
        }
    }

    protected void teardownAndDeleteBackupFile() throws Exception {
        super.teardown();
        FileUtils.deleteQuietly(new File(backupFilePath));
    }

    protected void shutdownManagement() throws Exception{

        CloudBootstrapper bootstrapper = getService().getBootstrapper();
        bootstrapper.setRestUrl(getRestUrl());

        LogUtils.log("shutting down managers");
        bootstrapper.shutdownManagers("default", backupFilePath, false);
    }

    public void testCorruptedPersistencyDirectory() throws Exception {

        String persistencyFolderPath = getService().getCloud().getConfiguration().getPersistentStoragePath();
        String fileToDeletePath = persistencyFolderPath + "/deploy/management-space";

        admin.getGridServiceManagers().waitFor(numOfManagementMachines);
        Iterator<GridServiceManager> GsmIterator = admin.getGridServiceManagers().iterator();
        String machineIp1 = GsmIterator.next().getMachine().getHostAddress();
        String machineIp2 = GsmIterator.next().getMachine().getHostAddress();
        SSHUtils.runCommand(machineIp1, OPERATION_TIMEOUT, "rm -rf " + fileToDeletePath, USER_NAME, PASSWORD);
        SSHUtils.runCommand(machineIp2, OPERATION_TIMEOUT, "rm -rf " + fileToDeletePath, USER_NAME, PASSWORD);

        shutdownManagement();

        CloudBootstrapper bootstrapper = new CloudBootstrapper();
        bootstrapper.useExistingFilePath(backupFilePath);
        bootstrapper.killJavaProcesses(false);
        bootstrapper.setBootstrapExpectedToFail(true);
        bootstrapper.timeoutInMinutes(7);
        super.bootstrap(bootstrapper);

        String output = bootstrapper.getLastActionOutput();
        AssertUtils.assertTrue("bootstrap succeeded with a corrupted persistency folder", !output.contains(BOOTSTRAP_SUCCEEDED_STRING));
    }

    @Override
    protected void customizeCloud() throws Exception {
        super.customizeCloud();
        getService().setNumberOfManagementMachines(numOfManagementMachines);
        getService().getProperties().put("persistencePath", "/tmp/byon/persistency");
    }
}
