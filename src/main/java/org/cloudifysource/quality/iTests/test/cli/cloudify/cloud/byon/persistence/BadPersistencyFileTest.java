package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.byon.persistence;

import org.testng.annotations.*;

/**
 * User: nirb
 * Date: 06/03/13
 */
public class BadPersistencyFileTest extends AbstractByonManagementPersistencyTest {

    @BeforeClass(alwaysRun = true)
    public void bootstrapAndInit() throws Exception{
        super.bootstrap();
        super.installTomcatService(3, null);
    }

    @Test(timeOut = DEFAULT_TEST_TIMEOUT * 4, enabled = true)
    public void testBadPersistencyFile() throws Exception {
        super.testBadPersistencyFile();
    }

    @AfterClass(alwaysRun = true)
    public void teardown() throws Exception{
        super.teardown();
    }
}