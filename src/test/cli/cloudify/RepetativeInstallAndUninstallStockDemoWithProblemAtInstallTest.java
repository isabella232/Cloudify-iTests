package test.cli.cloudify;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.karaf.util.Properties.PropertiesReader;
import org.apache.karaf.util.Properties.PropertiesWriter;
import org.testng.annotations.Test;

public class RepetativeInstallAndUninstallStockDemoWithProblemAtInstallTest {

	
	private final int repetitions = 4;
	private final String applicationDir = CommandTestUtils.getPath("apps/USM/usm/applications/stockdemo");	
	private ArrayList<String> originalProperties = new ArrayList<String>();
	
	@Test(timeOut = 1000 * 60 * 10, groups = "1", enabled = false)
	public void testInstallAndUninstall() throws IOException, InterruptedException {
		
//		saveOriginalProperties(applicationDir);
//		corruptCassandraService(applicationDir);
//		fixCassandraService(applicationDir);
		
		
		
		
		
		int scenarioSuccessCounter = 0;
		int scenarioFailCounter = 0;
		int firstInstallSuccessCounter = 0;
		
		for(int i=0 ; i < repetitions ; i++)
			switch(doTest()){
			case 1: firstInstallSuccessCounter++;
			case 2: scenarioSuccessCounter++;
			case 3: scenarioFailCounter++;
				
			}
		
		System.out.println(firstInstallSuccessCounter + "/" + repetitions + " times the first installation succeedded, these runs are irrelavent");
		System.out.println(scenarioSuccessCounter + "/" + repetitions + " times the second installation succeedded");
		System.out.println(scenarioFailCounter + "/" + repetitions + " times the second installation failed - THIS IS WHAT WE TEST FOR");
	}

	private int doTest() throws IOException, InterruptedException {
		corruptCassandraService(applicationDir);
		String failOutput = CommandTestUtils.runCommand("connect " + restUrl + ";install-application --verbose -timeout 4 " + applicationDir, true, true);		
		if(!failOutput.toLowerCase().contains("operation failed"))
			return 1;
		
		runCommand("connect " + restUrl + ";uninstall-application --verbose stockdemo");
				
		String successOutput = CommandTestUtils.runCommand("connect " + restUrl + ";install-application --verbose " + applicationDir, true, true);
		if(successOutput.toLowerCase().contains("successfully installed"))
			return 2;
		else
			return 3;
	}
	
	@Override
	@AfterMethod
	public void afterTest(){
		try {
			fixCassandraService(applicationDir);
		} catch (IOException e) {
			System.out.println("FAILED FIXING CASSANDRA SERVICE!!!");
			e.printStackTrace();
		}
		super.afterTest();
	}
	
	private void fixCassandraService(String applicationDir) throws IOException {
		
		File cassandraProperties = new File("D:/opt/cassandra.properties");
		FileWriter writer = new FileWriter(cassandraProperties);
		
		for(String property : originalProperties)
			writer.write(property);	
		
		writer.close();
	}

	private void saveOriginalProperties(String applicationDir)	throws FileNotFoundException, IOException {
		File cassandraProperties = new File("D:/opt/cassandra.properties");
		FileReader reader = new FileReader(cassandraProperties);
		
		PropertiesReader pr = new PropertiesReader(reader);
		
		String property = null;
		while((property = pr.readProperty())  != null)
			originalProperties.add(property);
		
		pr.close();
	}
	
	private void corruptCassandraService(String applicationDir) throws IOException {
		File cassandraProperties = new File("D:/opt/cassandra.properties");
		FileWriter writer = new FileWriter(cassandraProperties);
		PropertiesWriter pw = new PropertiesWriter(writer);
		
		for(String property : originalProperties){
			if(property.contains("script"))	
				property = "script = bad path";
			pw.writeln(property);	
			
		}
		writer.close();				
	}
	
}
