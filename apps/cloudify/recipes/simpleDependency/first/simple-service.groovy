service {
    
	name "first"
	icon "icon.png"
	type "UNDEFINED"
	
	lifecycle {

		preInstall { Thread.sleep(30000) }

		start (["Win.*":"run.bat", "Linux":"run.sh"])
        
	}
}