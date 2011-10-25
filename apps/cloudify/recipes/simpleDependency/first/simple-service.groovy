service {
    
	name "first"
	icon "icon.png"
	type "WEB"
	
	lifecycle {

		preInstall { Thread.sleep(30000) }

		start (["Win.*":"run.bat", "Linux":"run.sh"])
        
	}
}