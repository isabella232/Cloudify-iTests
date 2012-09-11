service {
    
	name "first"
	type "UNDEFINED"
	
	lifecycle {

		preInstall { Thread.sleep(30000) }

		start (["Win.*":"run.bat", "Linux":"run.sh"])
        
	}
}