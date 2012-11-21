
application {
	name="travelExtended"
	
	service {
		name = "cassandra-extend"
	}
	
	service {
		name = "tomcat"
		dependsOn = ["cassandra-extend"]
	}
}