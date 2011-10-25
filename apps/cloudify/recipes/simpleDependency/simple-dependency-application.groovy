
application {
	name="simpleDependency"
	
	service {
		name = "first"
	}
	
	service {
		name = "second"
		dependsOn = ["first"]
	}
	
	
}