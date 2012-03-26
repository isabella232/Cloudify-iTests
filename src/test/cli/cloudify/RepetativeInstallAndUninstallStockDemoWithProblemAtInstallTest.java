package test.cli.cloudify;

import java.io.File;
import java.io.IOException;

import junit.framework.Assert;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

public class RepetativeInstallAndUninstallStockDemoWithProblemAtInstallTest extends AbstractLocalCloudTest {

	private final int repetitions = 4;
	private String cassandraPostStartScriptPath = null;
	private String newPostStartScriptPath = null;
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * repetitions, groups = "1", enabled = true)
	public void installAndUninstallTest() throws IOException, InterruptedException {
		
		final String stockdemoAppPath = CommandTestUtils.getPath("apps/USM/usm/applications/stockdemo");	
		cassandraPostStartScriptPath = stockdemoAppPath + "/cassandra/cassandra_poststart.groovy";	
		newPostStartScriptPath = stockdemoAppPath + "/cassandra/cassandra_poststart123.groovy";
		int scenarioSuccessCounter = 0;
		int scenarioFailCounter = 0;
		int firstInstallSuccessCounter = 0;
		
		for(int i=0 ; i < repetitions ; i++){
			switch(doTest(stockdemoAppPath, cassandraPostStartScriptPath ,newPostStartScriptPath)){
			case 1: {firstInstallSuccessCounter++;break;}
			case 2: {scenarioSuccessCounter++;break;}
			case 3: {scenarioFailCounter++;break;}
				
			}
		}
		System.out.println(firstInstallSuccessCounter + "/" + repetitions + " times the first installation succeedded, these runs are irrelavent");
		System.out.println(scenarioSuccessCounter + "/" + repetitions + " times the second installation succeedded");
		System.out.println(scenarioFailCounter + "/" + repetitions + " times the second installation failed - THIS IS WHAT WE TEST FOR");
		Assert.assertTrue("second install should never fail, it failed " + scenarioFailCounter + " times", scenarioFailCounter==0);
	}

	private int doTest(String stockdemoAppPath, String cassandraPostStartScriptPath ,String  newPostStartScriptPath) throws IOException, InterruptedException {
		corruptCassandraService(cassandraPostStartScriptPath ,newPostStartScriptPath);
		
		String failOutput = CommandTestUtils.runCommand("connect " + restUrl + ";install-application --verbose -timeout 5 " + stockdemoAppPath, true, true);		
		if(!failOutput.toLowerCase().contains("operation failed"))
			return 1;
		fixCassandraService(cassandraPostStartScriptPath , newPostStartScriptPath);
		runCommand("connect " + restUrl + ";uninstall-application --verbose stockdemo");
				
		String successOutput = CommandTestUtils.runCommand("connect " + restUrl + ";install-application --verbose -timeout 5 " + stockdemoAppPath, true, true);
		runCommand("connect " + restUrl + ";uninstall-application --verbose stockdemo");
		if(successOutput.toLowerCase().contains("successfully installed"))
			return 2;
		else
			return 3;
	}
	
	@Override
	@AfterMethod
	public void afterTest(){
		super.afterTest();
		try {
			fixCassandraService(cassandraPostStartScriptPath , newPostStartScriptPath);
		} catch (IOException e) {
			System.out.println("FAILED FIXING CASSANDRA SERVICE!!!");
			e.printStackTrace();
		}
	}

	private void corruptCassandraService(String cassandraPostStartScriptPath , String newPostStartScriptPath) throws IOException {
		File cassandraPostStartScript = new File(cassandraPostStartScriptPath);
		boolean success = cassandraPostStartScript.renameTo(new File(newPostStartScriptPath));
		if(!success)
			throw new IOException("Test error: failed renaming " +  cassandraPostStartScriptPath + " to " + newPostStartScriptPath);
	}
	
	private void fixCassandraService(String cassandraPostStartScriptPath , String newPostStartScriptPath) throws IOException {
		File cassandraPostStartScript = new File(newPostStartScriptPath);
		boolean success = cassandraPostStartScript.renameTo(new File(cassandraPostStartScriptPath));
		if(!success)
			throw new IOException("Test error: failed renaming " +  newPostStartScriptPath + " to " + cassandraPostStartScriptPath);
	}
}
