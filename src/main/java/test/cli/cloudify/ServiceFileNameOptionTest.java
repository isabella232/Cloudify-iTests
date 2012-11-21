package test.cli.cloudify;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.openspaces.admin.pu.ProcessingUnit;
import org.testng.annotations.Test;

import framework.utils.IOUtils;

public class ServiceFileNameOptionTest extends AbstractLocalCloudTest {
	
	private final static String SERVICE_NAME = "simple";
	
	private final static String SERVICE_NAME_REPLACE = "simpleReplace";
	
	private final static String SERVICE_FILE_NAME = "test-file-name";
	
	private final static String APPLICATION_NAME = "default";
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
	public void testServiceFileName() throws IOException, InterruptedException {
		
		
		File simpleServiceNewDSLFile = null;
		try {
			String simpleServiceOriginalDSLPath = CommandTestUtils.getPath("apps/USM/usm/simple");
			File simpleServiceOriginalDSLFile = new File(simpleServiceOriginalDSLPath + "/simple-service.groovy");
			simpleServiceNewDSLFile = new File(simpleServiceOriginalDSLPath + "/" + SERVICE_FILE_NAME + ".groovy");

			FileUtils.copyFile(simpleServiceOriginalDSLFile, simpleServiceNewDSLFile);

			IOUtils.replaceTextInFile(simpleServiceNewDSLFile.getAbsolutePath(), SERVICE_NAME, SERVICE_NAME_REPLACE);

			String command = "connect localhost; install-service --verbose -service-file-name " + simpleServiceNewDSLFile.getName() + " " + simpleServiceOriginalDSLPath;
			CommandTestUtils.runCommandAndWait(command);
			
			ProcessingUnit simpleService = admin.getProcessingUnits().getProcessingUnit(APPLICATION_NAME + "." + SERVICE_NAME_REPLACE);
			assertNotNull(simpleService);	
		}
		finally {
			if ((simpleServiceNewDSLFile != null) && (simpleServiceNewDSLFile.exists())) {
				simpleServiceNewDSLFile.deleteOnExit();
			}
		}	
	}

}
