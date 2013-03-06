package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.byon.failover;

import org.cloudifysource.quality.iTests.framework.tools.SGTestHelper;
import org.cloudifysource.quality.iTests.framework.utils.Bootstrapper;
import org.cloudifysource.quality.iTests.framework.utils.CloudBootstrapper;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.byon.AbstractByonCloudTest;

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
    protected static final String BACKUP_FILE_PATH = SGTestHelper.getBuildDir() + "/backup-details.txt";

    private int numOfManagementMachines = 2;
    private int numOfServiceInstances = 2;

    public void prepareTest() throws Exception {

        super.bootstrap();
        super.installServiceAndWait(TOMCAT_SERVICE_PATH, TOMCAT_SERVICE_NAME, SERVICE_INSTALLATION_TIMEOUT_IN_MINUTES, numOfServiceInstances);

        Bootstrapper bootstrapper = new CloudBootstrapper();
        bootstrapper.setRestUrl(getRestUrl());
        List<String> attributesList = new LinkedList<String>();

        for(int i=1; i <= numOfServiceInstances; i++){
            String attributes = bootstrapper.listServiceInstanceAttributes("default", TOMCAT_SERVICE_NAME, i, false);
            attributesList.add(attributes);
        }

    }

    public void afterTest() throws Exception {
        super.teardown();
    }

    public abstract void shutdownManagement() throws Exception;

    public void testManagementPersistency() throws Exception{

        CloudBootstrapper bootstrapper = new CloudBootstrapper();
        bootstrapper.setRestUrl(getRestUrl());

        //check if the read attribute exist after restart.
        shutdownManagement();

        bootstrapper.useExistingFilePath(BACKUP_FILE_PATH);
        bootstrapper.bootstrap();

    }

    @Override
    protected void customizeCloud() throws Exception {
        super.customizeCloud();
        getService().setNumberOfManagementMachines(numOfManagementMachines);
    }


}
