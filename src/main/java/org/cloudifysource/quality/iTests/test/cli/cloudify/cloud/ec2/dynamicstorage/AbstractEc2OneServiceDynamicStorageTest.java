package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.ec2.dynamicstorage;

import org.cloudifysource.quality.iTests.test.cli.cloudify.CommandTestUtils;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.ec2.AbstractEc2OneServiceStorageAllocationTest;

/**
 * Base class for all dynamic storage tests that use one service. <br>
 * Defines a well known services folder to hold all tests services.
 * </br>
 *
 */
public abstract class AbstractEc2OneServiceDynamicStorageTest extends AbstractEc2OneServiceStorageAllocationTest {

    private static final String PATH_TO_SERVICES_FOLDER = CommandTestUtils.getPath("/src/main/resources/apps/USM/usm/dynamicstorage/");

    @Override
    public String getPathToServicesFolder() {
        return PATH_TO_SERVICES_FOLDER;
    }

}
