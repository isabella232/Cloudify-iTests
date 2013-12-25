service {
    
	name "simple-backwards"
	type "UNDEFINED"
	
	lifecycle {

		preInstall "pre_install.groovy"

		start (["Win.*":"run.bat", "Linux":"run.sh"])
        
	}
}