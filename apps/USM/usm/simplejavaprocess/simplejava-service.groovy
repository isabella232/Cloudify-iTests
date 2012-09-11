service {
	name "simplejavaprocess-service"
	type "UNDEFINED"

	lifecycle{

		start ([ "Linux": "run.sh" ,
					"Windows XP": "run.bat" ,
					"Windows 7": "run.bat" ])
		
	}

	plugins ([
		plugin{

			name "SimpleJavaProcess Metrics"

			className "org.cloudifysource.usm.jmx.JmxMonitor"

			config ([

						"Counter" : ["org.openspaces.usm.examples.simplejavaprocess:type=SimpleBlockingJavaProcess", "Counter"],
						port : 9999
					])
		}
	])
}