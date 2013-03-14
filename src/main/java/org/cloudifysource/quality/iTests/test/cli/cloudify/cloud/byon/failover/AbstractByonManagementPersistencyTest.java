package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.byon.failover;

import org.apache.commons.io.FileUtils;
import org.cloudifysource.quality.iTests.framework.tools.SGTestHelper;
import org.cloudifysource.quality.iTests.framework.utils.*;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.byon.AbstractByonCloudTest;
import org.openspaces.admin.pu.ProcessingUnit;

import java.io.File;
import java.net.URL;
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

    public static final String BOOTSTRAP_SUCCEEDED_STRING = "Successfully created Cloudify Manager";
    public static final String APPLICATION_NAME = "default";

    protected String backupFilePath = SGTestHelper.getBuildDir() + "/backup-details" + System.currentTimeMillis() + ".txt";

    private static final String TOMCAT_SERVICE_PATH = SGTestHelper.getBuildDir() + "/recipes/services/tomcat";
    private static final String TOMCAT_SERVICE_NAME = "tomcat";

    private int numOfManagementMachines = 2;
    private int numOfServiceInstances = 2;
    private List<String> attributesList = new LinkedList<String>();

    /**
     * 1. Shutdown management machines.
     * 2. Bootstrap using the persistence file.
     * 3. Retrieve attributes from space and compare with the ones before the shutdown.
     * 4. Shutdown an instance agent and wait for recovery.
     * @throws Exception
     */
    protected void testManagementPersistency() throws Exception{

        shutdownManagement();

        CloudBootstrapper bootstrapper = getService().getBootstrapper();
        bootstrapper.useExistingFilePath(backupFilePath);
        bootstrapper.killJavaProcesses(false);
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


    @Override
    protected void customizeCloud() throws Exception {
        super.customizeCloud();
        getService().setNumberOfManagementMachines(numOfManagementMachines);
        getService().getProperties().put("persistencePath", "/tmp/byon/persistency");

    }
}