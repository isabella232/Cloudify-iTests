import com.j_spaces.kernel.Environment;

service {
	
	def tomcatRecipeDir = Environment.getHomeDirectory() + "../recipes/services/tomcat"

	extend "${tomcatRecipeDir}"
	compute {
		template "TOMCAT"
	}
}