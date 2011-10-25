package test.cli.deploy;

import java.util.concurrent.TimeUnit;

import org.openspaces.admin.pu.ProcessingUnit;
import org.testng.annotations.Test;

import test.utils.CliUtils;

/***
 * Setup: Bring up 1 GSM and 2 GSC's on on machine
 * 
 * Test: "cli deploy-space" functionality
 * 
 * @author Dan Kilman
 *
 */
public class CliDeploySpaceTest extends CliDeployAbstractTest {
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1")
    public void testDeploySpace() {
        String[] args = {
                "deploy-space",
                "-max-instances-per-vm", "1",
                "-cluster", "schema=partitioned-sync2backup", "total_members=2,1",
                "testSpace"
        };
        
        CliUtils.invokeGSMainOn(false, args);
        
        ProcessingUnit pu = admin.getProcessingUnits().getProcessingUnit("testSpace");
        
        assertNotNull(pu);
        assertEquals(2, pu.getNumberOfInstances());
        assertEquals(1, pu.getNumberOfBackups());
        assertTrue(pu.waitFor(pu.getTotalNumberOfInstances(), 30000, TimeUnit.MILLISECONDS));
        
    }
}
