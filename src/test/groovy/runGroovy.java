package test.groovy;

import org.testng.Assert;
import org.testng.annotations.Test;

import test.AbstractTest;
import test.utils.SSHUtils;

public class runGroovy extends AbstractTest {
	
	@Test
	public void test(){
		String host = admin.getMachines().getMachines()[0].getHostAddress();
		String username = "tgrid";
		String password = "tgrid";
		String groovyFilePath = "./src/test/groovy/deploySpace.groovy";
		String output = SSHUtils.runGroovyFile(host, DEFAULT_TEST_TIMEOUT, username, password, groovyFilePath);
		Assert.assertTrue(output.contains("--- PASSED"));
		
	}
}
