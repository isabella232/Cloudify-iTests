
application {
	name="travelExtended"
	
	service {
		name = "cassandra"	
	}
	
	service {
		name = "tomcat"
		dependsOn = ["cassandra"]
	}
}