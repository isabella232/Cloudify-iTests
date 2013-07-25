import java.util.concurrent.TimeUnit

println "get_instance_context_property"

def context = org.cloudifysource.utilitydomain.context.ServiceContextFactory.getServiceContext()

context.attributes.thisInstance["myKey"];

