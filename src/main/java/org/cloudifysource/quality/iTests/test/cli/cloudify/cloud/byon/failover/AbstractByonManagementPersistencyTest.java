package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.byon.failover;

import org.cloudifysource.quality.iTests.framework.tools.SGTestHelper;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.byon.AbstractByonCloudTest;

/**
 *
 * User: nirb, sagib
 * Date: 05/03/13
 *
 **/

public abstract class AbstractByonManagementPersistencyTest extends AbstractByonCloudTest{

    private static final String TOMCAT_SERVICE_PATH = SGTestHelper.getBuildDir() + "/recipes/services/tomcat";
    private int numOfManagementMachines = 2;
    private int numOfServiceInstances = 2;

    public void prepareTest() throws Exception {

        super.bootstrap();
        super.installServiceAndWait(TOMCAT_SERVICE_PATH, "tomcat", SERVICE_INSTALLATION_TIMEOUT_IN_MINUTES, numOfServiceInstances);

        //bootstrap, install tomcat
        //read an attribute from mng space.
    }

    public void afterTest(){

        //teardown
    }

    public abstract void shutdownManagement() throws Exception;

    public void testManagementPersistency() throws Exception{

        //check if the read attribute exist after restart.

    }

    @Override
    protected void customizeCloud() throws Exception {
        super.customizeCloud();
        getService().setNumberOfManagementMachines(numOfManagementMachines);
    }


}
