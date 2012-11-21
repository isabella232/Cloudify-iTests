
application {
	name="cycle"
	
	service {
		name = "egg"
		dependsOn = ["chicken"]
	}
	
	service {
		name = "chicken"
		dependsOn = ["egg"]
	}
	
	
}