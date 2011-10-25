package test.cli.deploy;

import java.util.concurrent.TimeUnit;

import org.openspaces.admin.pu.ProcessingUnit;
import org.testng.annotations.Test;

import framework.utils.CliUtils;


/***
 * Setup: Bring up 1 GSM and 2 GSC's on on machine
 * 
 * Test: "cli deploy-memcached" functionality
 * 
 * @author Dan Kilman
 *
 */
public class CliDeployMemcachedTest extends CliDeployAbstractTest {
    
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1")
    public void testDeployMemcached() {
        String[] args = {
                "deploy-memcached",
                "-max-instances-per-vm", "1",
                "-cluster", "schema=partitioned-sync2backup", "total_members=2,1",
                "-override-name", "testDataGrid",
                "/./testMemcached"
        };
        
        CliUtils.invokeGSMainOn(false, args);
        
        ProcessingUnit pu = admin.getProcessingUnits().getProcessingUnit("testMemcached-memcached");
        
        assertNotNull(pu);
        assertEquals(2, pu.getNumberOfInstances());
        assertEquals(1, pu.getNumberOfBackups());
        assertTrue(pu.waitFor(pu.getTotalNumberOfInstances(), 30000, TimeUnit.MILLISECONDS));
        
    }
    
	
	
}
