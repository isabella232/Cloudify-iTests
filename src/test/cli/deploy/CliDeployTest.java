package test.cli.deploy;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import org.openspaces.admin.pu.ProcessingUnit;
import org.testng.annotations.Test;

import test.utils.CliUtils;

/***
 * Setup: Bring up 1 GSM and 2 GSC's on on machine
 * 
 * Test: "cli deploy" functionality
 * testing deployment using external sla.xml
 * 
 * @author Dan Kilman
 *
 */
public class CliDeployTest extends CliDeployAbstractTest {
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1")
	public void testDeploy() throws IOException {
        
	    // get resource into the filesystem so it can be found by GS.main
	    InputStream slaInputStream= Thread.currentThread().getContextClassLoader().getResourceAsStream("test/cli/resources/cli-deploy-sla.xml");
        File slaFile = CliUtils.createFileFromInputStream(slaInputStream, "sla.xml");
        String slaPath = slaFile.getAbsolutePath();
        
        String[] args = {
	            "deploy",
	            "-sla", slaPath,
	            "-override-name", "testDataGrid",
	            "templates/datagrid"
	    };
        slaInputStream.close();
        
	    CliUtils.invokeGSMainOn(false, args);

	    // cleanup
	    slaFile.delete();
	    
	    ProcessingUnit pu = admin.getProcessingUnits().getProcessingUnit("testDataGrid");
	    
	    assertNotNull(pu);
	    
	    // as defined in cli-deploy-sla.xml
	    assertEquals(1, pu.getNumberOfInstances());
	    assertEquals(0, pu.getNumberOfBackups());
	    assertTrue(pu.waitFor(pu.getTotalNumberOfInstances(), 30000, TimeUnit.MILLISECONDS));
	    
	}
	
}
