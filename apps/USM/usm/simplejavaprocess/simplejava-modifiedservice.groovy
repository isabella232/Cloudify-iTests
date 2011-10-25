
service {
	name "simplejavaprocess-service"
	icon "icon.png"

	lifecycle{

		init {println "This is the MODIFIED init lifecycle phase"}
		start ([ "Linux": "run.sh -port 7790" ,
					"Win.*": "run.bat -port 7790" ])
		
	}

	plugins ([
		plugin{

			name "SimpleJavaProcess Metrics"

			className "com.gigaspaces.cloudify.usm.jmx.JmxMonitor"

			config ([

						"Details" : ["org.openspaces.usm.examples.simplejavaprocess:type=SimpleBlockingJavaProcess", "Details"],
						"Counter" : ["org.openspaces.usm.examples.simplejavaprocess:type=SimpleBlockingJavaProcess", "Counter"],
						"Type" : ["org.openspaces.usm.examples.simplejavaprocess:type=SimpleBlockingJavaProcess", "Type"],
						port : 9999
					])
		}
	])
}