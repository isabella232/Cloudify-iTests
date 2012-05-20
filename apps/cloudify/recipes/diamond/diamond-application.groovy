
application {
	name="diamond"
	
	service {
		name = "A"
		dependsOn = ["B", "C"]
	}
	
	service {
		name = "B"
		dependsOn = ["D"]
	}
	
	service {
		name = "C"
		dependsOn = ["D"]
	}
	
	service {
		name = "D"
	}
	
}