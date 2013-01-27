package test.cli.cloudify;

import org.testng.annotations.Test;

import framework.tools.SGTestHelper;

/**
 * GS-1230
 * @author nirb
 *
 */
public class ChangeServiceNameTest extends AbstractLocalCloudTest{

	private static final String SGTEST_ROOT_DIR = SGTestHelper.getSGTestRootDir().replace('\\', '/');

	private static final String SERVICE_NAME = "newGroovy";
	private static final String SERVICE_FOLDER_NAME = "groovy-with-properties";
	private static final String SERVICE_PATH = SGTEST_ROOT_DIR + "/src/main/resources/apps/USM/usm/" + SERVICE_FOLDER_NAME;

	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void install() throws Exception{
		
		installServiceAndWait(SERVICE_PATH, SERVICE_NAME, false);
	}
}
