package test.cli.cloudify.recipes;

import java.io.File;
import java.io.IOException;

import junit.framework.Assert;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import test.cli.cloudify.AbstractLocalCloudTest;
import test.cli.cloudify.CommandTestUtils;

import org.cloudifysource.dsl.internal.DSLException;
import org.cloudifysource.dsl.internal.ServiceReader;

public class TestRecipeCommandTest extends AbstractLocalCloudTest {
	
	final private String SIMPLE_RECIPE_DIR_PATH = CommandTestUtils
			.getPath("apps/USM/usm/simplejavaprocess");
	final private String GROOVY_RECIPE_DIR_PATH = CommandTestUtils
			.getPath("apps/USM/usm/groovy");

	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
	public void testInvocationUsingDirAndFile() throws IOException,
			InterruptedException, DSLException {

		File serviceDir = new File(SIMPLE_RECIPE_DIR_PATH);
		File serviceFile = new File(SIMPLE_RECIPE_DIR_PATH,
				"simplejava-modifiedservice.groovy");
		org.cloudifysource.dsl.Service service = ServiceReader
				.getServiceFromFile(serviceFile, serviceDir)
				.getService();
		int port = (Integer) service.getPlugins().get(0).getConfig()
				.get("port");
		//TODO: is this thread even doing somthing? Check
		new Thread(new RecipeTestUtil.AsinchronicPortCheck(port)).start();

		String consoleOutput = runCommand("test-recipe --verbose "
				+ SIMPLE_RECIPE_DIR_PATH
				+ " 30 simplejava-modifiedservice.groovy");
		Assert.assertFalse("The command threw an exception - check the log",
				consoleOutput.contains("Exception"));
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
	public void testTestRecipeWithContext() throws IOException,
			InterruptedException, DSLException {

		
		runCommand("test-recipe --verbose "
				+ GROOVY_RECIPE_DIR_PATH
				+ " 30 ");
		
	}


	@Override
	@AfterMethod
	public void afterTest() {
	}
}
