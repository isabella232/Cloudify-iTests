package org.cloudifysource.quality.iTests.test.cli.cloudify;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.MessageFormat;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.io.FileUtils;
import iTests.framework.tools.SGTestHelper;
import iTests.framework.utils.AssertUtils;
import iTests.framework.utils.LogUtils;
import iTests.framework.utils.ScriptUtils;

public class CommandTestUtils {

    private static final boolean enableLogstash = Boolean.parseBoolean(System.getProperty("iTests.enableLogstash", "false"));

    /**
	 * Runs the specified cloudify commands and outputs the result log.
	 * @param command - The actual cloudify commands delimited by ';'.
	 * @param wait - used for determining if to wait for command 
	 * @param failCommand  - used for determining if the command is expected to fail
	 * @return String - log output or null if wait is false
	 * @throws IOException
	 * @throws InterruptedException 
	 */
	public static String runCommand(final String command, boolean wait, boolean failCommand) throws IOException, InterruptedException {
		return runLocalCommand( getCloudifyCommand(command), wait, failCommand);
	}

	public static ProcessOutputPair runCommand(final String command, long timeoutMillis, boolean backgroundConsole, boolean waitForForegroundConsole,
                                               boolean failCommand, AtomicReference<ThreadSignal> threadSignal, final Map<String, String> additionalProcessVariables, final String... expectedMessages) throws IOException, InterruptedException{
		return runLocalCommand(getCloudifyCommand(command), ".", timeoutMillis, backgroundConsole, waitForForegroundConsole, failCommand, threadSignal, additionalProcessVariables, expectedMessages);
	}

	/**
	 * Runs the specified cloudify commands and outputs the result log.
	 * @param cloudifyCommand - The actual cloudify commands delimited by ';'.
	 * @return the cloudify output, and the exitcode 
	 * @throws IOException
	 * @throws InterruptedException 
	 */
	public static ProcessResult runCloudifyCommandAndWait(final String cloudifyCommand) throws IOException, InterruptedException {
		final Process process = startProcess(getCloudifyCommand(cloudifyCommand));
	    ProcessResult handleCliOutput = handleCliOutput(process);
	    if (handleCliOutput.getExitcode() != 0) {
			AssertUtils.assertFail("CLI execution did not end succesfully, exit code was " + handleCliOutput.getExitcode() + 
					"output is " + handleCliOutput.getOutput());
		}
		return handleCliOutput;
	}


	private static String getCloudifyCommand(String command) {
		final String commandExtention = getCommandExtention();
		final String cloudifyPath = ScriptUtils.getBuildPath()+
				MessageFormat.format("{0}tools{0}cli{0}cloudify.{1} ", File.separatorChar, commandExtention);
		return (cloudifyPath + command).replace("\\", "/");
	}


    public static String runLocalCommand(final String command, boolean wait, boolean failCommand) throws IOException, InterruptedException {

        final Process process = startProcess(command);

        if(wait)
            return handleCliOutput(process, failCommand);
        else
            return null;
    }
    
    private static Process startProcess(String command)
    		throws IOException {
    	
    	String cmdLine = command;
    	if (isWindows()) {
    		// need to use the call command to intercept the cloudify batch file return code.
    		cmdLine = "cmd /c call " + cmdLine;
    	}
    	
    	final String[] parts = cmdLine.split(" ");
    	final ProcessBuilder pb = new ProcessBuilder(parts);
    	pb.redirectErrorStream(true);
    	
    	LogUtils.log("Executing Command line: " + cmdLine);
    	
    	final Process process = pb.start();
    	return process;
    }

    public static class ProcessResult {
    	
    	private final String output;
    	private final int exitcode;
    	
    	public ProcessResult(String output, int exitcode) {
			this.output = output;
			this.exitcode = exitcode;
		}
    	
		@Override
		public String toString() {
			return "ProcessResult [output=" + getOutput() + ", exitcode=" + getExitcode()
					+ "]";
		}

		public String getOutput() {
			return output;
		}

		public int getExitcode() {
			return exitcode;
		}
    }
    
	private static String handleCliOutput(Process process, boolean failCommand) throws IOException, InterruptedException{
		ProcessResult result = handleCliOutput(process);
		
		if (result.getExitcode() != 0 && !failCommand) {
			AssertUtils.assertFail("CLI execution did not end succesfully, exit code was " + result.getExitcode() + 
					"output is " + result.getOutput());
		}
		return result.output;
	}

	private static ProcessResult handleCliOutput(Process process) throws InterruptedException {
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
		
		int exitcode = process.waitFor();
		
		thread.join(5000);
		
		if (exception.get() != null) {
			AssertUtils.assertFail("Failed to get process output. output="+consoleOutput,exception.get());
		}
		String stdout = consoleOutput.toString();
		return new ProcessResult(stdout, exitcode);
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
	
	public static String runCommandUsingFile(final String command) throws IOException, InterruptedException {
		final File tempFile = File.createTempFile("testcommands", "txt");
		tempFile.deleteOnExit();
		FileUtils.writeStringToFile(tempFile, command);
		return runCommandAndWait("-f=" + tempFile.getAbsolutePath());
	}

    /**
     * General method for running local commands and output the result to the log. Can be used in the following manors: <p>
     * 1) simply run command<p>
     * 2) run commands, wait for the output and return it<p>
     * 3) run commands and inspect the output as the command runs, waiting for some message
     *
     * @param command
     * @param expectedMessages
     * @param workingDirPath
     * @param timeoutMillis - timeout for repetitive assert on command's output containing expectedMessage in background console
     * @param backgroundConsole - If true then the command to be run is expected to not return
     * and keep the console in the background. Since we can't wait until the command will end to inspect the output
     * handleOutputOfBlockingCommand() is called to inspect the output on the fly, according to timeoutMillis and expectedMessage.
     * @param waitForForegroundConsole - If the console is foreground this will indicate if the method waits for the command to return or not.
     * @param failCommand - used for determining if the command is expected to fail (in foreground case)
     * @return
     * @throws java.io.IOException
     * @throws InterruptedException
     */
    public static ProcessOutputPair runLocalCommand(final String command, final String workingDirPath,
                                                    long timeoutMillis, boolean backgroundConsole, boolean waitForForegroundConsole,
                                                    boolean failCommand, AtomicReference<ThreadSignal> threadSignal, final Map<String, String> additionalProcessVariables,final String... expectedMessages) throws IOException, InterruptedException{

        if(backgroundConsole && waitForForegroundConsole){
            throw new IllegalStateException("Usage: The console can't be both background and foreground");
        }

        String cmdLine = command;
        if (isWindows()) {
            cmdLine = "cmd /c call " + cmdLine;
        }

        final String[] parts = cmdLine.split(" ");
        final ProcessBuilder pb = new ProcessBuilder(parts);

        if(workingDirPath != null){
            File workingDir = new File(workingDirPath);
            if(!workingDir.exists() || !workingDir.isDirectory()){
                throw new IOException(workingDirPath + " should be a path of a directory");
            }
            pb.directory(workingDir);
        }
        if(additionalProcessVariables != null){
            Map<String, String> env = pb.environment();
            for(Map.Entry<String, String> additinalVar : additionalProcessVariables.entrySet()){
                env.put(additinalVar.getKey(), additinalVar.getValue());
            }
        }
        pb.redirectErrorStream(true);

        LogUtils.log("Executing Command line: " + cmdLine);
        final Process process = pb.start();

        if(backgroundConsole){
            return new ProcessOutputPair(process, handleOutputOfBackgroundCommand(threadSignal, process, command, timeoutMillis, expectedMessages));
        }
        if(waitForForegroundConsole){
            return new ProcessOutputPair(process, handleCliOutput(process, failCommand));
        }
        return new ProcessOutputPair(process, null);
    }

    public static String handleOutputOfBackgroundCommand(final AtomicReference<ThreadSignal> threadSignal ,Process process,
                                                         final String command , long timeoutMillis, final String... expectedMessages) throws IOException, InterruptedException{

        final BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
        final StringBuilder consoleOutput = new StringBuilder("");
        final AtomicReference<Throwable> exception = new AtomicReference<Throwable>();
        final AtomicBoolean foundOutputMessages = new AtomicBoolean(false);

        Thread thread = new Thread(new Runnable() {

            String line = null;
            boolean foundAllExpectedMessages = false;

            @Override
            public void run() {
                try {
                    while (threadSignal.get() == ThreadSignal.RUN_SIGNAL && (br.ready() ? ((line = br.readLine()) != null) : sleepAndReturnTrue())) {
                        //prevents repetitive logging of the same line
                        if(line != null){
                            LogUtils.log(line);
                            consoleOutput.append(line + "\n");
                            line = null;
                        }
                        foundAllExpectedMessages = stringContainsMultipleStrings(consoleOutput.toString(), expectedMessages);
                        foundOutputMessages.set(foundAllExpectedMessages);
                    }
                    threadSignal.set(ThreadSignal.STOPPED); // signal that the thread finished
                } catch (Throwable e) {
                    exception.set(e);
                }
            }
        });
        thread.setDaemon(true);
        thread.start();


        AssertUtils.repetitiveAssertTrue("The expected messages were not returned by the command " + command
                , new AssertUtils.RepetitiveConditionProvider() {

            @Override
            public boolean getCondition() {
                return foundOutputMessages.get();
            }
        }, timeoutMillis);

        AssertUtils.assertTrue(exception.get() == null);

        return consoleOutput.toString();
    }

    public static enum ThreadSignal{RUN_SIGNAL, STOP_SIGNAL, STOPPED}

    public static class ProcessOutputPair{

        Process process;
        String output;

        public ProcessOutputPair(Process process, String output){
            this.process = process;
            this.output = output;
        }

        public Process getProcess(){
            return process;
        }
        public String getOutput(){
            return output;
        }

    }

    private static boolean sleepAndReturnTrue() throws InterruptedException {
        AssertUtils.sleep(100);
        return true;
    }

    public static boolean stringContainsMultipleStrings(String s, String[] expecteds){

        for(String expected : expecteds){
            if(!s.contains(expected)){
                return false;
            }
        }
        return true;
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

	public static boolean isWindows() {
		return System.getProperty("os.name").toLowerCase().startsWith("win");
	}

	public static String getPath(String relativePath) {
		return new File(SGTestHelper.getSGTestRootDir(), relativePath).getAbsolutePath().replace('\\', '/');
	}
	
	public static String getBuildServicesPath() {
		return SGTestHelper.getBuildDir() + "/recipes/services";
	}
	
	public static String getBuildApplicationsPath() {
		return SGTestHelper.getBuildDir() + "/recipes/apps";
	}


}
