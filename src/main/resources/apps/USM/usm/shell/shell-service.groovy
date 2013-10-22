service {
	name "shell"
	type "WEB_SERVER"

	lifecycle {

		locator {
			NO_PROCESS_LOCATORS
		}
	}
	
	customCommands ([
		// dumps the environment variables to the output
		"env" : "env"
	])
	
	network {
		accessRules {
			incoming = ([
				accessRule {
					portRange "80"
					type "PUBLIC"
				},
				accessRule {
					portRange "81"
					type "SERVICE"
				}
			])
		}

		
	}

}