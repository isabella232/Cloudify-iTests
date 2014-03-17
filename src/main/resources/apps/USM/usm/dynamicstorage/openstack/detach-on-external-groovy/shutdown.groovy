import org.cloudifysource.utilitydomain.context.ServiceContextFactory

println "shutdown.groovy: About to shutdown service..."

def context = ServiceContextFactory.getServiceContext()
def volumeId = context.attributes.thisInstance["volumes"]

println "Detaching volume with id ${volumeId} from machine."
context.storage.detachVolume(volumeId)
println "Deleting volume with id ${volumeId}"
context.storage.deleteVolume(volumeId);
