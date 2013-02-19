service {
	name "simpleWithOverrides"
	icon "${iconName}.png"
	type "UNDEFINED"
	url urlProp
  
	lifecycle {
		init initService
		start {println "This is the start event"}
		shutdown {println "This is the shutdown event"}
	}
}