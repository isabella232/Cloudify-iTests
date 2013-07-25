import java.util.concurrent.TimeUnit

println "set_service_context_property"

def context = org.cloudifysource.utilitydomain.context.ServiceContextFactory.getServiceContext()
context.attributes.thisService["myKey"] = "myValue";


