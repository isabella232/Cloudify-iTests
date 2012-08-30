service {
	
	def tomcatRecipeDir = "../../../recipes/services/tomcat"

	extend "${tomcatRecipeDir}"
	compute {
		template "TOMCAT"
	}
}