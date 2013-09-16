import org.apache.commons.lang.StringUtils;
        
		String shutdownCommand;
        String operatingSystem = System.getProperty("os.name");
        
        if (StringUtils.isEmpty(operatingSystem)) {
        	 throw new RuntimeException("Unknown operating system.");
        }
        
        if (operatingSystem.contains("Linux") || operatingSystem.contains("Mac OS X")) {
            shutdownCommand = "sudo shutdown -h now";
        }
        else if (operatingSystem.contains("Windows")) {
            shutdownCommand = "shutdown.exe -s -t 0";
        }
        else {
            throw new RuntimeException("Unsupported operating system.");
        }
        Runtime.getRuntime().exec(shutdownCommand);
        System.exit(0);