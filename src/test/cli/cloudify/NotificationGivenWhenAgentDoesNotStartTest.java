package test.cli.cloudify;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.MessageFormat;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.AbstractTest;
import framework.tools.SGTestHelper;
import framework.utils.AssertUtils;
import framework.utils.LogUtils;
import framework.utils.ScriptUtils;

public class NotificationGivenWhenAgentDoesNotStartTest extends AbstractCommandTest {

	private String OriginalPath;
	private String newPath = "";
	private String cloudifyPath = ScriptUtils.getBuildPath() + "/tools/cli/cloudify.bat";
	
	@Override
	@BeforeMethod
	public void beforeTest(){
		OriginalPath = System.getenv("PATH");
		newPath = "";
		
		for(String s : OriginalPath.split(";"))
			if(!s.toLowerCase().contains("system"))
				newPath = newPath.concat(s).concat(";");	
	}
	
	@Override
	@AfterMethod
	public void afterTest(){
		try{
			LogUtils.log("tearing down local cloud");
			runCommand("teardown-localcloud");	
			LogUtils.log("Shutting down managment");
			runCommand("shutdown-management");	
			super.afterTest();
		}catch(Exception e){
			e.printStackTrace();
			AssertFail("", e);
		}
	}
	
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT , groups="1", enabled = false)
	public void testBootstrapLocalCloud() throws Exception {
		String output = runCommandSetEnv("bootstrap-localcloud", true, false, newPath);
		// TODO: assert some informative message was printed to cli due to agent not starting
		// (after creating one)
				
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT , groups="1", enabled = false)
	public void testStartManagement() throws Exception {
		String output = runCommandSetEnv("start-management", true, false, newPath);
		// TODO: assert some informative message was printed to cli due to agent not starting
		// (after creating one)
				
	}
	
	
////////////////////////////////////////////////////////////////////////////////////////////
	
	public static String runCommandSetEnv(final String command, boolean wait, boolean failCommand ,String newPath) 
			throws IOException, InterruptedException {
		
		final String commandExtention = getCommandExtention();
		
		final String cloudifyPath = ScriptUtils.getBuildPath()+
				MessageFormat.format("{0}tools{0}cli{0}cloudify.{1} ", File.separatorChar, commandExtention);

				String cmdLine = cloudifyPath + command;
		if (isWindows()) {
			// need to use the call command to intercept the cloudify batch file return code.
			cmdLine = "cmd /c call " + cmdLine;
		}
		
		final String[] parts = cmdLine.split(" ");
		final ProcessBuilder pb = new ProcessBuilder(parts);
		pb.redirectErrorStream(true);
		Map<String, String> env = pb.environment();
		env.put("PATH", newPath);
		
		LogUtils.log("Executing Command line: " + cmdLine);
		//logger.info("Command Environment: " + pb.environment());
		final Process process = pb.start();

		if(wait)
			return handleCliOutput(process, failCommand);
		else
			return null;
	}
	
	private static String handleCliOutput(Process process, boolean failCommand) throws IOException, InterruptedException{
		// Print CLI output if exists.
		final BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
		final StringBuilder consoleOutput = new StringBuilder("");
		final AtomicReference<Throwable> exception = new AtomicReference<Throwable>();
		
		Thread thread = new Thread(new Runnable() {
			
			String line = null;
			
			@Override
			public void run() {	
				try {
					while ((line = br.readLine()) != null) {
						LogUtils.log(line);
						consoleOutput.append(line + "\n");
					}
				} catch (Throwable e) {
					exception.set(e);
				}
				
			}
		});
		
		thread.setDaemon(true);
		
		thread.start();
		
		int result = process.waitFor();
		
		thread.join(5000);
		
		AssertUtils.assertTrue(exception.get() == null);
		
		if (result != 0 && !failCommand) {
			AbstractTest.AssertFail("In RunCommand: Process did not complete successfully");
		}
		return consoleOutput.toString();
	}
	
	private static String getCommandExtention() {
		String osExtention;
		if (isWindows()) {
			osExtention = "bat";
		} else {
			osExtention = "sh";
		}
		return osExtention;
	}

	private static boolean isWindows() {
		return System.getProperty("os.name").toLowerCase().startsWith("win");
	}

	public static String getPath(String relativePath) {
		return new File(SGTestHelper.getSGTestRootDir(), relativePath).getAbsolutePath().replace('\\', '/');
	}

}
