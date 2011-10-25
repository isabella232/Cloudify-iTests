service {
	name "simplejavaprocess-service"
	icon "icon.png"

	lifecycle{

		start ([ "Linux": "run.sh" ,
					"Windows XP": "run.bat" ,
					"Windows 7": "run.bat" ])
		
	}

	plugins ([
		plugin{

			name "SimpleJavaProcess Metrics"

			className "com.gigaspaces.cloudify.usm.jmx.JmxMonitor"

			config ([

						"Counter" : ["org.openspaces.usm.examples.simplejavaprocess:type=SimpleBlockingJavaProcess", "Counter"],
						port : 9999
					])
		}
	])
}