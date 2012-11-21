
service {
	name "SimpleFilewriteAndPortOpener-service"

	lifecycle {

		//init "init.groovy";//{ println "This is the init event" }

		preInstall {println "This is the preInstall event" }
		postInstall {println "This is the postInstall event"
			//throw new IllegalStateException("HAHA")
		}
		preStart {println "This is the preStart event" }

		start (["Win.*":"run.bat -filePath " + System.getProperty("java.io.tmpdir") + "\\groovyLog3.log",
				 "Linux":"run.sh -filePath " + System.getProperty("java.io.tmpdir") + "/groovyLog3.log"])
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
		plugin {
			name "fileLiveness"
			className "org.cloudifysource.usm.liveness.FileLivenessDetector"
			config ([
						"FilePath" : System.getProperty("java.io.tmpdir") + "/groovyLog3.log",
						"TimeoutInSeconds" : 30,
						//this regx doesn't match any string printed by the filewritter process. 
						"regularExpression" : "Bye_World"
					])
		},
		plugin {
			name "jmx"
			className "org.cloudifysource.usm.jmx.JmxMonitor"
			config ([

				"Counter" : [
					"org.openspaces.usm.examples.simplejavaprocess:type=SimpleBlockingJavaProcess",
					"Counter"
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