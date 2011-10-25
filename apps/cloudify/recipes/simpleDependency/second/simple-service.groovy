service {
    
	name "second"
	icon "icon.png"
	type "WEB"
	
	lifecycle {

		preInstall { Thread.sleep(10000) }

		start (["Win.*":"run.bat", "Linux":"run.sh"])
        
	}
}