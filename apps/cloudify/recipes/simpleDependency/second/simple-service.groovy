service {
    
	name "second"
	type "UNDEFINED"
	
	lifecycle {

		preInstall { Thread.sleep(10000) }

		start (["Win.*":"run.bat", "Linux":"run.sh"])
        
	}
}