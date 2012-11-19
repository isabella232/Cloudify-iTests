service {
	name "simple"
	icon "${iconName}.png"
	type "UNDEFINED"
	url urlProp
  
	lifecycle {
		init initService
		start (["Win.*":"run.bat", "Linux":"run.sh"])
		shutdown {println "This is the shutdown event" }
	}
	
}