
application {
	name="complexCycle"
	
	service {
		name = "1"
		dependsOn = ["2"]
	}
	
	service {
		name = "2"
		dependsOn = ["3"]
	}
	
	service {
		name = "3"
		dependsOn = ["4"]
	}
	
	service {
		name = "4"
		dependsOn = ["5"]
	}
	
	service {
		name = "5"
		dependsOn = ["6"]
	}
	
	service {
		name = "6"
		dependsOn = ["7"]
	}
	
	service {
		name = "7"
		dependsOn = ["8"]
	}
	
	service {
		name = "8"
		dependsOn = ["9"]
	}
	
	service {
		name = "9"
		dependsOn = ["10"]
	}
	
	service {
		name = "10"
		dependsOn = ["11"]
	}
	
	service {
		name = "11"
		dependsOn = ["12"]
	}
	
	service {
		name = "12"
		dependsOn = ["1"]
	}
	
}