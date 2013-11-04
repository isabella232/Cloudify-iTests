import org.apache.commons.lang.StringUtils;
        
		String restartCommand;
        String operatingSystem = System.getProperty("os.name");
        
        if (StringUtils.isEmpty(operatingSystem)) {
        	 throw new RuntimeException("Unknown operating system.");
        }
        
        if (operatingSystem.contains("Linux") || operatingSystem.contains("Mac OS X")) {
            restartCommand = "/sbin/shutdown -r now";
        }
        else if (operatingSystem.contains("Windows")) {
            restartCommand = "shutdown.exe /r /t 00";
        }
        else {
            throw new RuntimeException("Unsupported operating system.");
        }
        Runtime.getRuntime().exec(restartCommand);
        System.exit(0);