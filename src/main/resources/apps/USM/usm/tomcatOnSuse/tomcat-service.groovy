service {
	extend "tomcat"
	
	compute {
		template "SMALL_SUSE"
	}
	
	customCommands ([
		"GetTemplateName" : { return "Template: " + context.cloudTemplateName},
		"GetImageID" : { return "Image ID: " + context.imageID}
	])
	
}