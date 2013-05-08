import java.util.concurrent.TimeUnit
def context = org.cloudifysource.dsl.context.ServiceContextFactory.getServiceContext()
def service = context.waitForService("simpleCustomCommandsMultipleInstances-2", 10, TimeUnit.SECONDS)
if (service == null) {
	println("Could not find service simpleCustomCommandsMultipleInstances-2")
} else {
	service.invoke("sleep", ((Object[]) ""), 80000, TimeUnit.MILLISECONDS)
}
def serviceInstance = service.waitForInstances(1, 10, TimeUnit.SECONDS);
if (serviceInstance == null) {
	println("Could not find service instance for service simpleCustomCommandsMultipleInstances-2")
}
serviceInstance[0].invoke("shortSleep", ((Object[]) ""), 20000, TimeUnit.MILLISECONDS)