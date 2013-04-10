package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.ec2.staticstorage;

import org.cloudifysource.quality.iTests.test.cli.cloudify.CommandTestUtils;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.ec2.AbstractEc2OneServiceStorageAllocationTest;

/**
 * Base class for all static storage tests that use one service. <br>
 * Defines a well known services folder to hold all tests services.
 * </br>
 * Created with IntelliJ IDEA.
 * User: elip
 * Date: 4/9/13
 * Time: 3:42 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class AbstractEc2OneServiceStaticStorageTest extends AbstractEc2OneServiceStorageAllocationTest {

    private static final String PATH_TO_SERVICES_FOLDER = CommandTestUtils.getPath("/src/main/resources/apps/USM/usm/staticstorage/");

    @Override
    public String getPathToServicesFolder() {
        return PATH_TO_SERVICES_FOLDER;
    }
}
