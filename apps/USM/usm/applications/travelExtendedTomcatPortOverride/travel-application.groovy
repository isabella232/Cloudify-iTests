
application {
	name="travelExtendedTomcatPortOverride"
	
	service {
		name = "cassandra"	
	}
	
	service {
		name = "tomcat"
		dependsOn = ["cassandra"]
	}
	
	
}