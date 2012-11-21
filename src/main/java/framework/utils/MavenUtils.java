package framework.utils;

import static framework.utils.ScriptUtils.getBuildPath;

import java.io.File;

import org.openspaces.admin.machine.Machine;

import test.AbstractTestSupport;

/**
 * Utility class for maven operations
 * @author elip
 *
 */
public class MavenUtils {

	public static final String username = "tgrid";
	public static final String password = "tgrid";
	public static final String mavenCreate = "mvn os:create -Dtemplate=";
	public static final String mavenCompile = "mvn compile";
	public static final String mavenPackage = "mvn package";
	public static final String mavenDeploy = "mvn os:deploy";
	public static final String mavenRun = "mvn os:run";
	public static final String mavenRunStandalone = "mvn os:run-standalone";


	/**
	 * installs the maven repository
	 * @throws Exception 
	 */
	public static boolean installMavenRep(Machine machine) {

		String mavenHome = getBuildPath() + "/tools/maven";
		String scriptOutPut = SSHUtils.runCommand(machine.getHostAddress(), AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 2, 
				"cd " + mavenHome + ";" + "./installmavenrep.sh", MavenUtils.username, MavenUtils.password);
		if (scriptOutPut == null) return false;
		return true;
	}


	/**
	 * checks if maven created the my-app folder
	 * this method will be usually invoked to ensure that maven os:create was successful
	 * @param hostName - the host on witch the app should be created on
	 * @return
	 */
	public static boolean isAppExists(String hostName) {
		String pathName = getBuildPath() + "/../my-app";
		File myAppDir = new File(pathName);
		return myAppDir.isDirectory();	
	}


	/**
	 * delete the maven repository from the specified machine
	 * @param machine
	 */
	public static void deleteMavenRep(Machine machine) {
		SSHUtils.runCommand(machine.getHostAddress(), AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 2, 
				"cd ~;cd .m2;rm -rf *", MavenUtils.username, MavenUtils.password);
	}


	/**
	 * delete the app maven created
	 * @param machine
	 */
	public static void deleteApp(Machine machine) {
		String buildPath = ScriptUtils.getBuildPath();
		SSHUtils.runCommand(machine.getHostAddress(), AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 2, 
				"cd " + buildPath + "/..;rm -rf my-app", MavenUtils.username, MavenUtils.password);

	}


	public static boolean importMuleJars(Machine machine) throws Exception {
//		WGetUtils.wget("http://dist.codehaus.org/mule/distributions/mule-3.2.0-embedded.jar", 
//				ScriptUtils.getBuildPath() + "/lib/platform/mule/");
		String mulePath = getBuildPath() + "/lib/platform/mule";
	  SSHUtils.runCommand(machine.getHostAddress(), 600000, "mkdir "  + mulePath +  ";cp -r /export/utils/temp/\"mule jars\"/* " + 
			  mulePath, MavenUtils.username, MavenUtils.password);
		
		
		return true;
	}

}