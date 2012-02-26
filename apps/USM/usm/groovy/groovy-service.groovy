import java.util.concurrent.TimeUnit


service {
	name "groovy"
	icon "icon.png"
	type "WEB_SERVER"
	elastic true
	lifecycle { start "run.groovy" }

	customCommands ([
				"echo" : {x ->
					return x
				},

				"contextInvoke": { x ->
					Object[] results =
							context.waitForService("groovy", 10, TimeUnit.SECONDS)
							.invoke("echo", x + " from " + context.instanceId )
					return java.util.Arrays.toString(results)
				}



			])
}