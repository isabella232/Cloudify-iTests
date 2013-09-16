    String shutdownCommand;
    String operatingSystem = System.getProperty("os.name");
	println operatingSystem;
    if (operatingSystem.contains("Linux") || operatingSystem.contains("Mac OS X")) {
        shutdownCommand = "sudo shutdown -r now";
    }
    else if (operatingSystem.contains("Windows")) {
        shutdownCommand = "sudo shutdown.exe -r -t 0";
    }
    else {
        throw new RuntimeException("Unsupported operating system.");
    }
	println "running command " + shutdownCommand;
    Runtime.getRuntime().exec(shutdownCommand);
    System.exit(0);



//import org.apache.commons.lang.SystemUtils
//import java.io.*

//String shutdownCommand = null;

//String t = "now";

//if(SystemUtils.IS_OS_FREE_BSD || SystemUtils.IS_OS_LINUX || SystemUtils.IS_OS_MAC|| SystemUtils.IS_OS_MAC_OSX || SystemUtils.IS_OS_NET_BSD || //SystemUtils.IS_OS_OPEN_BSD || SystemUtils.IS_OS_UNIX)
//    shutdownCommand = "shutdown -r -h " + t;
//else if(SystemUtils.IS_OS_HP_UX)
//    shutdownCommand = "shutdown -r -hy " + t;
//else if(SystemUtils.IS_OS_IRIX)
//    shutdownCommand = "shutdown -r -y -g " + t;
//else if(SystemUtils.IS_OS_SOLARIS || SystemUtils.IS_OS_SUN_OS)
//    shutdownCommand = "shutdown -r -y -i5 -g" + t;
//else if(SystemUtils.IS_OS_WINDOWS_XP || SystemUtils.IS_OS_WINDOWS_VISTA || SystemUtils.IS_OS_WINDOWS_7)
//   shutdownCommand = "shutdown.exe -r -t " + t;
//else
//   return false;

//Runtime.getRuntime().exec(shutdownCommand);
//return true;