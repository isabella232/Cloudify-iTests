
service {
	name "SimpleFilewriteAndPortOpener-service"

	lifecycle {

		//init "init.groovy";//{ println "This is the init event" }

		preStart {ServiceUtils.arePortsFree([3668,3667])}
		startDetection {ServiceUtils.arePortsOccupied([3668,3667])}
		preInstall {println "This is the preInstall event" }
		postInstall {println "This is the postInstall event"
			//throw new IllegalStateException("HAHA")
		}
		preStart {println "This is the preStart event" }

		start (["Win.*":"run.bat -port 3666",
				 "Linux":"run.sh -port 3666"])
		//		start {
		//			def fullPath =  context.dir + "\\run.bat"
		//			println "Executing command: " + fullPath
		//			return  fullPath.execute()
		//			}

		postStart {println "This is the postStart event" }

		preStop {println "This is the preStop event" }
		postStop {println "This is the postStop event" }
		shutdown {println "This is the shutdown event" }
	}

	plugins ([
//		plugin {
//			name "portLiveness"
//			className "org.cloudifysource.usm.liveness.PortLivenessDetector"
//			config ([
//						"Port" : [3666, 3667],
//						"TimeoutInSeconds" : 30,
//						"Host" : "127.0.0.1"
//					])
//		},
		plugin {
			name "jmx"
			className "org.cloudifysource.usm.jmx.JmxMonitor"
			config ([

				"Details" : [
					"org.openspaces.usm.examples.simplejavaprocess:type=SimpleBlockingJavaProcess",
					"Details"
				],
				"Counter" : [
					"org.openspaces.usm.examples.simplejavaprocess:type=SimpleBlockingJavaProcess",
					"Counter"
				],
				"Type" : [
					"org.openspaces.usm.examples.simplejavaprocess:type=SimpleBlockingJavaProcess",
					"Type"
				],
				"port" : 9999
			])
		}
	])

	userInterface {
		metricGroups = [
			metricGroup{
				name = "process"
				metrics = ["cpu", "memory"]
			},
			metricGroup{
				name = "space"
				metrics = ["reads", "writes"]
			}
		]
		widgetGroups = [
			widgetGroup{
				name  ="cpu"
				widgets = [
					balanceGauge{metric = "cpu"},
					barLineChart{metric = "cpu"}
				]
			},
			widgetGroup {
				name = "memory"
				widgets = [
					balanceGauge { metric = "memory" },
					barLineChart{ metric = "memory" }
				]
			}
		]
	}
}