service {
	extend "tomcat"
	
	compute {
		template "SMALL_FEDORA"
	}
	
	customCommands ([
		"GetTemplateName" : { return "Template: " + context.cloudTemplateName},
		"GetImageID" : { return "Image ID: " + context.imageID}
	])
	
}