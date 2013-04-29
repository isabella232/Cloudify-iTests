import java.util.concurrent.TimeUnit
def context = org.cloudifysource.dsl.context.ServiceContextFactory.getServiceContext()
def service = context.waitForService("simpleCustomCommandsMultipleInstances-2", 10, TimeUnit.SECONDS)
if (service == null) {
	println("Could not find service simpleCustomCommandsMultipleInstances-2")
} else {
	service.invoke("sleep", ((Object[]) ""), 80000, TimeUnit.MILLISECONDS)
}