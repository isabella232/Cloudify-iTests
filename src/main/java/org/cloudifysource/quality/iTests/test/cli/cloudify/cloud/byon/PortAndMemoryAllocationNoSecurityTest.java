package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.byon;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * test port and memory allocation without security.
 * 
 * @author adaml
 *
 */
public class PortAndMemoryAllocationNoSecurityTest extends PortAndMemoryAllocationTest {

	@BeforeClass(alwaysRun = true)
	protected void bootstrap() throws Exception {
		super.bootstrap();
		installServiceAndWait(SIMPLE_RECIPE_FOLDER, SERVICE_NAME);
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, enabled = true)
	public void testPortAndMemoryAllocation() {
		super.testPortAndMemoryAllocation();
	}
}
