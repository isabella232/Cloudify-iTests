service {
	name = "eval"
	
	lifecycle {
		locator {
			NO_PROCESS_LOCATORS
		}
	}
	
	customCommands ([
		"eval" : { x->
			println  "Evaluating: " + x 
			evaluate(x) 
		}
	])
}