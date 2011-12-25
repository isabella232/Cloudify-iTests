
application {
	name="travelExtended"
	
	service {
		name = "cassandra-extend"
	}
	
	service {
		name = "tomcat-extend"
		dependsOn = ["cassandra-extend"]
	}
}