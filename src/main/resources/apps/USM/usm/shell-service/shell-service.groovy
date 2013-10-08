service {
	name = "shell"
	
	lifecycle {
		locator {
			NO_PROCESS_LOCATORS
		}
	}
	
	compute {
		template templateName
	}
	
	customCommands ([
		"eval" : { x->
			println  "Evaluating: " + x 
			evaluate(x) 
		},
		"eval-script" : "eval.sh"
	])
}