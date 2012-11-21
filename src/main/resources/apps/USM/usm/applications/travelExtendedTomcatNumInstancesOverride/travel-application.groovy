
application {
	name="travelExtendedTomcatNumInstancesOverride"
	
	service {
		name = "cassandra"	
	}
	
	service {
		name = "tomcat"
		dependsOn = ["cassandra"]
	}
	
	
}