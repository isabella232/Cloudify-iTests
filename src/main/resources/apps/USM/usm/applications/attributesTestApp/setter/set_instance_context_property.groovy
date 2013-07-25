import java.util.concurrent.TimeUnit

println "set_instance_context_property"

def context = org.cloudifysource.utilitydomain.context.ServiceContextFactory.getServiceContext()
context.attributes.thisInstance["myKey"] = "myValue";


