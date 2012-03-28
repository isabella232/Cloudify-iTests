package test.cli.cloudify;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.MessageFormat;
import java.util.concurrent.atomic.AtomicReference;

import org.openspaces.admin.Admin;
import org.openspaces.admin.AdminFactory;

import test.AbstractTest;
import framework.tools.SGTestHelper;
import framework.utils.AssertUtils;
import framework.utils.DumpUtils;
import framework.utils.LogUtils;
import framework.utils.ScriptUtils;

public class CommandTestUtils {


	private static final int ADMIN_REFRESH_TIME_MILLIS = 15000;

	/**
	 * Runs over all the commands and outputs the result log.
	 * @param command - The actual commands delimited by ';'.
	 * @param commandNam - used for determining the log file name.
	 * @param wait - used for determining if to wait for command 
	 * @param failCommand  - used for determining if the command is expected to fail
	 * @return String - log output or null if wait is false
	 * @throws IOException
	 * @throws InterruptedException 
	 */
	public static String runCommand(final String command, boolean wait, boolean failCommand) throws IOException, InterruptedException {
		
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

		LogUtils.log("Executing Command line: " + cmdLine);
		//logger.info("Command Environment: " + pb.environment());
		final Process process = pb.start();

		if(wait)
			return handleCliOutput(process, failCommand);
		else
			return null;
	}

    public static String runLocalCommand(final String command, boolean wait, boolean failCommand) throws IOException, InterruptedException {

        String cmdLine = command;
        if (isWindows()) {
            cmdLine = "cmd /c call " + cmdLine;
        }

        final String[] parts = cmdLine.split(" ");
        final ProcessBuilder pb = new ProcessBuilder(parts);
        pb.redirectErrorStream(true);

        LogUtils.log("Executing Command line: " + cmdLine);
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
			Admin admin = getAdminWithLocators();
			//Give the admin some time to refresh.
			Thread.sleep(ADMIN_REFRESH_TIME_MILLIS);
			DumpUtils.dumpProcessingUnit(admin);
			admin.close();
			AbstractTest.AssertFail("In RunCommand: Process did not complete successfully");
		}
		return consoleOutput.toString();
	}
	
	private static Admin getAdminWithLocators() {
		// Class LocalhostGridAgentBootsrapper defines the locator discovery addresses.
		final String nicAddress = "127.0.0.1"; 

		final AdminFactory factory = new AdminFactory();
		LogUtils.log("adding locator to admin : " + nicAddress + ":4168");
		factory.addLocator(nicAddress + ":4168");
		return factory.createAdmin();
	}

	/**
	 * @param command
	 * @return
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static String runCommandAndWait(final String command) throws IOException, InterruptedException {
		return runCommand(command, true, false);
	}

	/**
	 * @param command
	 * @return
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static String runCommandExpectedFail(final String command) throws IOException, InterruptedException {
		return runCommand(command, true, true);
	}
	
	public static String runCommand(final String command) throws IOException, InterruptedException {
		return runCommand(command, false, false);
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
