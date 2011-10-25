package test.cli.cloudify;

import java.io.IOException;

import org.testng.annotations.Test;

public class ConnectTest extends AbstractCommandTest{
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
	public void testRestConnect() throws IOException, InterruptedException{
		
		String consoleOutput = runCommand("connect " + this.restUrl +
		";disconnect;");
		//TODO: connection assertion
		//assertEquals("Connected succesfully", consoleOutput);
	}
}
