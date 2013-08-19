import org.cloudifysource.utilitydomain.context.ServiceContextFactory

new File("marker.txt").write("MARKER")

def context = ServiceContextFactory.getServiceContext()

def value = context.attributes.global.getProperty("GlobalAttribute")

context.attributes.global.setProperty("GlobalAttribute",value + 1)

throw new Exception("Groovy error service install event - this is an error test")

sleep(Long.MAX_VALUE)